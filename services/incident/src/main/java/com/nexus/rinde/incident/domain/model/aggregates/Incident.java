package com.nexus.rinde.incident.domain.model.aggregates;

import com.nexus.rinde.incident.domain.model.commands.ReportIncidentCommand;
import com.nexus.rinde.incident.domain.model.events.IncidentClosed;
import com.nexus.rinde.incident.domain.model.events.IncidentReported;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentResolution;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentStatus;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentType;
import com.nexus.rinde.incident.domain.model.valueobjects.TripSnapshot;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ForbiddenOperationException;
import com.nexus.rinde.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * Incidencia reportada por el conductor durante un viaje en ruta. Nace abierta, pasa a atención y
 * se cierra con la acción tomada.
 */
@Entity
@Table(
    schema = "incident",
    name = "incidents",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_incidents_tenant_idempotency",
            columnNames = {"tenant_id", "idempotency_key"}),
    indexes = {
      @Index(name = "idx_incidents_tenant_status", columnList = "tenant_id, status"),
      @Index(name = "idx_incidents_tenant_trip", columnList = "tenant_id, trip_id"),
      @Index(name = "idx_incidents_tenant_vehicle", columnList = "tenant_id, vehicle_id")
    })
public class Incident extends AuditableAbstractAggregateRoot {

  private static final int MAX_TEXT_LENGTH = 500;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "trip_id", nullable = false, updatable = false)
  private UUID tripId;

  @Column(name = "vehicle_id", nullable = false, updatable = false)
  private UUID vehicleId;

  @Column(name = "reported_by", nullable = false, updatable = false)
  private UUID reportedBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 30, updatable = false)
  private IncidentType type;

  @Column(name = "description", nullable = false, length = MAX_TEXT_LENGTH, updatable = false)
  private String description;

  @Column(name = "evidence_url", length = MAX_TEXT_LENGTH, updatable = false)
  private String evidenceUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private IncidentStatus status;

  @Column(name = "idempotency_key", length = 64, updatable = false)
  private String idempotencyKey;

  @Column(name = "reported_at", nullable = false, updatable = false)
  private Instant reportedAt;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "action", column = @Column(name = "resolution_action", length = 500)),
    @AttributeOverride(name = "closedBy", column = @Column(name = "resolution_closed_by")),
    @AttributeOverride(name = "closedAt", column = @Column(name = "resolution_closed_at"))
  })
  private IncidentResolution resolution;

  protected Incident() {}

  /**
   * Registra una incidencia abierta. Solo el conductor asignado puede reportarla y únicamente
   * mientras el viaje está en ruta.
   */
  public static Incident report(
      TripSnapshot trip, UUID reporterDriverId, ReportIncidentCommand command, Instant now) {
    if (command.type() == null) {
      throw new BusinessRuleException("El tipo de incidencia es obligatorio.");
    }
    String description = trimToNull(command.description());
    if (description == null) {
      throw new BusinessRuleException("La descripción de la incidencia es obligatoria.");
    }
    if (description.length() > MAX_TEXT_LENGTH) {
      throw new BusinessRuleException("La descripción no puede superar los 500 caracteres.");
    }
    String evidenceUrl = trimToNull(command.evidenceUrl());
    if (evidenceUrl != null && evidenceUrl.length() > MAX_TEXT_LENGTH) {
      throw new BusinessRuleException("La URL de la evidencia no puede superar los 500 caracteres.");
    }
    if (!trip.isAssignedTo(reporterDriverId)) {
      throw new ForbiddenOperationException(
          "Solo el conductor asignado al viaje puede reportar incidencias.");
    }
    if (!trip.isInRoute()) {
      throw new ConflictException("Solo se puede reportar una incidencia cuando el viaje está en ruta.");
    }

    Instant reportedAt = now != null ? now : Instant.now();
    Incident incident = new Incident();
    incident.tenantId = command.tenantId();
    incident.tripId = command.tripId();
    incident.vehicleId = trip.vehicleId();
    incident.reportedBy = command.reportedBy();
    incident.type = command.type();
    incident.description = description;
    incident.evidenceUrl = evidenceUrl;
    incident.status = IncidentStatus.OPEN;
    incident.idempotencyKey = trimToNull(command.idempotencyKey());
    incident.reportedAt = reportedAt;
    incident.registerEvent(
        new IncidentReported(
            UUID.randomUUID(),
            reportedAt,
            incident.tenantId,
            incident.getId(),
            incident.tripId,
            incident.vehicleId,
            incident.reportedBy,
            incident.type.name()));
    return incident;
  }

  /** Pasa la incidencia a atención. Solo es posible desde el estado abierta. */
  public void startAttention() {
    if (status != IncidentStatus.OPEN) {
      throw new ConflictException("Solo una incidencia abierta puede pasar a atención.");
    }
    this.status = IncidentStatus.IN_PROGRESS;
  }

  /** Cierra la incidencia registrando la acción tomada, quién la atendió y cuándo. */
  public void close(UUID closedBy, String action, Instant now) {
    if (status == IncidentStatus.CLOSED) {
      throw new ConflictException("La incidencia ya está cerrada.");
    }
    String actionTaken = trimToNull(action);
    if (actionTaken == null) {
      throw new BusinessRuleException("La acción tomada es obligatoria.");
    }
    if (actionTaken.length() > MAX_TEXT_LENGTH) {
      throw new BusinessRuleException("La acción tomada no puede superar los 500 caracteres.");
    }
    Instant closedAt = now != null ? now : Instant.now();
    this.status = IncidentStatus.CLOSED;
    this.resolution = new IncidentResolution(actionTaken, closedBy, closedAt);
    registerEvent(
        new IncidentClosed(
            UUID.randomUUID(),
            closedAt,
            tenantId,
            getId(),
            tripId,
            vehicleId,
            closedBy,
            actionTaken));
  }

  public boolean isReportedBy(UUID userId) {
    return userId != null && userId.equals(reportedBy);
  }

  private static String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getTripId() {
    return tripId;
  }

  public UUID getVehicleId() {
    return vehicleId;
  }

  public UUID getReportedBy() {
    return reportedBy;
  }

  public IncidentType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public String getEvidenceUrl() {
    return evidenceUrl;
  }

  public IncidentStatus getStatus() {
    return status;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public Instant getReportedAt() {
    return reportedAt;
  }

  public IncidentResolution getResolution() {
    return resolution;
  }
}
