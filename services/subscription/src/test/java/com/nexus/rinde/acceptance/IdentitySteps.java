package com.nexus.rinde.acceptance;

import com.nexus.rinde.support.TenantAccess;
import com.nexus.rinde.support.TenantAccounts;
import com.nexus.rinde.support.TenantAccounts.Account;
import com.nexus.rinde.support.TestTokens;
import io.cucumber.java.es.Dado;
import java.util.UUID;

/**
 * Pasos de empresas, administradores e invitaciones para Subscriptions. Son los que necesitan los
 * escenarios de US38, US39, US40, US43, US44 y US45; el resto de la preparación de core no aplica aquí.
 */
public class IdentitySteps {

  private final ScenarioContext context;
  private final TenantAccounts accounts;
  private final TestTokens tokens;

  public IdentitySteps(ScenarioContext context, TenantAccounts accounts, TestTokens tokens) {
    this.context = context;
    this.accounts = accounts;
    this.tokens = tokens;
  }

  @Dado("que existe una empresa verificada con RUC {string} y administrador {string}")
  public void aVerifiedTenantExists(String ruc, String administratorEmail) {
    String tenantId = access().registerAndVerifyTenant(administratorEmail);
    context.rememberTenantId(ruc, tenantId);
    context.setCurrentTenantId(tenantId);
    accounts
        .accountByEmail(administratorEmail)
        .ifPresent(account -> context.rememberUserId(administratorEmail, account.userId().toString()));
  }

  @Dado("que el administrador {string} inició sesión")
  public void theAdministratorSignsIn(String email) {
    String token = access().accessTokenFor(email);
    context.rememberAccessToken(email, token);
    context.setCurrentAdministratorToken(token);
    context.setCurrentTenantId(accounts.accountByEmail(email).orElseThrow().tenantId().toString());
  }

  @Dado("que el administrador invitó a {string} con el correo {string} y el rol {string}")
  public void theAdministratorInvitesUser(String fullName, String email, String role) {
    UUID userId = UUID.randomUUID();
    accounts.addAccount(new Account(userId, UUID.fromString(context.currentTenantId()), role, email));
    context.rememberUserId(email, userId.toString());
  }

  /** Al definir la contraseña la cuenta queda activa y el usuario puede usar su token. */
  @Dado("que {string} definió la contraseña {string} con su enlace de invitación")
  public void theInvitedUserDefinesPassword(String email, String password) {
    context.rememberAccessToken(email, access().accessTokenFor(email));
  }

  private TenantAccess access() {
    return new TenantAccess(accounts, tokens);
  }
}
