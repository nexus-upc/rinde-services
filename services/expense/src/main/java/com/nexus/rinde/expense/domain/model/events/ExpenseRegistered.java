package com.nexus.rinde.expense.domain.model.events;

import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Evento publicado al registrar un gasto nuevo para que Notifications avise a la empresa. */
public record ExpenseRegistered(
    UUID eventId,
    UUID tenantId,
    UUID expenseId,
    UUID tripId,
    UUID driverId,
    BigDecimal amount,
    String currency,
    Instant occurredAt)
    implements IntegrationEvent {

  @Override
  public String type() {
    return "ExpenseRegistered";
  }
}
