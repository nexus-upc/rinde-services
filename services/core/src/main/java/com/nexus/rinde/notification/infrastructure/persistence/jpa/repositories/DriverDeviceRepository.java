package com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.DriverDeviceEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio JPA de tokens push registrados por conductores. */
@Repository
public interface DriverDeviceRepository extends JpaRepository<DriverDeviceEntity, UUID> {

  Optional<DriverDeviceEntity> findByToken(String token);

  List<DriverDeviceEntity> findAllByTenantIdAndUserIdOrderByUpdatedAtDesc(
      UUID tenantId, UUID userId);

  void deleteByToken(String token);
}
