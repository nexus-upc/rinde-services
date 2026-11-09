package com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.fleet.domain.model.aggregates.Driver;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio JPA para el agregado Driver. */
@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {

  Optional<Driver> findByIdAndTenantId(UUID id, UUID tenantId);

  List<Driver> findByTenantId(UUID tenantId);

  boolean existsByTenantIdAndDocumentNumber(UUID tenantId, String documentNumber);

  boolean existsByTenantIdAndLicenseNumber(UUID tenantId, String licenseNumber);

  Optional<Driver> findByTenantIdAndUserId(UUID tenantId, UUID userId);

  List<Driver> findAllByTenantIdAndUserId(UUID tenantId, UUID userId);
}
