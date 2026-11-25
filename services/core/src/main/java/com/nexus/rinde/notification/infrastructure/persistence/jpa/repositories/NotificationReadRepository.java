package com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.NotificationReadEntity;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.NotificationReadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio JPA de lecturas de notificaciones por usuario. */
@Repository
public interface NotificationReadRepository
    extends JpaRepository<NotificationReadEntity, NotificationReadId> {}
