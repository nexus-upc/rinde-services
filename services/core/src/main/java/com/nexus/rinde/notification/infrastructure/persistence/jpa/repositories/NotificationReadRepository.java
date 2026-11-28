package com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.NotificationReadEntity;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.NotificationReadId;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repositorio JPA de lecturas de notificaciones por usuario. */
@Repository
public interface NotificationReadRepository
    extends JpaRepository<NotificationReadEntity, NotificationReadId> {

  /** Registra la lectura solo si el usuario no la tiene; así conserva el readAt original. */
  @Modifying
  @Query(
      value =
          """
          insert into notification.notification_reads (notification_id, user_id, tenant_id, read_at)
          values (:notificationId, :userId, :tenantId, :readAt)
          on conflict (notification_id, user_id) do nothing
          """,
      nativeQuery = true)
  int insertReadIfAbsent(
      @Param("notificationId") UUID notificationId,
      @Param("userId") UUID userId,
      @Param("tenantId") UUID tenantId,
      @Param("readAt") Instant readAt);

  /** Marca como leídas las notificaciones de la empresa que el usuario aún no ha leído. */
  @Modifying
  @Query(
      value =
          """
          insert into notification.notification_reads (notification_id, user_id, tenant_id, read_at)
          select n.id, :userId, n.tenant_id, :readAt
          from notification.company_notifications n
          where n.tenant_id = :tenantId
          on conflict (notification_id, user_id) do nothing
          """,
      nativeQuery = true)
  int insertReadsForAllOfTenant(
      @Param("userId") UUID userId,
      @Param("tenantId") UUID tenantId,
      @Param("readAt") Instant readAt);
}
