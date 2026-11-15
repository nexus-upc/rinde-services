package com.nexus.rinde.support;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Cuentas de usuario de las pruebas. Con IAM fuera de este servicio, los tokens se emiten para estas
 * cuentas; el estado de cada empresa lo responde IamStub por HTTP.
 */
@Component
public class TenantAccounts {

  /** Cuenta de usuario con la que se emite un token de prueba. */
  public record Account(UUID userId, UUID tenantId, String role, String email) {}

  private final Map<String, Account> accounts = new ConcurrentHashMap<>();

  public void reset() {
    accounts.clear();
  }

  public void addAccount(Account account) {
    accounts.put(account.email(), account);
  }

  public Optional<Account> accountByEmail(String email) {
    return Optional.ofNullable(accounts.get(email));
  }
}
