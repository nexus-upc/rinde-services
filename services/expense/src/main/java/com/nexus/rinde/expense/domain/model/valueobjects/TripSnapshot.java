package com.nexus.rinde.expense.domain.model.valueobjects;

import java.util.UUID;

/** Datos del viaje que Expense necesita para validar el registro de gastos. */
public record TripSnapshot(UUID tripId, String status, UUID driverId) {

  public boolean isAssignedTo(UUID otherDriverId) {
    return otherDriverId != null && otherDriverId.equals(driverId);
  }
}
