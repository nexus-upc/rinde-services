package com.nexus.rinde.notification.infrastructure.persistence.jpa.entities;

import com.nexus.rinde.notification.domain.model.valueobjects.RetryPolicy;
import com.nexus.rinde.notification.domain.model.valueobjects.TripAssignmentNotice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Registro operativo de un aviso pendiente, fallido o entregado; eventId es la clave idempotente. */
@Entity
@Table(schema = "notification", name = "retry_store")
public class RetryStoreEntry {

  public static final String PENDING = "PENDING";
  public static final String DELIVERED = "DELIVERED";
  public static final String FAILED = "FAILED";
  public static final String SKIPPED = "SKIPPED";

  @Id
  @Column(name = "event_id", nullable = false, updatable = false)
  private UUID eventId;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "trip_id", nullable = false, updatable = false)
  private UUID tripId;

  @Column(name = "recipient_driver_id", nullable = false, updatable = false)
  private UUID recipientDriverId;

  @Column(name = "trip_code", nullable = false, length = 20, updatable = false)
  private String tripCode;

  @Column(name = "destination", nullable = false, length = 120, updatable = false)
  private String destination;

  @Column(name = "departure_date", nullable = false, updatable = false)
  private LocalDate departureDate;

  @Column(name = "message", nullable = false, length = 500, updatable = false)
  private String message;

  @Column(name = "delivery_status", nullable = false, length = 20)
  private String deliveryStatus;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;

  @Column(name = "last_error", length = 500)
  private String lastError;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  protected RetryStoreEntry() {}

  private RetryStoreEntry(TripAssignmentNotice notice, Instant now) {
    this.eventId = notice.eventId();
    this.tenantId = notice.tenantId();
    this.tripId = notice.tripId();
    this.recipientDriverId = notice.driverId();
    this.tripCode = notice.tripCode();
    this.destination = notice.destination();
    this.departureDate = notice.departureDate();
    this.message = notice.message();
    this.deliveryStatus = PENDING;
    this.retryCount = 0;
    this.lastError = null;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static RetryStoreEntry prepared(TripAssignmentNotice notice, Instant now) {
    return new RetryStoreEntry(notice, now);
  }

  public void markDelivered(Instant now) {
    this.deliveryStatus = DELIVERED;
    this.nextAttemptAt = null;
    this.lastError = null;
    this.deliveredAt = now;
    this.updatedAt = now;
  }

  /** Marca el aviso como omitido cuando no hay usuario o dispositivos sin programar reintentos. */
  public void markSkipped(String reason, Instant now) {
    this.deliveryStatus = SKIPPED;
    this.nextAttemptAt = null;
    this.lastError = shorten(reason);
    this.updatedAt = now;
  }

  /** La primera falla no es un reintento; cada reintento fallido suma uno a retryCount. */
  public void markFailed(String error, boolean retry, RetryPolicy policy, Instant now) {
    this.lastError = shorten(error);
    this.updatedAt = now;
    if (retry) {
      this.retryCount++;
    }
    scheduleNextAttempt(policy, now);
  }

  /** Indica si el aviso sigue pendiente y su próximo intento ya venció. */
  public boolean isDueAt(Instant now) {
    return PENDING.equals(deliveryStatus) && nextAttemptAt != null && !nextAttemptAt.isAfter(now);
  }

  /** Reconstruye el aviso con los datos guardados; el instante del evento no se persiste, así que se usa createdAt. */
  public TripAssignmentNotice toNotice() {
    return new TripAssignmentNotice(
        eventId,
        tenantId,
        tripId,
        recipientDriverId,
        tripCode,
        destination,
        departureDate,
        createdAt,
        message);
  }

  private void scheduleNextAttempt(RetryPolicy policy, Instant now) {
    Optional<Duration> wait = policy.waitBeforeNextRetry(retryCount);
    if (wait.isPresent()) {
      this.deliveryStatus = PENDING;
      this.nextAttemptAt = now.plus(wait.get());
    } else {
      this.deliveryStatus = FAILED;
      this.nextAttemptAt = null;
    }
  }

  public UUID getEventId() {
    return eventId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getTripId() {
    return tripId;
  }

  public UUID getRecipientDriverId() {
    return recipientDriverId;
  }

  public String getTripCode() {
    return tripCode;
  }

  public String getDestination() {
    return destination;
  }

  public LocalDate getDepartureDate() {
    return departureDate;
  }

  public String getMessage() {
    return message;
  }

  public String getDeliveryStatus() {
    return deliveryStatus;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getDeliveredAt() {
    return deliveredAt;
  }

  /** Recorta el error al tamaño de la columna para que guardarlo nunca falle. */
  private static String shorten(String error) {
    return error != null && error.length() > 500 ? error.substring(0, 500) : error;
  }
}
