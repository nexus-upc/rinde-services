package com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.CompanyNotificationEntity;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repositorio JPA de notificaciones de empresa. */
@Repository
public interface CompanyNotificationRepository
    extends JpaRepository<CompanyNotificationEntity, UUID> {

  boolean existsByEventId(UUID eventId);

  Optional<CompanyNotificationEntity> findByEventId(UUID eventId);

  boolean existsByIdAndTenantId(UUID id, UUID tenantId);

  /** Lista las notificaciones de la empresa con la lectura del usuario, más recientes primero. */
  @Query(
      value =
          """
          select new com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRow(
              n.id, n.type, n.title, n.message, n.link, n.createdAt, r.readAt)
          from CompanyNotificationEntity n
          left join NotificationReadEntity r
              on r.id.notificationId = n.id and r.id.userId = :userId
          where n.tenantId = :tenantId
            and n.type in :types
            and (:unreadOnly = false or r.readAt is null)
          order by n.createdAt desc, n.id desc
          """,
      countQuery =
          """
          select count(n)
          from CompanyNotificationEntity n
          left join NotificationReadEntity r
              on r.id.notificationId = n.id and r.id.userId = :userId
          where n.tenantId = :tenantId
            and n.type in :types
            and (:unreadOnly = false or r.readAt is null)
          """)
  Page<CompanyNotificationRow> findPageForUser(
      @Param("tenantId") UUID tenantId,
      @Param("userId") UUID userId,
      @Param("unreadOnly") boolean unreadOnly,
      @Param("types") Collection<String> types,
      Pageable pageable);

  /** Cuenta las notificaciones de la empresa que el usuario aún no ha leído. */
  @Query(
      """
      select count(n) from CompanyNotificationEntity n
      where n.tenantId = :tenantId
        and not exists (
          select r from NotificationReadEntity r
          where r.id.notificationId = n.id and r.id.userId = :userId)
      """)
  long countUnreadForUser(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);
}
