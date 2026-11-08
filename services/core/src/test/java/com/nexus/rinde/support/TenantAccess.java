package com.nexus.rinde.support;

import java.util.UUID;

/**
 * Registra empresas verificadas y emite sus tokens de acceso. Las cuentas quedan en ContextDoubles,
 * que hace de IAM en las pruebas; la contraseña no se valida porque el inicio de sesión ya no es de core.
 */
public class TenantAccess {

  private final ContextDoubles doubles;
  private final TestTokens tokens;

  public TenantAccess(ContextDoubles doubles, TestTokens tokens) {
    this.doubles = doubles;
    this.tokens = tokens;
  }

  /** Registra la empresa como verificada, con su administrador activo, y devuelve su id. */
  public String registerAndVerifyTenant(String administratorEmail) {
    UUID tenantId = UUID.randomUUID();
    doubles.registerTenant(tenantId, administratorEmail);
    doubles.addAccount(
        new ContextDoubles.Account(
            UUID.randomUUID(), tenantId, "ADMINISTRATOR", administratorEmail));
    return tenantId.toString();
  }

  /** Token de acceso de una cuenta registrada en la prueba. */
  public String accessTokenFor(String email) {
    ContextDoubles.Account account =
        doubles
            .accountByEmail(email)
            .orElseThrow(() -> new IllegalStateException("No existe la cuenta " + email));
    return tokens.issue(account.userId(), account.tenantId(), account.role(), account.email());
  }
}
