package com.nexus.rinde.trip.domain.model.entities;

import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.trip.domain.model.valueobjects.GeoCoordinates;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Punto de ubicación reportado por el conductor durante un viaje en ruta. */
@Entity
@Table(
    schema = "trip",
    name = "trip_locations",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_trip_locations_trip_recorded_at",
            columnNames = {"trip_id", "recorded_at"}),
    indexes =
        @Index(
            name = "idx_trip_locations_tenant_trip_recorded_at",
            columnList = "tenant_id, trip_id, recorded_at"))
public class TripLocation {

  @Id private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "trip_id", nullable = false, updatable = false)
  private UUID tripId;

  @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
  private BigDecimal latitude;

  @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
  private BigDecimal longitude;

  @Column(name = "accuracy_meters", precision = 10, scale = 2)
  private BigDecimal accuracyMeters;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  protected TripLocation() {}

  /** Registra un punto truncando los instantes a microsegundos para coincidir con PostgreSQL. */
  public static TripLocation record(
      UUID tenantId,
      UUID tripId,
      GeoCoordinates coordinates,
      Instant recordedAt,
      Instant receivedAt) {
    if (tenantId == null
        || tripId == null
        || coordinates == null
        || recordedAt == null
        || receivedAt == null) {
      throw new BusinessRuleException("Los datos de ubicación del viaje son obligatorios.");
    }
    TripLocation location = new TripLocation();
    location.id = UUID.randomUUID();
    location.tenantId = tenantId;
    location.tripId = tripId;
    location.latitude = coordinates.latitude();
    location.longitude = coordinates.longitude();
    location.accuracyMeters = coordinates.accuracyMeters();
    location.recordedAt = recordedAt.truncatedTo(ChronoUnit.MICROS);
    location.receivedAt = receivedAt.truncatedTo(ChronoUnit.MICROS);
    return location;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getTripId() {
    return tripId;
  }

  public BigDecimal getLatitude() {
    return latitude;
  }

  public BigDecimal getLongitude() {
    return longitude;
  }

  public BigDecimal getAccuracyMeters() {
    return accuracyMeters;
  }

  public Instant getRecordedAt() {
    return recordedAt;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }
}
