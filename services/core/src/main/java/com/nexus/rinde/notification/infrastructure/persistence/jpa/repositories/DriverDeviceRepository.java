package com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.DriverDeviceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio JPA de tokens push registrados por conductores. */
@Repository
public interface DriverDeviceRepository extends JpaRepository<DriverDeviceEntity, UUID> {}
