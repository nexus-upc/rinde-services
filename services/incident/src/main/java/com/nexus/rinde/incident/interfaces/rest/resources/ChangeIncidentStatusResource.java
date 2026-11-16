package com.nexus.rinde.incident.interfaces.rest.resources;

import com.nexus.rinde.incident.domain.model.valueobjects.IncidentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Nuevo estado de atención de la incidencia. */
public record ChangeIncidentStatusResource(
    @Schema(example = "IN_PROGRESS", allowableValues = {"IN_PROGRESS"}) @NotNull IncidentStatus status) {}
