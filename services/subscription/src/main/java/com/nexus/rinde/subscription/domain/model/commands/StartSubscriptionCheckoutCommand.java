package com.nexus.rinde.subscription.domain.model.commands;

import java.util.UUID;

/** Inicia un intento de pago simulado para una suscripción de la empresa autenticada. */
public record StartSubscriptionCheckoutCommand(UUID tenantId, UUID subscriptionId) {}
