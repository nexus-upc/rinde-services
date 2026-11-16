package com.nexus.rinde.settlement.domain.model.valueobjects;

import java.util.List;

/** Gastos del viaje para la liquidación: los aprobados y la cantidad que aún no se revisa. */
public record TripExpenseReview(List<ApprovedExpense> approved, int pendingCount) {}
