package com.nexus.rinde.expense.domain.model.aggregates;

import com.nexus.rinde.expense.domain.model.entities.Evidence;
import com.nexus.rinde.expense.domain.model.events.ExpenseObserved;
import com.nexus.rinde.expense.domain.model.valueobjects.EvidenceFile;
import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseCategory;
import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseStatus;
import com.nexus.rinde.expense.domain.model.valueobjects.Money;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Agregado de gasto operativo con comprobante y auditoría de liquidación. */
@Entity
@Table(
    schema = "expense",
    name = "expenses",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_expenses_idempotency_key",
            columnNames = {"idempotency_key"}),
    indexes = {
      @Index(name = "idx_expenses_tenant_trip", columnList = "tenant_id, trip_id"),
      @Index(name = "idx_expenses_trip_status", columnList = "trip_id, status")
    })
public class Expense extends AuditableAbstractAggregateRoot {

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "trip_id", nullable = false, updatable = false)
  private UUID tripId;

  @Column(name = "driver_id", nullable = false, updatable = false)
  private UUID driverId;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 30)
  private ExpenseCategory category;

  @Embedded private Money amount;

  @Column(name = "expense_date", nullable = false)
  private LocalDate expenseDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ExpenseStatus status;

  @Column(name = "idempotency_key", nullable = false, length = 64, updatable = false)
  private String idempotencyKey;

  @Column(name = "observation_reason", length = 255)
  private String observationReason;

  @Column(name = "is_locked", nullable = false)
  private boolean isLocked;

  @OneToOne(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
  private Evidence evidence;

  protected Expense() {}

  /** Crea un gasto operativo. Nace en PENDING_SUPPORT hasta que se adjunta su comprobante. */
  public static Expense create(
      UUID tenantId,
      UUID tripId,
      UUID driverId,
      ExpenseCategory category,
      Money amount,
      LocalDate expenseDate,
      String idempotencyKey) {
    if (tenantId == null || tripId == null || driverId == null) {
      throw new BusinessRuleException("La empresa, el viaje y el conductor son obligatorios.");
    }
    if (category == null || amount == null || expenseDate == null) {
      throw new BusinessRuleException("La categoría, el monto y la fecha del gasto son obligatorios.");
    }
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new BusinessRuleException("La clave de idempotencia es obligatoria.");
    }

    Expense expense = new Expense();
    expense.tenantId = tenantId;
    expense.tripId = tripId;
    expense.driverId = driverId;
    expense.category = category;
    expense.amount = amount;
    expense.expenseDate = expenseDate;
    expense.idempotencyKey = idempotencyKey.trim();
    expense.isLocked = false;
    expense.status = ExpenseStatus.PENDING_SUPPORT;
    return expense;
  }

  /**
   * Adjunta el comprobante o reemplaza el anterior. Un gasto pendiente de sustento pasa a
   * REGISTERED. Devuelve la clave del archivo reemplazado, si lo había.
   */
  public String attachEvidence(EvidenceFile file, String storageKey, UUID uploadedBy, Instant now) {
    ensureNotLocked();
    Instant uploadedAt = now != null ? now : Instant.now();
    String previousKey = null;
    if (evidence == null) {
      evidence = new Evidence(this, tenantId, file, storageKey, uploadedBy, uploadedAt);
    } else {
      previousKey = evidence.getStorageKey();
      evidence.replaceFile(file, storageKey, uploadedBy, uploadedAt);
    }
    if (status == ExpenseStatus.PENDING_SUPPORT) {
      status = ExpenseStatus.REGISTERED;
    }
    return previousKey;
  }

  /** Aprueba el gasto durante la revisión. */
  public void approve(UUID reviewerId, Instant now) {
    ensureNotLocked();
    this.status = ExpenseStatus.APPROVED;
    this.observationReason = null;
  }

  /** Observa el gasto durante la revisión e indica el motivo de inconsistencia. */
  public void observe(UUID reviewerId, String reason, Instant now) {
    ensureNotLocked();
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("El motivo de observación es obligatorio.");
    }
    this.status = ExpenseStatus.OBSERVED;
    this.observationReason = reason.trim();
    registerEvent(
        new ExpenseObserved(
            UUID.randomUUID(),
            now != null ? now : Instant.now(),
            tenantId,
            getId(),
            tripId,
            driverId,
            this.observationReason));
  }

  /** Bloquea el gasto cuando Settlement cierra la liquidación del viaje. */
  public void lock() {
    this.isLocked = true;
  }

  /** Rechaza cualquier cambio cuando la liquidación del viaje ya está cerrada. */
  public void ensureNotLocked() {
    if (isLocked) {
      throw new ConflictException("El gasto se encuentra bloqueado por liquidación cerrada.");
    }
  }

  public boolean belongsTo(UUID otherTenantId) {
    return tenantId != null && tenantId.equals(otherTenantId);
  }

  public boolean isForTrip(UUID otherTripId) {
    return tripId != null && tripId.equals(otherTripId);
  }

  public boolean isByDriver(UUID otherDriverId) {
    return driverId != null && driverId.equals(otherDriverId);
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getTripId() {
    return tripId;
  }

  public UUID getDriverId() {
    return driverId;
  }

  public ExpenseCategory getCategory() {
    return category;
  }

  public Money getAmount() {
    return amount;
  }

  public LocalDate getExpenseDate() {
    return expenseDate;
  }

  public ExpenseStatus getStatus() {
    return status;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getObservationReason() {
    return observationReason;
  }

  public boolean isLocked() {
    return isLocked;
  }

  public Evidence getEvidence() {
    return evidence;
  }
}
