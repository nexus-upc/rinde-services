package com.nexus.rinde.fleet.interfaces.rest.transform;

import com.nexus.rinde.fleet.domain.model.commands.LinkDriverAccountCommand;
import com.nexus.rinde.fleet.domain.model.commands.RegisterDriverCommand;
import com.nexus.rinde.fleet.interfaces.rest.resources.CreateDriverResource;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import java.util.Map;
import java.util.UUID;

/** Transformador de recurso REST a comandos de Driver. */
public final class DriverCommandAssembler {

  private DriverCommandAssembler() {}

  public static RegisterDriverCommand toCommand(UUID tenantId, CreateDriverResource resource) {
    return new RegisterDriverCommand(
        tenantId,
        resource.userId(),
        resource.fullName(),
        resource.documentType(),
        resource.documentNumber(),
        resource.licenseNumber(),
        resource.licenseCategory(),
        resource.licenseExpirationDate());
  }

  /** Exige la clave userId en el cuerpo: con null se retira el vínculo, sin la clave no se hace nada. */
  public static LinkDriverAccountCommand toLinkCommand(
      UUID tenantId, UUID driverId, Map<String, Object> body) {
    if (body == null || !body.containsKey("userId")) {
      throw new BusinessRuleException(
          "Indique el identificador de la cuenta, o null para retirar el vínculo.");
    }
    Object value = body.get("userId");
    return new LinkDriverAccountCommand(tenantId, driverId, value == null ? null : accountId(value));
  }

  private static UUID accountId(Object value) {
    try {
      return UUID.fromString(value.toString());
    } catch (IllegalArgumentException ex) {
      throw new BusinessRuleException("El identificador de la cuenta no es válido.");
    }
  }
}
