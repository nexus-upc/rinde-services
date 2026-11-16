package com.nexus.rinde.subscription.interfaces.rest.transform;

import com.nexus.rinde.subscription.domain.model.queries.SubscriptionDetails;
import com.nexus.rinde.subscription.interfaces.rest.resources.SubscriptionResource;

/** Convierte una proyección de consulta en el recurso público de la API. */
public final class SubscriptionResourceFromDetailsAssembler {

  private SubscriptionResourceFromDetailsAssembler() {}

  public static SubscriptionResource toResource(SubscriptionDetails details) {
    return new SubscriptionResource(
        details.subscription().getSubscriptionId(),
        PlanResourceFromEntityAssembler.toResource(details.plan()),
        details.subscription().getStatus(),
        details.subscription().getStartsAt(),
        details.subscription().getExpiresAt(),
        details.renewalNotice(),
        details.latestPaymentStatus(),
        details.lastPaymentFailureReason());
  }
}
