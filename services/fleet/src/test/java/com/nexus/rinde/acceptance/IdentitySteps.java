package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.support.IamStub;
import com.nexus.rinde.support.TestTokens;
import io.cucumber.java.es.Dado;
import java.util.UUID;

/**
 * Pasos de empresas, sesiones e invitaciones. Las empresas y los usuarios se crean en la prueba y
 * sus tokens se firman igual que lo haría IAM; no se llama a IAM.
 */
public class IdentitySteps {

  private final ScenarioContext context;
  private final IamStub iam;
  private final TestTokens tokens;

  public IdentitySteps(ScenarioContext context, IamStub iam, TestTokens tokens) {
    this.context = context;
    this.iam = iam;
    this.tokens = tokens;
  }

  @Dado("que existe una empresa verificada con RUC {string} y administrador {string}")
  public void aVerifiedTenantExists(String ruc, String administratorEmail) {
    UUID tenantId = UUID.randomUUID();
    context.rememberTenant(ruc, tenantId);
    context.rememberUser(administratorEmail, UUID.randomUUID(), tenantId, "ADMINISTRATOR");
    context.setCurrentTenantId(tenantId);
  }

  @Dado("que el administrador {string} inició sesión")
  public void theAdministratorSignedIn(String email) {
    String token = context.accessTokenOf(email);
    context.rememberAccessToken(email, token);
    context.setCurrentAdministratorToken(token);
  }

  @Dado("que el administrador invitó a {string} con el correo {string} y el rol {string}")
  public void theAdministratorInvited(String fullName, String email, String role) {
    UUID userId = UUID.randomUUID();
    context.rememberUser(email, userId, context.currentTenantId(), role);
  }

  @Dado("que {string} definió la contraseña {string} con su enlace de invitación")
  public void thePasswordWasDefined(String email, String password) {
    assertThat(context.userIdOf(email)).isNotNull();
    context.rememberAccessToken(email, context.accessTokenOf(email));
  }

  @Dado("que el conductor {string} inició sesión")
  public void theDriverSignedIn(String email) {
    assertThat(context.userIdOf(email)).isNotNull();
    context.rememberAccessToken(email, context.accessTokenOf(email));
  }

  /** IAM informa la cuenta en la empresa donde el usuario fue invitado, con el rol indicado. */
  @Dado("que IAM informa la cuenta de {string} con el rol {string}")
  public void iamReportsAccount(String email, String role) {
    iam.registerAccount(context.tenantOfUser(email), context.userIdOf(email), role);
  }

  /** IAM informa la cuenta con su estado, por ejemplo INVITED o DISABLED. */
  @Dado("que IAM informa la cuenta de {string} con el rol {string} y el estado {string}")
  public void iamReportsAccountWithStatus(String email, String role, String status) {
    iam.registerAccount(context.tenantOfUser(email), context.userIdOf(email), role, status);
  }

  /** IAM responde RESTRICTED para la empresa desde ahora; el filtro lo lee en cada petición. */
  @Dado("que la suscripción de la empresa con RUC {string} está restringida")
  public void theTenantSubscriptionIsRestricted(String ruc) {
    iam.restrictTenant(context.tenantIdOf(ruc));
  }
}
