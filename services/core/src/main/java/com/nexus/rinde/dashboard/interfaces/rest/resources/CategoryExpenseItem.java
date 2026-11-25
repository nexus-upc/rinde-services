package com.nexus.rinde.dashboard.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Gasto acumulado en moneda base para una categoría. */
public record CategoryExpenseItem(
    @Schema(example = "FUEL") String category,
    @Schema(example = "2450.00") BigDecimal total) {}
