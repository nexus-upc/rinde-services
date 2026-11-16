package com.nexus.rinde.subscription.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.UUID;

/** Representación pública de un plan activo. */
public record PlanResource(UUID planId, String name, BigDecimal monthlyPrice, Integer unitLimit) {}
