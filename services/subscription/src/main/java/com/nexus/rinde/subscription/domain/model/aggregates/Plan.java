package com.nexus.rinde.subscription.domain.model.aggregates;

import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Plan del catálogo común a todas las empresas. */
@Entity
@Table(schema = "subscription", name = "plans")
public class Plan {

  @Id
  @Column(name = "plan_id", nullable = false, updatable = false)
  private UUID planId;

  @Column(name = "name", nullable = false, length = 80)
  private String name;

  @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal monthlyPrice;

  @Column(name = "unit_limit", nullable = false)
  private Integer unitLimit;

  @Column(name = "active", nullable = false)
  private boolean active;

  protected Plan() {}

  public Plan(String name, BigDecimal monthlyPrice, Integer unitLimit, boolean active) {
    this(UUID.randomUUID(), name, monthlyPrice, unitLimit, active);
  }

  public Plan(
      UUID planId, String name, BigDecimal monthlyPrice, Integer unitLimit, boolean active) {
    if (planId == null) {
      throw new BusinessRuleException("El identificador del plan es obligatorio.");
    }
    if (name == null || name.isBlank()) {
      throw new BusinessRuleException("El nombre del plan es obligatorio.");
    }
    if (monthlyPrice == null || monthlyPrice.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessRuleException("El precio mensual del plan debe ser mayor a cero.");
    }
    if (unitLimit == null || unitLimit <= 0) {
      throw new BusinessRuleException("El límite de unidades debe ser mayor a cero.");
    }
    this.planId = planId;
    this.name = name.trim();
    this.monthlyPrice = monthlyPrice;
    this.unitLimit = unitLimit;
    this.active = active;
  }

  public UUID getPlanId() {
    return planId;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getMonthlyPrice() {
    return monthlyPrice;
  }

  public Integer getUnitLimit() {
    return unitLimit;
  }

  public void changeAvailability(boolean active) {
    this.active = active;
  }

  public boolean isActive() {
    return active;
  }
}
