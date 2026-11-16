package com.nexus.rinde.incident.application.internal.queryservices;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentStatus;
import com.nexus.rinde.incident.domain.services.IncidentQueryService;
import com.nexus.rinde.incident.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.nexus.rinde.incident.infrastructure.persistence.jpa.repositories.IncidentSpecifications;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de incidencias. Un conductor no ve incidencias que reportó otro conductor. */
@Service
public class IncidentQueryServiceImpl implements IncidentQueryService {

  private static final String DRIVER_ROLE = "DRIVER";

  private final IncidentRepository incidentRepository;

  public IncidentQueryServiceImpl(IncidentRepository incidentRepository) {
    this.incidentRepository = incidentRepository;
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
}
