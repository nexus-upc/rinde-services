package com.nexus.rinde.trip.domain.model.aggregates;

import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.nexus.rinde.trip.domain.model.entities.StatusChange;
import com.nexus.rinde.trip.domain.model.events.TripAssigned;
import com.nexus.rinde.trip.domain.model.events.TripFinished;
import com.nexus.rinde.trip.domain.model.events.TripStarted;
import com.nexus.rinde.trip.domain.model.valueobjects.Assignment;
import com.nexus.rinde.trip.domain.model.valueobjects.Availability;
import com.nexus.rinde.trip.domain.model.valueobjects.Cargo;
import com.nexus.rinde.trip.domain.model.valueobjects.MaintenanceState;
import com.nexus.rinde.trip.domain.model.valueobjects.Route;
import com.nexus.rinde.trip.domain.model.valueobjects.TripStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Viaje y raíz única del agregado Trip Management. */
@Entity
@Table(
    schema = "trip",
    name = "trips",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_trips_tenant_code", columnNames = {"tenant_id", "code"}),
    indexes = {
      @Index(name = "idx_trips_tenant_status", columnList = "tenant_id, status"),
      @Index(name = "idx_trips_tenant_driver", columnList = "tenant_id, driver_id")
    })
public class Trip extends AuditableAbstractAggregateRoot {

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "code", nullable = false, length = 20, updatable = false)
  private String code;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(
        name = "origin",
        column = @Column(name = "origin", nullable = false, length = 120)),
    @AttributeOverride(
        name = "destination",
        column = @Column(name = "destination", nullable = false, length = 120))
  })
  private Route route;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(
        name = "description",
        column = @Column(name = "cargo_description", nullable = false, length = 200)),
    @AttributeOverride(
        name = "weightKg",
        column = @Column(name = "cargo_weight_kg", precision = 10, scale = 2))
  })
  private Cargo cargo;

  @Column(name = "departure_date", nullable = false)
  private LocalDate departureDate;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "vehicleId", column = @Column(name = "vehicle_id")),
    @AttributeOverride(name = "driverId", column = @Column(name = "driver_id")),
    @AttributeOverride(name = "assignedAt", column = @Column(name = "assigned_at")),
    @AttributeOverride(
        name = "overdueMaintenanceConfirmedBy",
        column = @Column(name = "maintenance_confirmed_by"))
  })
  private Assignment assignment;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private TripStatus status;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("changedAt ASC")
  private List<StatusChange> statusChanges = new ArrayList<>();

  protected Trip() {}

  /** Crea un viaje programado y registra su estado inicial. */
  public static Trip schedule(
      UUID tenantId,
      String code,
      Route route,
      Cargo cargo,
      LocalDate departureDate,
      boolean confirmPastDate,
      LocalDate today,
      Instant now,
      UUID changedBy) {
    if (tenantId == null || code == null || !code.matches("TRP-\\d{6}")) {
      throw new BusinessRuleException("La empresa y el código del viaje son obligatorios.");
    }
    if (route == null || cargo == null || departureDate == null || today == null || now == null) {
      throw new BusinessRuleException("La ruta, la carga y la fecha de salida son obligatorias.");
    }
    if (changedBy == null) {
      throw new BusinessRuleException("El usuario que programa el viaje es obligatorio.");
    }
    if (departureDate.isBefore(today) && !confirmPastDate) {
      throw new ConflictException(
          "La fecha de salida es anterior a hoy. Confirme que se trata de un viaje ya ejecutado.");
    }
    Trip trip = new Trip();
    trip.tenantId = tenantId;
    trip.code = code;
    trip.route = route;
    trip.cargo = cargo;
    trip.departureDate = departureDate;
    trip.status = TripStatus.SCHEDULED;
    trip.addStatusChange(TripStatus.SCHEDULED, now, changedBy);
    return trip;
  }

  /** Solo un viaje programado puede recibir vehículo y conductor. */
  public void ensureAssignable() {
    requireStatus(TripStatus.SCHEDULED, "asignar");
  }

  /** Solo un viaje en ruta admite el registro de puntos de ubicación. */
  public void ensureInRouteForLocation() {
    if (status != TripStatus.IN_ROUTE) {
      throw new ConflictException("Solo se registra la ubicación de un viaje en ruta.");
    }
  }

  /** Asigna los recursos después de validar su disponibilidad en Trip y Fleet. */
  public void assign(
      UUID vehicleId,
      UUID driverId,
      Availability availability,
      boolean confirmOverdueMaintenance,
      UUID assignedBy,
      Instant now) {
    ensureAssignable();
    if (vehicleId == null || driverId == null || availability == null || now == null) {
      throw new BusinessRuleException("El vehículo, el conductor y la disponibilidad son obligatorios.");
    }
    if (!availability.driverEnabled()) {
      throw new ConflictException("El conductor no está habilitado para ser asignado.");
    }
    if (availability.maintenance() == MaintenanceState.OVERDUE && !confirmOverdueMaintenance) {
      throw new ConflictException(
          "La unidad tiene el mantenimiento vencido. Confirme la asignación para continuar.");
    }
    if (assignedBy == null) {
      throw new BusinessRuleException("El usuario que asigna el viaje es obligatorio.");
    }
    UUID confirmedBy =
        availability.maintenance() == MaintenanceState.OVERDUE ? assignedBy : null;
    assignment = new Assignment(vehicleId, driverId, now, confirmedBy);
    status = TripStatus.ASSIGNED;
    addStatusChange(status, now, assignedBy);
    registerEvent(
        new TripAssigned(
            UUID.randomUUID(),
            now,
            tenantId,
            getId(),
            code,
            route.destination(),
            departureDate,
            vehicleId,
            driverId));
  }

  /** Inicia el viaje y conserva la fecha de inicio. */
  public void start(UUID startedBy, Instant now) {
    requireStatus(TripStatus.ASSIGNED, "iniciar");
    if (assignment == null || now == null || startedBy == null) {
      throw new BusinessRuleException("La asignación y el usuario que inicia son obligatorios.");
    }
    status = TripStatus.IN_ROUTE;
    startedAt = now;
    addStatusChange(status, now, startedBy);
    registerEvent(
        new TripStarted(
            UUID.randomUUID(),
            now,
            tenantId,
            getId(),
            assignment.vehicleId(),
            assignment.driverId()));
  }

  /** Finaliza el viaje y conserva la fecha de fin. */
  public void finish(UUID finishedBy, Instant now) {
    requireStatus(TripStatus.IN_ROUTE, "finalizar");
    if (assignment == null || now == null || finishedBy == null) {
      throw new BusinessRuleException("La asignación y el usuario que finaliza son obligatorios.");
    }
    status = TripStatus.FINISHED;
    finishedAt = now;
    addStatusChange(status, now, finishedBy);
    registerEvent(
        new TripFinished(
            UUID.randomUUID(),
            now,
            tenantId,
            getId(),
            assignment.vehicleId(),
            assignment.driverId()));
  }

  /** Marca el viaje liquidado tras SettlementClosed. */
  public void markSettled(UUID settledBy, Instant now) {
    requireStatus(TripStatus.FINISHED, "liquidar");
    if (settledBy == null || now == null) {
      throw new BusinessRuleException("El usuario y la fecha de liquidación son obligatorios.");
    }
    status = TripStatus.SETTLED;
    addStatusChange(status, now, settledBy);
  }

  private void requireStatus(TripStatus expected, String action) {
    if (status != expected) {
      String current = status == null ? "desconocido" : status.name();
      throw new ConflictException(
          "No se puede "
              + action
              + " el viaje porque su estado actual es "
              + current
              + "; se requiere "
              + expected.name()
              + ".");
    }
  }

  private void addStatusChange(TripStatus newStatus, Instant changedAt, UUID changedBy) {
    statusChanges.add(new StatusChange(this, tenantId, newStatus, changedAt, changedBy));
  }

  public boolean belongsTo(UUID otherTenantId) {
    return tenantId.equals(otherTenantId);
  }

  public boolean isAssignedTo(UUID driverId) {
    return assignment != null && assignment.driverId().equals(driverId);
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getCode() {
    return code;
  }

  public Route getRoute() {
    return route;
  }

  public Cargo getCargo() {
    return cargo;
  }

  public LocalDate getDepartureDate() {
    return departureDate;
  }

  public Assignment getAssignment() {
    return assignment;
  }

  public TripStatus getStatus() {
    return status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public List<StatusChange> getStatusChanges() {
    return Collections.unmodifiableList(statusChanges);
  }
}
