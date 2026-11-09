package com.nexus.rinde.expense.interfaces.rest.transform;

import com.nexus.rinde.expense.domain.model.commands.RegisterExpenseCommand;
import com.nexus.rinde.expense.domain.model.valueobjects.Money;
import com.nexus.rinde.expense.interfaces.rest.resources.RegisterExpenseResource;
import java.util.UUID;

/** Convierte el request de registro de gasto en el comando del caso de uso. */
public final class RegisterExpenseCommandFromResourceAssembler {

  private RegisterExpenseCommandFromResourceAssembler() {}

  public static RegisterExpenseCommand toCommand(
      UUID tenantId, UUID tripId, UUID driverId, RegisterExpenseResource resource) {
    Money money = new Money(resource.amount(), resource.currency());
    return new RegisterExpenseCommand(
        tenantId,
        tripId,
        driverId,
        resource.category(),
        money,
        resource.expenseDate(),
        resource.idempotencyKey());
  }
}
