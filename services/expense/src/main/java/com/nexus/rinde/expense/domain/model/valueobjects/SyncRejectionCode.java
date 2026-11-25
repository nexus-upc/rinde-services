package com.nexus.rinde.expense.domain.model.valueobjects;

/** Motivo por el que un gasto de un lote offline no se pudo sincronizar. */
public enum SyncRejectionCode {
  TRIP_NOT_FOUND,
  TRIP_NOT_ASSIGNED,
  TRIP_NOT_STARTED,
  TRIP_SETTLED,
  INVALID_EXPENSE,
  DUPLICATE_KEY
}
