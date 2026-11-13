package com.nexus.rinde.settlement.interfaces.rest.transform;

import com.nexus.rinde.settlement.domain.model.commands.RegisterAdvanceCommand;
import com.nexus.rinde.settlement.interfaces.rest.resources.RegisterAdvanceResource;
import java.util.UUID;

/** Convierte RegisterAdvanceResource en RegisterAdvanceCommand. */
public class RegisterAdvanceCommandFromResourceAssembler {

  private RegisterAdvanceCommandFromResourceAssembler() {}

  public static RegisterAdvanceCommand toCommand(
      UUID tenantId, UUID registeredBy, RegisterAdvanceResource resource) {
    return new RegisterAdvanceCommand(
        tenantId,
        resource.tripId(),
        registeredBy,
        resource.amount(),
        resource.currency() != null ? resource.currency() : "PEN");
  }
}
