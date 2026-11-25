package com.nexus.rinde.notification.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Marca de lectura por usuario de una notificación de empresa. */
@Entity
@Table(schema = "notification", name = "notification_reads")
public class NotificationReadEntity {

  @EmbeddedId private NotificationReadId id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "read_at", nullable = false, updatable = false)
  private Instant readAt;

  protected NotificationReadEntity() {}

  public NotificationReadId getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public Instant getReadAt() {
    return readAt;
  }
}
