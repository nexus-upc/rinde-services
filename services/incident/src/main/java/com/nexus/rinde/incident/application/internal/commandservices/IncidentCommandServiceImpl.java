package com.nexus.rinde.incident.application.internal.commandservices;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.commands.AttachIncidentEvidenceCommand;
import com.nexus.rinde.incident.domain.model.commands.CloseIncidentCommand;
import com.nexus.rinde.incident.domain.model.commands.ReportIncidentCommand;
import com.nexus.rinde.incident.domain.model.commands.StartIncidentAttentionCommand;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentEvidenceFile;
import com.nexus.rinde.incident.domain.model.valueobjects.ReportOutcome;
import com.nexus.rinde.incident.domain.model.valueobjects.TripSnapshot;
import com.nexus.rinde.incident.domain.services.DriverLookupService;
import com.nexus.rinde.incident.domain.services.IncidentCommandService;
import com.nexus.rinde.incident.domain.services.IncidentEvidenceStorage;
import com.nexus.rinde.incident.domain.services.TripLookupService;
import com.nexus.rinde.incident.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ForbiddenOperationException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Coordina el reporte, la atención y el cierre de incidencias con su persistencia. */
@Service
public class IncidentCommandServiceImpl implements IncidentCommandService {

  private static final String DUPLICATE_KEY_MESSAGE = "La clave de idempotencia ya fue utilizada.";

  private final IncidentRepository incidentRepository;
  private final TripLookupService tripLookupService;
  private final DriverLookupService driverLookupService;
  private final IncidentEvidenceStorage evidenceStorage;
  private final Clock clock;
  private final long maxEvidenceSizeBytes;

  public IncidentCommandServiceImpl(
      IncidentRepository incidentRepository,
      TripLookupService tripLookupService,
      DriverLookupService driverLookupService,
      IncidentEvidenceStorage evidenceStorage,
      Clock clock,
      @Value("${rinde.evidence.max-size-bytes:5242880}") String maxEvidenceSizeBytes) {
    this.incidentRepository = incidentRepository;
    this.tripLookupService = tripLookupService;
    this.driverLookupService = driverLookupService;
    this.evidenceStorage = evidenceStorage;
    this.clock = clock;
    this.maxEvidenceSizeBytes =
        (maxEvidenceSizeBytes == null || maxEvidenceSizeBytes.isBlank())
            ? 5242880L
            : Long.parseLong(maxEvidenceSizeBytes.trim());
  }

  /**
   * Un reenvío con la misma clave devuelve la incidencia ya registrada, sin duplicarla. La clave
   * solo la puede reutilizar el mismo usuario que la creó.
   */
  @Override
  @Transactional
  public ReportOutcome report(ReportIncidentCommand command) {
    String key = normalize(command.idempotencyKey());
    if (key != null) {
      Optional<Incident> existing =
          incidentRepository.findByTenantIdAndIdempotencyKey(command.tenantId(), key);
      if (existing.isPresent()) {
        if (!existing.get().isReportedBy(command.reportedBy())) {
          throw new ConflictException(DUPLICATE_KEY_MESSAGE);
        }
        return new ReportOutcome(existing.get(), false);
      }
    }

    TripSnapshot trip =
        tripLookupService
            .findTrip(command.tenantId(), command.tripId())
            .orElseThrow(() -> new ResourceNotFoundException("El viaje no existe."));
    UUID reporterDriverId =
        driverLookupService
            .findDriverIdByUserId(command.tenantId(), command.reportedBy())
            .orElse(null);

    Incident incident =
        Incident.report(trip, reporterDriverId, withNormalizedKey(command, key), clock.instant());
    return new ReportOutcome(incidentRepository.saveAndFlush(incident), true);
  }

  @Override
  @Transactional
  public Incident startAttention(StartIncidentAttentionCommand command) {
    Incident incident = find(command.tenantId(), command.incidentId());
    incident.startAttention();
    return incidentRepository.saveAndFlush(incident);
  }

  @Override
  @Transactional
  public Incident close(CloseIncidentCommand command) {
    Incident incident = find(command.tenantId(), command.incidentId());
    incident.close(command.closedBy(), command.action(), clock.instant());
    return incidentRepository.saveAndFlush(incident);
  }

  /** Guarda el archivo de evidencia y borra el anterior después de confirmar la transacción. */
  @Override
  @Transactional
  public Incident attachEvidence(AttachIncidentEvidenceCommand command) {
    if (command.originalFilename() == null
        && (command.content() == null || command.content().length == 0)) {
      throw new BusinessRuleException("Adjunte el comprobante en el campo file.");
    }
    Incident incident = find(command.tenantId(), command.incidentId());
    if (!incident.isReportedBy(command.userId())) {
      throw new ForbiddenOperationException(
          "Solo el conductor que reportó la incidencia puede adjuntar su evidencia.");
    }

    IncidentEvidenceFile file =
        IncidentEvidenceFile.inspect(
            command.originalFilename(), command.content(), maxEvidenceSizeBytes);
    String storageKey = command.tenantId() + "/" + incident.getId() + "/" + UUID.randomUUID();
    evidenceStorage.save(storageKey, command.content());

    String previousKey =
        incident.attachEvidence(file, storageKey, command.userId(), clock.instant());
    Incident saved = incidentRepository.saveAndFlush(incident);
    deleteAfterCommit(previousKey);
    return saved;
  }

  /** Elimina el archivo reemplazado únicamente tras confirmarse la transacción en base de datos. */
  private void deleteAfterCommit(String previousKey) {
    if (previousKey == null) {
      return;
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      evidenceStorage.delete(previousKey);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            evidenceStorage.delete(previousKey);
          }
        });
  }

  private Incident find(UUID tenantId, UUID incidentId) {
    return incidentRepository
        .findByIdAndTenantId(incidentId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("La incidencia no existe."));
  }

  private static ReportIncidentCommand withNormalizedKey(ReportIncidentCommand command, String key) {
    return new ReportIncidentCommand(
        command.tenantId(),
        command.tripId(),
        command.reportedBy(),
        command.type(),
        command.description(),
        command.evidenceUrl(),
        key);
  }

  /** La clave se guarda sin espacios alrededor, igual que se busca al reenviar. */
  private static String normalize(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      return null;
    }
    return idempotencyKey.trim();
  }
}
