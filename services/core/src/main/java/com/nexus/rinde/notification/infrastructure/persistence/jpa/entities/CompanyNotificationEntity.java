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

  private CompanyNotificationEntity(
      UUID id,
      UUID eventId,
      UUID tenantId,
      String type,
      String title,
      String message,
      String link,
      Instant createdAt) {
    this.id = id;
    this.eventId = eventId;
    this.tenantId = tenantId;
    this.type = type;
    this.title = title;
    this.message = message;
    this.link = link;
    this.createdAt = createdAt;
  }

  /** Crea el aviso de empresa que deja un evento ya validado; el id lo asigna la aplicación. */
  public static CompanyNotificationEntity create(
      UUID eventId,
      UUID tenantId,
      String type,
      String title,
      String message,
      String link,
      Instant createdAt) {
    return new CompanyNotificationEntity(
        UUID.randomUUID(), eventId, tenantId, type, title, message, link, createdAt);
  }

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
