package com.nexus.rinde.support;

import java.util.UUID;

/**
 * Registra empresas verificadas y emite sus tokens de acceso. Las cuentas quedan en TenantAccounts y
 * la empresa figura como ACTIVE en IamStub, que responde ACTIVE para cualquier empresa. La contraseña
 * no se valida porque el inicio de sesión no es de este servicio.
 */
public class TenantAccess {

  private final TenantAccounts accounts;
  private final TestTokens tokens;

  public TenantAccess(TenantAccounts accounts, TestTokens tokens) {
    this.accounts = accounts;
    this.tokens = tokens;
  }

  /** Registra la empresa como verificada, con su administrador activo, y devuelve su id. */
  public String registerAndVerifyTenant(String administratorEmail) {
    UUID tenantId = UUID.randomUUID();
    accounts.addAccount(
        new TenantAccounts.Account(UUID.randomUUID(), tenantId, "ADMINISTRATOR", administratorEmail));
    return tenantId.toString();
  }

  /** Token de acceso de una cuenta registrada en la prueba. */
  public String accessTokenFor(String email) {
    TenantAccounts.Account account =
        accounts
            .accountByEmail(email)
            .orElseThrow(() -> new IllegalStateException("No existe la cuenta " + email));
    return tokens.issue(account.userId(), account.tenantId(), account.role(), account.email());
  }
}
