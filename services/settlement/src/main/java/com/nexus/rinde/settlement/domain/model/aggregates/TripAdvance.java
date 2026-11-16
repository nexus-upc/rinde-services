package com.nexus.rinde.settlement.domain.model.aggregates;

import com.nexus.rinde.settlement.domain.model.valueobjects.AdvanceDeliveryMethod;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Adelanto de viáticos entregado al conductor antes de la salida del viaje (US35). */
@Entity
@Table(
    schema = "settlement",
    name = "trip_advances",
    indexes = {@Index(name = "idx_trip_advances_tenant_trip", columnList = "tenant_id, trip_id")})
public class TripAdvance extends AuditableAbstractAggregateRoot {

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "trip_id", nullable = false, updatable = false)
  private UUID tripId;

  @Column(name = "amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_method", nullable = false, length = 20)
  private AdvanceDeliveryMethod deliveryMethod;

  @Column(name = "registered_by", nullable = false, updatable = false)
  private UUID registeredBy;

  protected TripAdvance() {}

  /** Registra un adelanto con dos decimales; sin moneda indicada, se usa PEN. */
  public static TripAdvance register(
      UUID tenantId,
      UUID tripId,
      BigDecimal amount,
      String currency,
      AdvanceDeliveryMethod deliveryMethod,
      UUID registeredBy) {
    if (amount == null || amount.signum() <= 0) {
      throw new BusinessRuleException("El monto del adelanto debe ser mayor a cero.");
    }
    if (deliveryMethod == null) {
      throw new BusinessRuleException("Indique el medio de entrega del adelanto.");
    }
    TripAdvance advance = new TripAdvance();
    advance.tenantId = tenantId;
    advance.tripId = tripId;
    advance.amount = amount.setScale(2, RoundingMode.HALF_UP);
    advance.currency = normalizeCurrency(currency);
    advance.deliveryMethod = deliveryMethod;
    advance.registeredBy = registeredBy;
    return advance;
  }

  /** Devuelve el código de moneda en mayúsculas; sin dato, devuelve PEN. */
  public static String normalizeCurrency(String currency) {
    return currency != null && !currency.isBlank() ? currency.trim().toUpperCase() : "PEN";
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getTripId() {
    return tripId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public AdvanceDeliveryMethod getDeliveryMethod() {
    return deliveryMethod;
  }

  public UUID getRegisteredBy() {
    return registeredBy;
  }
}
