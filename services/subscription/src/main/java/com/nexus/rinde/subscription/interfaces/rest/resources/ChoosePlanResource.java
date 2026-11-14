package com.nexus.rinde.subscription.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Cuerpo para seleccionar un plan del catálogo. */
public record ChoosePlanResource(@NotNull UUID planId) {}
