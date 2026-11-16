package com.nexus.rinde.subscription.domain.model.queries;

import java.util.UUID;

/** Consulta la suscripción vigente o pendiente de pago de la empresa autenticada. */
public record GetCurrentSubscriptionQuery(UUID tenantId) {}
