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
