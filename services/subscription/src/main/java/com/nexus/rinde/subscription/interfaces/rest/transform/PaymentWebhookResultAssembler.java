package com.nexus.rinde.subscription.interfaces.rest.transform;

import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentWebhookResult;
import com.nexus.rinde.subscription.interfaces.rest.resources.PaymentWebhookResultResource;

public final class PaymentWebhookResultAssembler {

  private PaymentWebhookResultAssembler() {}

  public static PaymentWebhookResultResource toResource(PaymentWebhookResult result) {
    return new PaymentWebhookResultResource(
        result.providerEventId(),
        result.duplicate(),
        result.paymentStatus(),
        result.subscriptionStatus(),
        result.startsAt(),
        result.expiresAt(),
        result.failureReason());
  }
}
