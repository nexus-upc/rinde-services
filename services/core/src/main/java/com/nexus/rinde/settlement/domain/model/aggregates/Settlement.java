package com.nexus.rinde.settlement.domain.model.aggregates;

import com.nexus.rinde.settlement.domain.model.valueobjects.ApprovedExpense;
import com.nexus.rinde.settlement.domain.model.valueobjects.SettlementStatus;
import com.nexus.rinde.settlement.interfaces.acl.SettlementClosed;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Agregado de liquidación de viaje: consolida anticipo y gastos aprobados del conductor. */
@Entity
@Table(
    schema = "settlement",
    name = "settlements",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_settlements_trip_id", columnNames = {"trip_id"}),
    indexes = {
      @Index(name = "idx_settlements_tenant_trip", columnList = "tenant_id, trip_id")
    })
public class Settlement extends AuditableAbstractAggregateRoot {

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "trip_id", nullable = false, updatable = false)
  private UUID tripId;

  @Column(name = "driver_id", nullable = false, updatable = false)
  private UUID driverId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private SettlementStatus status;

  @Column(name = "advance_amount", precision = 10, scale = 2)
  private BigDecimal advanceAmount;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "advance_currency", length = 3)
  private String advanceCurrency;

  @Column(name = "expense_total_amount", precision = 10, scale = 2)
  private BigDecimal expenseTotalAmount;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "expense_total_currency", length = 3)
  private String expenseTotalCurrency;

  @Column(name = "advance_balance", precision = 10, scale = 2)
  private BigDecimal advanceBalance;

  @Column(name = "closed_by")
  private UUID closedBy;

  @Column(name = "closed_at")
  private Instant closedAt;

  protected Settlement() {}

  /** Crea una liquidación abierta al finalizar el viaje. */
  public static Settlement create(UUID tenantId, UUID tripId, UUID driverId) {
    if (tenantId == null || tripId == null || driverId == null) {
      throw new BusinessRuleException("La empresa, el viaje y el conductor son obligatorios.");
    }
    Settlement s = new Settlement();
    s.tenantId = tenantId;
    s.tripId = tripId;
    s.driverId = driverId;
    s.status = SettlementStatus.OPEN;
    return s;
  }

  /** Incorpora el total de los adelantos registrados antes de la salida del viaje. */
  public void incorporateAdvance(BigDecimal total, String currency) {
    ensureOpen();
    if (total == null || total.signum() <= 0) {
      return;
    }
    this.advanceAmount = total;
    this.advanceCurrency = currency;
  }

  /** Recalcula el total de gastos aprobados y el balance del anticipo. */
  public void recalculate(List<ApprovedExpense> approvedExpenses) {
    ensureOpen();
    BigDecimal total =
        approvedExpenses.stream()
            .map(ApprovedExpense::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    String currency = this.advanceCurrency != null ? this.advanceCurrency : "PEN";
    this.expenseTotalAmount = total;
    this.expenseTotalCurrency = currency;
    this.advanceBalance =
        this.advanceAmount != null ? this.advanceAmount.subtract(total) : total.negate();
  }

  /** Cierra la liquidación sumando los gastos aprobados y publica SettlementClosed. */
  public void close(
      List<ApprovedExpense> approvedExpenses, int pendingExpenses, UUID closedBy, Instant now) {
    ensureOpen();
    if (pendingExpenses > 0) {
      throw new ConflictException(
          "No se puede cerrar la liquidación mientras haya gastos sin revisar. Apruébelos u obsérvelos primero.");
    }
    recalculate(approvedExpenses);
    this.status = SettlementStatus.CLOSED;
    this.closedBy = closedBy;
    this.closedAt = now != null ? now : Instant.now();

    registerEvent(
        new SettlementClosed(UUID.randomUUID(), this.closedAt, tenantId, tripId, closedBy));
  }

  private void ensureOpen() {
    if (status == SettlementStatus.CLOSED) {
      throw new ConflictException("La liquidación ya fue cerrada.");
    }
  }

  public boolean belongsTo(UUID otherTenantId) {
    return tenantId != null && tenantId.equals(otherTenantId);
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

  public SettlementStatus getStatus() {
    return status;
  }

  public BigDecimal getAdvanceAmount() {
    return advanceAmount;
  }

  public String getAdvanceCurrency() {
    return advanceCurrency;
  }

  public BigDecimal getExpenseTotalAmount() {
    return expenseTotalAmount;
  }

  public String getExpenseTotalCurrency() {
    return expenseTotalCurrency;
  }

  public BigDecimal getAdvanceBalance() {
    return advanceBalance;
  }

  public UUID getClosedBy() {
    return closedBy;
  }

  public Instant getClosedAt() {
    return closedAt;
  }
}
