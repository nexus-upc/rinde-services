package com.nexus.rinde.subscription.domain.model.commands;

import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;

/** Resultado simulado que entrega la pasarela por webhook. */
public record ProcessPaymentWebhookCommand(
    String providerEventId,
    String providerReference,
    PaymentStatus status,
    String amount,
    String failureReason) {}
