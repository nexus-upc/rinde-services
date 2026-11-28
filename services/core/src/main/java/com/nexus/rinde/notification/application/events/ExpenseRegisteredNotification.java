package com.nexus.rinde.notification.application.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Contrato propio de Notifications para recibir expense.registered sin importar el dominio de Expense. */
public record ExpenseRegisteredNotification(
    UUID eventId,
    UUID tenantId,
    UUID expenseId,
    UUID tripId,
    UUID driverId,
    BigDecimal amount,
    String currency,
    Instant occurredAt) {

  public ExpenseRegisteredNotification {
    Objects.requireNonNull(eventId, "Falta el identificador del evento.");
    Objects.requireNonNull(occurredAt, "Falta la fecha del evento.");
    Objects.requireNonNull(tenantId, "Falta la empresa.");
    Objects.requireNonNull(tripId, "Falta el viaje.");
    Objects.requireNonNull(amount, "Falta el monto del gasto.");
    if (currency == null || currency.length() != 3) {
      throw new IllegalArgumentException("La moneda del gasto no es válida.");
    }
  }
}
