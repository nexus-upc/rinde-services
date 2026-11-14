package com.nexus.rinde.subscription.domain.services;

import com.nexus.rinde.subscription.domain.model.commands.ProcessPaymentWebhookCommand;
import com.nexus.rinde.subscription.domain.model.commands.StartSubscriptionCheckoutCommand;
import com.nexus.rinde.subscription.domain.model.commands.SimulatePaymentCommand;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentCheckout;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentWebhookResult;

/** Operaciones de checkout y recepción idempotente del resultado del pago. */
public interface PaymentCommandService {

  PaymentCheckout handle(StartSubscriptionCheckoutCommand command);

  PaymentWebhookResult handle(ProcessPaymentWebhookCommand command);

  PaymentWebhookResult handle(SimulatePaymentCommand command);
}
