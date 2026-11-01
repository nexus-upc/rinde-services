package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.shared.infrastructure.security.JwtClaims;
import com.nexus.rinde.subscription.domain.model.aggregates.Payment;
import com.nexus.rinde.subscription.domain.model.aggregates.Plan;
import com.nexus.rinde.subscription.domain.model.aggregates.Subscription;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.PaymentRepository;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import com.nexus.rinde.subscription.platform.infrastructure.PlatformCompanyDirectory;
import com.nexus.rinde.support.CapturedEvents;
import com.nexus.rinde.support.IamApi;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import io.jsonwebtoken.Jwts;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Pasos del panel del dueño de Rinde: catálogo, pagos y auditoría (US43, US44 y US45). */
public class PlatformBillingSteps {

  private static final String COMPANY_EMAIL = "admin@catalogo.pe";

  private final ScenarioContext context;
  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  private final CapturedEvents capturedEvents;
  private final PlanRepository planRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final PaymentRepository paymentRepository;
  private final PlatformCompanyDirectory companyDirectory;
  private final Map<String, UUID> companies = new HashMap<>();
  private String ownerToken;
  private String companyToken;
  private UUID pendingSubscriptionId;

  public PlatformBillingSteps(
      ScenarioContext context,
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      CapturedEvents capturedEvents,
      PlanRepository planRepository,
      SubscriptionRepository subscriptionRepository,
      PaymentRepository paymentRepository,
      PlatformCompanyDirectory companyDirectory) {
    this.context = context;
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.capturedEvents = capturedEvents;
    this.planRepository = planRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.paymentRepository = paymentRepository;
    this.companyDirectory = companyDirectory;
  }

  @Dado("que el dueño de Rinde inició sesión en el panel de la plataforma")
  public void theOwnerSignedIn() {
    ownerToken =
        Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .issuer("rinde-platform")
            .audience()
            .add("rinde-platform-api")
            .and()
            .claim("scope", "PLATFORM")
            .claim("role", "PLATFORM_OWNER")
            .claim("email", "owner@example.invalid")
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(JwtClaims.signingKey(CucumberSpringConfiguration.PLATFORM_SECRET))
            .compact();
  }

  @Cuando(
      "el dueño registra el plan {string} con precio mensual {string}, límite {int} y motivo {string}")
  @Dado(
      "que el dueño registró el plan {string} con precio mensual {string}, límite {int} y motivo {string}")
  public void theOwnerCreatesAPlan(String name, String price, int unitLimit, String reason)
      throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("name", name);
    body.put("monthlyPrice", new BigDecimal(price));
    body.put("unitLimit", unitLimit);
    body.put("reason", reason);
    context.record(mockMvc.perform(asOwner(post("/api/v1/platform/plans")).content(json(body))));
  }

  @Y("el plan {string} queda publicado con precio {string} y límite {int}")
  public void thePlanIsPublished(String name, String price, int unitLimit) {
    Plan plan = plan(name);
    assertThat(plan.isActive()).isTrue();
    assertThat(plan.getMonthlyPrice()).isEqualByComparingTo(price);
    assertThat(plan.getUnitLimit()).isEqualTo(unitLimit);
  }

  @Y("las empresas ven el plan {string} en el catálogo")
  public void companiesSeeThePlan(String name) throws Exception {
    assertThat(companyCatalog()).contains(name);
  }

  @Y("las empresas no ven el plan {string} en el catálogo")
  public void companiesDoNotSeeThePlan(String name) throws Exception {
    assertThat(companyCatalog()).doesNotContain(name);
  }

  @Y("que una empresa tiene una suscripción pendiente del plan {string}")
  public void aCompanyHasAPendingSubscription(String planName) {
    pendingSubscriptionId =
        subscriptionRepository
            .saveAndFlush(Subscription.pendingPayment(UUID.randomUUID(), plan(planName).getPlanId()))
            .getSubscriptionId();
  }

  @Cuando("el dueño intenta cambiar el precio del plan {string} a {string}")
  public void theOwnerTriesToChangeThePrice(String name, String price) throws Exception {
    String body = json(Map.of("monthlyPrice", new BigDecimal(price)));
    context.record(
        mockMvc.perform(
            asOwner(patch("/api/v1/platform/plans/" + plan(name).getPlanId())).content(body)));
  }

  @Entonces("la respuesta no permite modificar el plan")
  public void theChangeIsNotAllowed() {
    assertThat(context.statusCode()).isIn(404, 405);
    assertThat(plan("Plan Base").getMonthlyPrice()).isEqualByComparingTo("80.00");
  }

  @Cuando("el dueño retira el plan {string} con motivo {string}")
  public void theOwnerWithdrawsThePlan(String name, String reason) throws Exception {
    changeAvailability(name, false, reason);
  }

  @Cuando("el dueño vuelve a publicar el plan {string} con motivo {string}")
  public void theOwnerRepublishesThePlan(String name, String reason) throws Exception {
    changeAvailability(name, true, reason);
  }

  @Entonces("la suscripción de la empresa conserva el plan {string} con precio {string}")
  public void theSubscriptionKeepsItsPlan(String name, String price) {
    Subscription subscription = subscriptionRepository.findById(pendingSubscriptionId).orElseThrow();
    Plan plan = planRepository.findById(subscription.getPlanId()).orElseThrow();
    assertThat(plan.getName()).isEqualTo(name);
    assertThat(plan.getMonthlyPrice()).isEqualByComparingTo(price);
  }

  @Y("la auditoría del catálogo registra {int} cambios")
  public void theAuditHasChanges(int changes) throws Exception {
    context.record(mockMvc.perform(asOwner(get("/api/v1/platform/audit/billing"))));
    assertThat(context.body().get("total").asInt()).isEqualTo(changes);
  }

  @Cuando("el dueño consulta la auditoría del catálogo")
  public void theOwnerReadsCatalogAudit() throws Exception {
    context.record(mockMvc.perform(asOwner(get("/api/v1/platform/audit/billing"))));
  }

  @Entonces("la auditoría del catálogo conserva actor, fecha, motivos y estados")
  public void catalogAuditPreservesMetadata() throws Exception {
    assertThat(context.statusCode()).isEqualTo(200);
    List<JsonNode> entries = items();
    assertThat(entries).hasSize(3);
    assertThat(entries).extracting(entry -> entry.get("action").asText())
        .containsExactlyInAnyOrder("PLAN_CREATED", "PLAN_WITHDRAWN", "PLAN_PUBLISHED");
    assertThat(entries).extracting(entry -> entry.get("reason").asText())
        .containsExactlyInAnyOrder("Alta para auditoría", "Retiro por revisión", "Reactivación");
    for (JsonNode entry : entries) {
      assertThat(entry.get("actorEmail").asText()).isEqualTo("owner@example.invalid");
      assertThat(entry.get("targetType").asText()).isEqualTo("PLAN");
      assertThat(entry.get("targetId").asText()).isEqualTo(plan("Plan Auditoría").getPlanId().toString());
      assertThat(Instant.parse(entry.get("occurredAt").asText()))
          .isBetween(Instant.now().minusSeconds(300), Instant.now().plusSeconds(5));
      String action = entry.get("action").asText();
      assertThat(entry.get("beforeState").asText()).endsWith(
          "PLAN_CREATED".equals(action) ? "absent" : "PLAN_WITHDRAWN".equals(action) ? "true" : "false");
      assertThat(entry.get("afterState").asText()).endsWith(
          "PLAN_WITHDRAWN".equals(action) ? "false" : "true");
    }
  }

  @Cuando("el dueño intenta editar y eliminar la entrada de auditoría del catálogo")
  public void theOwnerTriesToChangeCatalogAudit() throws Exception {
    theOwnerReadsCatalogAudit();
    JsonNode original = context.body().deepCopy();
    String path = "/api/v1/platform/audit/billing/" + items().get(0).get("id").asText();
    int editStatus = mockMvc.perform(asOwner(patch(path)).content(json(Map.of("reason", "Alterado"))))
        .andReturn().getResponse().getStatus();
    int deleteStatus = mockMvc.perform(asOwner(delete(path))).andReturn().getResponse().getStatus();
    assertThat(editStatus).isIn(404, 405);
    assertThat(deleteStatus).isIn(404, 405);
    theOwnerReadsCatalogAudit();
    assertThat(context.statusCode()).isEqualTo(200);
    assertThat(context.body()).isEqualTo(original);
  }

  @Cuando("un administrador de empresa consulta la auditoría del catálogo")
  public void aCompanyReadsCatalogAudit() throws Exception {
    context.record(mockMvc.perform(get("/api/v1/platform/audit/billing")
        .header("Authorization", "Bearer " + companyToken())));
  }

  @Y("no se registra el plan {string}")
  public void thePlanIsNotRegistered(String name) {
    assertThat(planRepository.findAll()).noneMatch(plan -> name.equals(plan.getName()));
  }

  @Y(
      "que la empresa {string} tiene una suscripción activa del plan {string} con un pago confirmado de {string}")
  public void aCompanyHasAnActiveSubscription(String company, String planName, String amount) {
    UUID tenantId = company(company);
    Subscription subscription =
        Subscription.pendingPayment(tenantId, planNamed(planName, amount).getPlanId());
    subscription.applyConfirmedPayment(Instant.now());
    subscription = subscriptionRepository.saveAndFlush(subscription);
    Payment payment =
        Payment.pending(
            tenantId,
            subscription.getSubscriptionId(),
            "test-" + tenantId,
            new BigDecimal(amount),
            Instant.now());
    payment.confirm("confirmation-" + tenantId);
    paymentRepository.saveAndFlush(payment);
  }

  @Y("que la empresa {string} tiene una suscripción pendiente de pago del plan {string}")
  public void aCompanyHasAPendingPayment(String company, String planName) {
    subscriptionRepository.saveAndFlush(
        Subscription.pendingPayment(company(company), planNamed(planName, "120.00").getPlanId()));
  }

  @Cuando("el dueño abre el resumen comercial")
  public void theOwnerOpensTheOverview() throws Exception {
    context.record(mockMvc.perform(asOwner(get("/api/v1/platform/billing/overview"))));
  }

  @Y("el resumen muestra {int} suscripciones, {int} pendiente de pago y {string} en pagos confirmados")
  public void theOverviewShows(int subscriptions, int pending, String amount) throws Exception {
    JsonNode body = context.body();
    assertThat(body.get("subscriptions").asInt()).isEqualTo(subscriptions);
    assertThat(body.get("pendingSubscriptions").asInt()).isEqualTo(pending);
    assertThat(body.get("confirmedAmount").decimalValue()).isEqualByComparingTo(amount);
  }

  @Entonces("el resumen indica que el modo de pago es {string}")
  public void theOverviewShowsThePaymentMode(String mode) throws Exception {
    assertThat(context.body().get("paymentMode").asText()).isEqualTo(mode);
  }

  @Cuando("el dueño consulta las suscripciones de la empresa {string}")
  public void theOwnerListsSubscriptions(String company) throws Exception {
    context.record(
        mockMvc.perform(
            asOwner(get("/api/v1/platform/subscriptions").param("tenantId", company(company).toString()))));
  }

  @Y("cada registro muestra la empresa {string}, el plan {string} y su estado")
  public void eachSubscriptionShows(String company, String planName) throws Exception {
    List<JsonNode> items = items();
    assertThat(items).isNotEmpty();
    assertThat(items)
        .allSatisfy(
            item -> {
              assertThat(item.get("companyName").asText()).isEqualTo(company);
              assertThat(item.get("planName").asText()).isEqualTo(planName);
              assertThat(item.get("status").asText()).isNotBlank();
            });
  }

  @Cuando("el dueño consulta los pagos de la empresa {string}")
  public void theOwnerListsPayments(String company) throws Exception {
    context.record(
        mockMvc.perform(
            asOwner(get("/api/v1/platform/payments").param("tenantId", company(company).toString()))));
  }

  @Y("el pago muestra la empresa {string}, el importe {string} y su referencia")
  public void thePaymentShows(String company, String amount) throws Exception {
    JsonNode payment = items().get(0);
    assertThat(payment.get("companyName").asText()).isEqualTo(company);
    assertThat(payment.get("amount").decimalValue()).isEqualByComparingTo(amount);
    assertThat(payment.get("reference").asText()).isNotBlank();
  }

  @Cuando("el administrador de una empresa intenta abrir el resumen comercial")
  public void aCompanyTriesToOpenTheOverview() throws Exception {
    context.record(
        mockMvc.perform(
            get("/api/v1/platform/billing/overview")
                .header("Authorization", "Bearer " + companyToken())));
  }

  @Entonces("la respuesta no es exitosa")
  public void theResponseIsRejected() {
    assertThat(context.statusCode()).isBetween(400, 499);
  }

  private void changeAvailability(String name, boolean active, String reason) throws Exception {
    String body = json(Map.of("active", active, "reason", reason));
    context.record(
        mockMvc.perform(
            asOwner(patch("/api/v1/platform/plans/" + plan(name).getPlanId() + "/availability"))
                .content(body)));
  }

  private List<String> companyCatalog() throws Exception {
    JsonNode catalog =
        objectMapper.readTree(
            mockMvc
                .perform(get("/api/v1/plans").header("Authorization", "Bearer " + companyToken()))
                .andReturn()
                .getResponse()
                .getContentAsString());
    return StreamSupport.stream(catalog.spliterator(), false)
        .map(plan -> plan.get("name").asText())
        .toList();
  }

  private String companyToken() throws Exception {
    if (companyToken == null) {
      var iam = new IamApi(mockMvc, objectMapper, capturedEvents);
      iam.registerAndVerifyTenant("20987654321", COMPANY_EMAIL, ScenarioContext.PASSWORD);
      companyToken = iam.signInForToken(COMPANY_EMAIL, ScenarioContext.PASSWORD);
    }
    return companyToken;
  }

  /** Las empresas del panel se identifican por un id y el directorio de IAM devuelve su nombre. */
  private UUID company(String name) {
    UUID id = companies.computeIfAbsent(name, key -> UUID.randomUUID());
    Map<UUID, String> names = new HashMap<>();
    companies.forEach((companyName, companyId) -> names.put(companyId, companyName));
    when(companyDirectory.names(anyCollection())).thenReturn(names);
    return id;
  }

  private Plan planNamed(String name, String price) {
    return planRepository.findAll().stream()
        .filter(plan -> name.equals(plan.getName()))
        .findFirst()
        .orElseGet(
            () -> planRepository.saveAndFlush(new Plan(name, new BigDecimal(price), 15, true)));
  }

  private Plan plan(String name) {
    return planRepository.findAll().stream()
        .filter(plan -> name.equals(plan.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No existe el plan " + name));
  }

  private List<JsonNode> items() throws Exception {
    return StreamSupport.stream(context.body().get("items").spliterator(), false).toList();
  }

  private MockHttpServletRequestBuilder asOwner(MockHttpServletRequestBuilder request) {
    return request
        .header("Authorization", "Bearer " + ownerToken)
        .contentType(MediaType.APPLICATION_JSON);
  }

  private String json(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }
}
