package com.nexus.rinde.incident.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IncidentRepository
    extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {

  Optional<Incident> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<Incident> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

  List<Incident> findByTenantIdAndStatusNotOrderByReportedAtDesc(
      UUID tenantId, IncidentStatus status);
}
