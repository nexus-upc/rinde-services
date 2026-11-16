package com.nexus.rinde.fleet.application.internal.outboundservices.acl;

import java.util.Optional;
import java.util.UUID;

/** Puerto hacia IAM para validar la cuenta que se vincula a un conductor. */
public interface IamAccountClient {

  /** Devuelve la cuenta del usuario en la empresa; vacío si no existe en ella. */
  Optional<IamUserAccount> findUserAccount(UUID tenantId, UUID userId);
}
