package com.nexus.rinde.incident.application.internal.queryservices;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.entities.IncidentEvidence;
import com.nexus.rinde.incident.domain.model.queries.GetIncidentEvidenceContentQuery;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentEvidenceContent;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentEvidenceFile;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentStatus;
import com.nexus.rinde.incident.domain.services.IncidentEvidenceStorage;
import com.nexus.rinde.incident.domain.services.IncidentQueryService;
import com.nexus.rinde.incident.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.nexus.rinde.incident.infrastructure.persistence.jpa.repositories.IncidentSpecifications;
import com.nexus.rinde.shared.domain.exceptions.ForbiddenOperationException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de incidencias. Un conductor no ve incidencias que reportó otro conductor. */
@Service
public class IncidentQueryServiceImpl implements IncidentQueryService {

  private static final String DRIVER_ROLE = "DRIVER";
  private static final Set<String> COMPANY_READERS = Set.of("ADMINISTRATOR", "OPERATIONS_MANAGER");

  private final IncidentRepository incidentRepository;
  private final IncidentEvidenceStorage evidenceStorage;

  public IncidentQueryServiceImpl(
      IncidentRepository incidentRepository, IncidentEvidenceStorage evidenceStorage) {
    this.incidentRepository = incidentRepository;
    this.evidenceStorage = evidenceStorage;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Incident> list(UUID tenantId, IncidentStatus status, UUID tripId, UUID vehicleId) {
    return incidentRepository.findAll(
        IncidentSpecifications.matching(tenantId, status, tripId, vehicleId),
        Sort.by(Sort.Direction.DESC, "reportedAt"));
  }

  @Override
  @Transactional(readOnly = true)
  public Incident findVisible(UUID tenantId, UUID incidentId, UUID userId, String role) {
    Incident incident =
        incidentRepository
            .findByIdAndTenantId(incidentId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("La incidencia no existe."));
    if (DRIVER_ROLE.equals(role) && !incident.isReportedBy(userId)) {
      throw new ResourceNotFoundException("La incidencia no existe.");
    }
    return incident;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Incident> findOpen(UUID tenantId) {
    return incidentRepository.findByTenantIdAndStatusNotOrderByReportedAtDesc(
        tenantId, IncidentStatus.CLOSED);
  }

  /** Devuelve los bytes de la evidencia verificando permisos y huella SHA-256 (C4). */
  @Override
  @Transactional(readOnly = true)
  public IncidentEvidenceContent findEvidenceContent(GetIncidentEvidenceContentQuery query) {
    Incident incident =
        incidentRepository
            .findByIdAndTenantId(query.incidentId(), query.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("La incidencia no existe."));

    boolean companyReader = COMPANY_READERS.contains(query.role());
    boolean ownerDriver = DRIVER_ROLE.equals(query.role()) && incident.isReportedBy(query.userId());
    if (!companyReader && !ownerDriver) {
      throw new ForbiddenOperationException("No tiene permiso para ver esta evidencia.");
    }

    IncidentEvidence evidence = incident.getEvidence();
    if (evidence == null || !evidence.getId().equals(query.evidenceId())) {
      throw new ResourceNotFoundException("La evidencia no existe.");
    }
    if (evidence.getStorageKey() == null) {
      throw new ResourceNotFoundException(
          "El archivo del comprobante no está disponible en el almacenamiento.");
    }

    byte[] content = evidenceStorage.load(evidence.getStorageKey());
    if (!IncidentEvidenceFile.sha256Hex(content).equals(evidence.getSha256())) {
      throw new IllegalStateException("El archivo de la evidencia no coincide con su huella.");
    }
    return new IncidentEvidenceContent(evidence.getFileName(), evidence.getContentType(), content);
  }
}
