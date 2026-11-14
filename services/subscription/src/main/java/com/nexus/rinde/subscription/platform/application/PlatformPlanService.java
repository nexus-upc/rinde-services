package com.nexus.rinde.subscription.platform.application;

import com.nexus.rinde.shared.domain.exceptions.*;
import com.nexus.rinde.shared.infrastructure.security.*;
import com.nexus.rinde.shared.interfaces.rest.PageResource;
import com.nexus.rinde.subscription.domain.model.aggregates.Plan;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformPlanService {
  private final PlanRepository plans;
  private final PlatformAuditTrail audit;
  private final Clock clock;

  public PlatformPlanService(PlanRepository plans, JdbcTemplate jdbc, Clock clock) {
    this.plans = plans;
    this.audit = new PlatformAuditTrail(jdbc, "subscription");
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResource<PlanRow> list(int page, int size) {
    return PageResource.from(
        plans
            .findAll(PageRequest.of(page, size, Sort.by("name").and(Sort.by("planId"))))
            .map(PlanRow::from));
  }

  @Transactional
  public PlanRow create(
      String name, BigDecimal price, int units, String reason, PlatformPrincipal actor) {
    Plan plan = plans.saveAndFlush(new Plan(name, price, units, true));
    audit.record(
        actor,
        "PLAN_CREATED",
        "PLAN",
        plan.getPlanId(),
        reason,
        "absent",
        snapshot(plan),
        clock.instant());
    return PlanRow.from(plan);
  }

  @Transactional
  public PlanRow availability(UUID id, boolean active, String reason, PlatformPrincipal actor) {
    Plan plan =
        plans
            .findForPlatformUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("El plan no existe."));
    if (plan.isActive() == active)
      throw new ConflictException("El plan ya tiene esa disponibilidad.");
    String before = snapshot(plan);
    plan.changeAvailability(active);
    audit.record(
        actor,
        active ? "PLAN_PUBLISHED" : "PLAN_WITHDRAWN",
        "PLAN",
        id,
        reason,
        before,
        snapshot(plan),
        clock.instant());
    return PlanRow.from(plan);
  }

  @Transactional(readOnly = true)
  public PageResource<PlatformAuditTrail.Entry> audit(int page, int size) {
    return audit.list(page, size);
  }

  private String snapshot(Plan plan) {
    return plan.getName()
        + " | PEN "
        + plan.getMonthlyPrice()
        + " | "
        + plan.getUnitLimit()
        + " | "
        + plan.isActive();
  }

  public record PlanRow(
      UUID planId, String name, BigDecimal monthlyPrice, int unitLimit, boolean active) {
    public static PlanRow from(Plan p) {
      return new PlanRow(
          p.getPlanId(), p.getName(), p.getMonthlyPrice(), p.getUnitLimit(), p.isActive());
    }
  }
}
