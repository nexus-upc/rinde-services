package com.nexus.rinde.subscription.interfaces.rest.transform;

import com.nexus.rinde.subscription.domain.model.aggregates.Plan;
import com.nexus.rinde.subscription.interfaces.rest.resources.PlanResource;

/** Convierte un plan del dominio en el recurso público de la API. */
public final class PlanResourceFromEntityAssembler {

  private PlanResourceFromEntityAssembler() {}

  public static PlanResource toResource(Plan plan) {
    return new PlanResource(
        plan.getPlanId(), plan.getName(), plan.getMonthlyPrice(), plan.getUnitLimit());
  }
}
