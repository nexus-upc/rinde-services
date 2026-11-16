package com.nexus.rinde.incident.application.internal.commandservices;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.commands.CloseIncidentCommand;
import com.nexus.rinde.incident.domain.model.commands.ReportIncidentCommand;
import com.nexus.rinde.incident.domain.model.commands.StartIncidentAttentionCommand;
import com.nexus.rinde.incident.domain.model.valueobjects.ReportOutcome;
import com.nexus.rinde.incident.domain.model.valueobjects.TripSnapshot;
import com.nexus.rinde.incident.domain.services.DriverLookupService;
import com.nexus.rinde.incident.domain.services.IncidentCommandService;
import com.nexus.rinde.incident.domain.services.TripLookupService;
import com.nexus.rinde.incident.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordina el reporte, la atención y el cierre de incidencias con su persistencia. */
@Service
public class IncidentCommandServiceImpl implements IncidentCommandService {

  private static final String DUPLICATE_KEY_MESSAGE = "La clave de idempotencia ya fue utilizada.";

  private final IncidentRepository incidentRepository;
  private final TripLookupService tripLookupService;
  private final DriverLookupService driverLookupService;
  private final Clock clock;

  public IncidentCommandServiceImpl(
      IncidentRepository incidentRepository,
      TripLookupService tripLookupService,
      DriverLookupService driverLookupService,
      Clock clock) {
    this.incidentRepository = incidentRepository;
    this.tripLookupService = tripLookupService;
    this.driverLookupService = driverLookupService;
    this.clock = clock;
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
