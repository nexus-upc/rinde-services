package com.nexus.rinde.notification.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Notificación operativa de empresa generada a partir de eventos de integración. */
@Entity
@Table(schema = "notification", name = "company_notifications")
public class CompanyNotificationEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "event_id", nullable = false, unique = true, updatable = false)
  private UUID eventId;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "type", nullable = false, length = 40, updatable = false)
  private String type;

  @Column(name = "title", nullable = false, length = 150, updatable = false)
  private String title;

  @Column(name = "message", nullable = false, length = 500, updatable = false)
  private String message;

  @Column(name = "link", nullable = false, length = 255, updatable = false)
  private String link;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected CompanyNotificationEntity() {}

  public UUID getId() {
    return id;
  }

  public UUID getEventId() {
    return eventId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getMessage() {
    return message;
  }

  public String getLink() {
    return link;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
