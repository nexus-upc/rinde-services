package com.nexus.rinde.subscription.domain.model.queries;

import java.util.UUID;

/** Consulta de suscripción limitada a una empresa. */
public record GetSubscriptionByTenantQuery(UUID tenantId, UUID subscriptionId) {}
