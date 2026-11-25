package com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.CompanyNotificationEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio JPA de notificaciones de empresa. */
@Repository
public interface CompanyNotificationRepository
    extends JpaRepository<CompanyNotificationEntity, UUID> {}
