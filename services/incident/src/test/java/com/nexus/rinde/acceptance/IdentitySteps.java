package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Dado;
import java.util.UUID;

/** Pasos de empresas, usuarios y sesiones para Incident Management. */
public class IdentitySteps {

  private final ScenarioContext context;

  public IdentitySteps(ScenarioContext context) {
    this.context = context;
  }

  @Dado("que existe una empresa verificada con RUC {string} y administrador {string}")
  public void aVerifiedTenantExists(String ruc, String administratorEmail) {
    UUID tenantId = UUID.randomUUID();
    context.rememberTenant(ruc, tenantId);
    context.rememberUser(administratorEmail, UUID.randomUUID(), tenantId, "ADMINISTRATOR");
    context.setCurrentTenantId(tenantId);
    context.setCurrentAdminEmail(administratorEmail);
  }

  @Dado("que el administrador {string} inició sesión")
  public void theAdministratorSignedIn(String email) {
    assertThat(context.userIdOf(email)).isNotNull();
    context.accessTokenOf(email);
  }

  @Dado("que el administrador invitó a {string} con el correo {string} y el rol {string}")
  public void theAdministratorInvited(String fullName, String email, String role) {
    context.rememberUser(email, UUID.randomUUID(), context.currentTenantId(), role);
  }

  @Dado("que {string} definió la contraseña {string} con su enlace de invitación")
  public void thePasswordWasDefined(String email, String password) {
    assertThat(context.userIdOf(email)).isNotNull();
  }

  @Dado("que el conductor {string} inició sesión")
  public void theDriverSignedIn(String email) {
    assertThat(context.userIdOf(email)).isNotNull();
    context.accessTokenOf(email);
  }

  @Dado("que existe otra empresa verificada con RUC {string} y administrador {string}")
  public void anotherTenantExists(String ruc, String adminEmail) {
    UUID tenantId = UUID.randomUUID();
    context.rememberTenant(ruc, tenantId);
    context.rememberUser(adminEmail, UUID.randomUUID(), tenantId, "ADMINISTRATOR");
  }

  @Dado("que el administrador de la otra empresa {string} inició sesión")
  public void anotherAdminSignedIn(String email) {
    assertThat(context.userIdOf(email)).isNotNull();
    context.accessTokenOf(email);
  }
}
