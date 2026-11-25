package com.nexus.rinde.notification.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Token de dispositivo registrado por un conductor para recibir avisos push. */
@Entity
@Table(schema = "notification", name = "driver_devices")
public class DriverDeviceEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token", nullable = false, unique = true, length = 512)
  private String token;

  @Column(name = "platform", nullable = false, length = 20)
  private String platform;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected DriverDeviceEntity() {}

  private DriverDeviceEntity(
      UUID id,
      UUID tenantId,
      UUID userId,
      String token,
      String platform,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.userId = userId;
    this.token = token;
    this.platform = platform;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** Crea un nuevo registro de dispositivo para notificaciones push del conductor. */
  public static DriverDeviceEntity register(
      UUID tenantId, UUID userId, String token, String platform, Instant now) {
    return new DriverDeviceEntity(UUID.randomUUID(), tenantId, userId, token, platform, now, now);
  }

  /** Actualiza el propietario, la plataforma y la marca de uso sin duplicar el token. */
  public void reassign(UUID tenantId, UUID userId, String platform, Instant now) {
    this.tenantId = tenantId;
    this.userId = userId;
    this.platform = platform;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getToken() {
    return token;
  }

  public String getPlatform() {
    return platform;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
