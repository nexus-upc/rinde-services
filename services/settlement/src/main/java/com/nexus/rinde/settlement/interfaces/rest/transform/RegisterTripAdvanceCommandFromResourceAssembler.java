package com.nexus.rinde.settlement.interfaces.rest.transform;

import com.nexus.rinde.settlement.domain.model.commands.RegisterTripAdvanceCommand;
import com.nexus.rinde.settlement.domain.model.valueobjects.AdvanceDeliveryMethod;
import com.nexus.rinde.settlement.interfaces.rest.resources.RegisterTripAdvanceResource;
import java.util.UUID;

/** Convierte RegisterTripAdvanceResource en RegisterTripAdvanceCommand. */
public class RegisterTripAdvanceCommandFromResourceAssembler {

  private RegisterTripAdvanceCommandFromResourceAssembler() {}

  public static RegisterTripAdvanceCommand toCommand(
      UUID tenantId, UUID registeredBy, UUID tripId, RegisterTripAdvanceResource resource) {
    return new RegisterTripAdvanceCommand(
        tenantId,
        tripId,
        registeredBy,
        resource.amount(),
        resource.currency(),
        AdvanceDeliveryMethod.valueOf(resource.deliveryMethod()));
  }
}
