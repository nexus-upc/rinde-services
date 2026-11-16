package com.nexus.rinde.subscription.domain.model.valueobjects;

/** Referencia de checkout emitida por el adaptador de pasarela. */
public record PaymentGatewaySession(String providerReference) {}
