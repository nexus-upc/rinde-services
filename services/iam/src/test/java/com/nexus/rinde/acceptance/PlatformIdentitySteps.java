package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.iam.application.events.SubscriptionStatusMessage;
import com.nexus.rinde.iam.application.internal.eventhandlers.SubscriptionMessageHandler;
import com.nexus.rinde.iam.infrastructure.persistence.jpa.repositories.TenantRepository;
import com.nexus.rinde.iam.platform.domain.PlatformOwner;
import com.nexus.rinde.iam.platform.infrastructure.PlatformOwnerRepository;
import com.nexus.rinde.support.CapturedEvents;
import com.nexus.rinde.support.IamApi;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Escenarios de identidad, acceso y auditoría del dueño de Rinde (US41, US42 y US45). */
public class PlatformIdentitySteps {
  private static final String OWNER_EMAIL = "owner@example.invalid";
  private static final String OWNER_PASSWORD = "TestOwnerPassword!42";
  private static final String COMPANY_EMAIL = "company@example.invalid";
  private static final String COMPANY_RUC = "20123456789";
  private final ScenarioContext context;
  private final MockMvc mvc;
  private final ObjectMapper mapper;
  private final PlatformOwnerRepository owners;
  private final PasswordEncoder encoder;
  private final TenantRepository tenants;
  private final SubscriptionMessageHandler subscriptions;
  private final IamApi api;
  private String ownerToken;
  private String companyToken;
  private UUID companyId;
  private boolean rejectedSessions;
  private boolean auditMutationRejected;

  public PlatformIdentitySteps(ScenarioContext context, MockMvc mvc, ObjectMapper mapper,
      PlatformOwnerRepository owners, PasswordEncoder encoder, TenantRepository tenants,
      SubscriptionMessageHandler subscriptions, CapturedEvents events) {
    this.context = context;
    this.mvc = mvc;
    this.mapper = mapper;
    this.owners = owners;
    this.encoder = encoder;
    this.tenants = tenants;
    this.subscriptions = subscriptions;
    this.api = new IamApi(mvc, mapper, events);
  }

  @Dado("que existe un dueño de plataforma para la prueba")
  public void ownerExists() {
    owners.saveAndFlush(new PlatformOwner("Dueño de prueba", OWNER_EMAIL,
        encoder.encode(OWNER_PASSWORD), Instant.now()));
  }

  @Dado("que existe una empresa activa para la prueba de plataforma")
  public void companyExists() throws Exception {
    companyId = UUID.fromString(api.registerAndVerifyTenant(
        COMPANY_RUC, COMPANY_EMAIL, ScenarioContext.PASSWORD));
    companyToken = api.signInForToken(COMPANY_EMAIL, ScenarioContext.PASSWORD);
  }

  @Cuando("el dueño inicia sesión en el acceso de plataforma")
  public void ownerSignsIn() throws Exception {
    signIn(OWNER_PASSWORD);
  }

  @Cuando("el dueño intenta ingresar con una contraseña incorrecta")
  public void invalidPassword() throws Exception {
    signIn("IncorrectPassword!42");
  }

  @Dado("que el dueño tiene una sesión de plataforma")
  public void ownerHasSession() throws Exception {
    signIn(OWNER_PASSWORD);
    assertThat(context.statusCode()).isEqualTo(200);
  }

  private void signIn(String password) throws Exception {
    context.record(mvc.perform(post("/api/v1/platform/auth/sign-in")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(Map.of("email", OWNER_EMAIL, "password", password)))));
    if (context.statusCode() == 200) ownerToken = context.body().get("accessToken").asText();
  }

  @Y("la sesión permite consultar el perfil de dueño sin crear una empresa")
  public void independentProfile() throws Exception {
    context.record(mvc.perform(get("/api/v1/platform/me").header("Authorization", "Bearer " + ownerToken)));
    assertThat(context.statusCode()).isEqualTo(200);
    assertThat(context.body().get("role").asText()).isEqualTo("PLATFORM_OWNER");
    assertThat(context.body().get("email").asText()).isEqualTo(OWNER_EMAIL);
    assertThat(tenants.count()).isZero();
  }

  @Cuando("se intenta intercambiar las sesiones de empresa y plataforma")
  public void exchangeSessions() throws Exception {
    int companyStatus = mvc.perform(get("/api/v1/platform/companies")
        .header("Authorization", "Bearer " + companyToken)).andReturn().getResponse().getStatus();
    int ownerStatus = mvc.perform(get("/api/v1/users")
        .header("Authorization", "Bearer " + ownerToken)).andReturn().getResponse().getStatus();
    rejectedSessions = companyStatus == 401 && ownerStatus == 401;
  }

  @Entonces("ambas solicitudes de acceso se rechazan")
  public void sessionsRejected() { assertThat(rejectedSessions).isTrue(); }

  @Cuando("el administrador intenta invitar al rol de plataforma")
  public void inviteOwner() throws Exception {
    context.record(api.inviteUser(companyToken, "Invitado", "invite@example.invalid", "PLATFORM_OWNER"));
  }

  @Cuando("el dueño busca la empresa por su RUC")
  public void search() throws Exception {
    context.record(mvc.perform(get("/api/v1/platform/companies").param("search", COMPANY_RUC)
        .param("page", "0").param("size", "1").header("Authorization", "Bearer " + ownerToken)));
  }

  @Y("la búsqueda devuelve la empresa con su estado comercial y administrativo")
  public void searchResult() throws Exception {
    assertThat(context.body().get("total").asInt()).isEqualTo(1);
    var item = context.body().get("items").get(0);
    assertThat(item.get("ruc").asText()).isEqualTo(COMPANY_RUC);
    assertThat(item.get("status").asText()).isEqualTo("ACTIVE");
    assertThat(item.get("accessSuspended").asBoolean()).isFalse();
  }

  @Cuando("el dueño suspende la empresa con motivo {string}")
  public void suspend(String reason) throws Exception { changeAccess(true, reason); }

  @Cuando("el dueño restablece la empresa con motivo {string}")
  public void restore(String reason) throws Exception { changeAccess(false, reason); }

  private void changeAccess(boolean suspended, String reason) throws Exception {
    context.record(mvc.perform(patch("/api/v1/platform/companies/" + companyId + "/access")
        .header("Authorization", "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(Map.of("suspended", suspended, "reason", reason)))));
  }

  @Dado("que el dueño suspendió la empresa para revisión")
  public void suspendedFixture() throws Exception {
    changeAccess(true, "Revisión de soporte");
    assertThat(context.statusCode()).isEqualTo(200);
  }

  @Y("la sesión existente de la empresa queda bloqueada")
  public void existingSessionBlocked() throws Exception {
    api.listUsers(companyToken).andExpect(status().isForbidden());
  }

  @Cuando("se recibe una activación comercial de la suscripción")
  public void paymentActivation() {
    subscriptions.handle(new SubscriptionStatusMessage(UUID.randomUUID(), Instant.now(), companyId),
        "subscription.activated");
  }

  @Entonces("la suscripción queda activa y la empresa sigue suspendida")
  public void billingIndependent() {
    var tenant = tenants.findById(companyId).orElseThrow();
    assertThat(tenant.getSubscriptionStatus().name()).isEqualTo("ACTIVE");
    assertThat(tenant.isAccessSuspended()).isTrue();
  }

  @Dado("que la suscripción de la empresa está restringida")
  public void billingRestricted() {
    var tenant = tenants.findById(companyId).orElseThrow();
    tenant.restrict();
    tenants.saveAndFlush(tenant);
  }

  @Y("la empresa conserva su restricción comercial")
  public void stillRestricted() throws Exception {
    var tenant = tenants.findById(companyId).orElseThrow();
    assertThat(tenant.isAccessSuspended()).isFalse();
    assertThat(tenant.getSubscriptionStatus().name()).isEqualTo("RESTRICTED");
    assertThat(context.body().get("status").asText()).isEqualTo("RESTRICTED");
    assertThat(context.body().get("accessSuspended").asBoolean()).isFalse();
  }

  @Y("el acceso de la empresa permanece habilitado")
  public void remainsEnabled() throws Exception {
    assertThat(tenants.findById(companyId).orElseThrow().isAccessSuspended()).isFalse();
    api.listUsers(companyToken).andExpect(status().isOk());
  }

  @Cuando("el dueño consulta la auditoría de identidad")
  public void readAudit() throws Exception {
    context.record(mvc.perform(get("/api/v1/platform/audit/identity")
        .header("Authorization", "Bearer " + ownerToken)));
  }

  @Y("la auditoría conserva los datos de la suspensión")
  public void auditMetadata() throws Exception {
    assertThat(context.body().get("total").asInt()).isEqualTo(1);
    var entry = context.body().get("items").get(0);
    assertThat(entry.get("actorEmail").asText()).isEqualTo(OWNER_EMAIL);
    assertThat(entry.get("targetId").asText()).isEqualTo(companyId.toString());
    assertThat(entry.get("reason").asText()).isEqualTo("Revisión de soporte");
    assertThat(entry.get("beforeState").asText()).isEqualTo("false");
    assertThat(entry.get("afterState").asText()).isEqualTo("true");
    assertThat(entry.get("action").asText()).isEqualTo("COMPANY_ACCESS_SUSPENDED");
    assertThat(Instant.parse(entry.get("occurredAt").asText())).isNotNull();
  }

  @Cuando("el dueño intenta editar y eliminar la entrada de auditoría")
  public void mutateAudit() throws Exception {
    readAudit();
    String path = "/api/v1/platform/audit/identity/" + context.body().get("items").get(0).get("id").asText();
    int edit = mvc.perform(patch(path).header("Authorization", "Bearer " + ownerToken)
        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Alterado\"}"))
        .andReturn().getResponse().getStatus();
    int remove = mvc.perform(delete(path).header("Authorization", "Bearer " + ownerToken))
        .andReturn().getResponse().getStatus();
    auditMutationRejected = (edit == 404 || edit == 405) && (remove == 404 || remove == 405);
    readAudit();
  }

  @Entonces("las operaciones no están disponibles y la entrada se conserva")
  public void auditUnchanged() throws Exception {
    assertThat(auditMutationRejected).isTrue();
    auditMetadata();
  }

  @Cuando("un administrador de otra empresa consulta la auditoría de identidad")
  public void companyReadsAudit() throws Exception {
    api.registerAndVerifyTenant("20987654321", "other@example.invalid", ScenarioContext.PASSWORD);
    String token = api.signInForToken("other@example.invalid", ScenarioContext.PASSWORD);
    context.record(mvc.perform(get("/api/v1/platform/audit/identity")
        .header("Authorization", "Bearer " + token)));
  }
}
