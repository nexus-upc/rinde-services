package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.subscription.domain.model.aggregates.Plan;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;
import com.nexus.rinde.subscription.application.internal.commandservices.SubscriptionLifecycleService;
import com.nexus.rinde.subscription.infrastructure.services.PaymentWebhookSignature;
import com.nexus.rinde.subscription.interfaces.rest.resources.PaymentWebhookResource;
import com.nexus.rinde.subscription.interfaces.acl.SubscriptionSuspended;
import com.nexus.rinde.support.CapturedEvents;
import com.nexus.rinde.support.ContextDoubles;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import com.nexus.rinde.support.RelativeDates;
import com.nexus.rinde.support.SubscriptionApi;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Pasos de aceptación para selección de planes y consulta de suscripciones. */
public class SubscriptionSteps {

  private static final String TENANT_RUC = "20123456789";

  private final ScenarioContext context;
  private final PlanRepository planRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final JdbcTemplate jdbcTemplate;
  private final SubscriptionApi subscriptionApi;
  private final ContextDoubles doubles;
  private final PaymentWebhookSignature webhookSignature;
  private final SubscriptionLifecycleService lifecycleService;
  private final Clock clock;
  private final CapturedEvents capturedEvents;
  private String latestProviderReference;
  private String latestPaymentAmount;
  private PaymentWebhookResource latestWebhook;
  private Instant confirmedExpiresAt;

  public SubscriptionSteps(
      ScenarioContext context,
      PlanRepository planRepository,
      SubscriptionRepository subscriptionRepository,
      JdbcTemplate jdbcTemplate,
      PaymentWebhookSignature webhookSignature,
      SubscriptionLifecycleService lifecycleService,
      Clock clock,
      CapturedEvents capturedEvents,
      ContextDoubles doubles,
      MockMvc mockMvc,
      ObjectMapper objectMapper) {
    this.context = context;
    this.planRepository = planRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.subscriptionApi = new SubscriptionApi(mockMvc, objectMapper);
    this.doubles = doubles;
    this.webhookSignature = webhookSignature;
    this.lifecycleService = lifecycleService;
    this.clock = clock;
    this.capturedEvents = capturedEvents;
  }

  @Dado("que existe el plan activo de prueba {string} con precio mensual {string} y límite {int}")
  public void anActiveTestPlanExists(String name, String monthlyPrice, int unitLimit) {
    Plan plan =
        planRepository.saveAndFlush(
            new Plan(name, new BigDecimal(monthlyPrice), unitLimit, true));
    context.rememberPlanId(name, plan.getPlanId().toString());
  }

  /** La flota de la empresa la responde el doble de Fleet que consulta core para validar el plan. */
  @Dado("que la empresa tiene {int} vehículos registrados")
  public void theCompanyHasRegisteredVehicles(int count) {
    UUID tenantId = UUID.fromString(context.tenantIdOf(TENANT_RUC));
    for (int i = 1; i <= count; i++) {
      doubles.addVehicle(
          tenantId, UUID.randomUUID(), "T" + String.format("%07d", i), "Volvo", "FH540");
    }
  }

  @Cuando("el administrador consulta los planes disponibles")
  public void theAdministratorListsPlans() throws Exception {
    context.record(subscriptionApi.listPlans(context.currentAdministratorToken()));
  }

  @Cuando("el administrador selecciona el plan {string}")
  public void theAdministratorSelectsPlan(String name) throws Exception {
    selectPlan(name);
  }

  @Cuando("el administrador intenta seleccionar el plan {string}")
  public void theAdministratorAttemptsToSelectPlan(String name) throws Exception {
    selectPlan(name);
  }

  private void selectPlan(String name) throws Exception {
    context.record(
        subscriptionApi.choosePlan(
            context.currentAdministratorToken(), context.planIdOf(name)));
    if (context.statusCode() == 201) {
      String subscriptionId = context.body().get("subscriptionId").asText();
      context.rememberSubscriptionId("creada", subscriptionId);
      context.rememberLatestSubscriptionId(subscriptionId);
    }
  }

  @Cuando("el conductor {string} intenta seleccionar el plan {string}")
  public void aDriverTriesToChoosePlan(String email, String name) throws Exception {
    context.record(
        subscriptionApi.choosePlan(context.accessTokenOf(email), context.planIdOf(name)));
  }

  @Cuando("una persona sin token consulta los planes disponibles")
  public void anUnauthenticatedPersonListsPlans() throws Exception {
    context.record(subscriptionApi.listPlans(null));
  }

  @Y("el catálogo incluye el plan {string} con precio {string} y límite {int}")
  public void theCatalogIncludesPlan(String name, String price, int unitLimit) throws Exception {
    JsonNode plan =
        arrayElements(context.body()).stream()
            .filter(item -> name.equals(item.get("name").asText()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No se encontró el plan " + name));
    assertThat(plan.get("monthlyPrice").decimalValue())
        .isEqualByComparingTo(new BigDecimal(price));
    assertThat(plan.get("unitLimit").asInt()).isEqualTo(unitLimit);
  }

  @Y("la suscripción creada tiene estado {string}")
  public void createdSubscriptionHasStatus(String status) throws Exception {
    assertThat(context.body().get("status").asText()).isEqualTo(status);
  }

  @Y("la suscripción creada no tiene vigencia pagada")
  public void createdSubscriptionHasNoPaidPeriod() throws Exception {
    assertThat(context.body().get("startsAt").isNull()).isTrue();
    assertThat(context.body().get("expiresAt").isNull()).isTrue();
  }

  @Cuando("el administrador consulta la suscripción creada")
  public void theAdministratorGetsCreatedSubscription() throws Exception {
    context.record(
        subscriptionApi.getSubscription(
            context.currentAdministratorToken(), context.subscriptionIdOf("creada")));
  }

  @Dado("que el administrador crea una suscripción pendiente con el plan {string}")
  public void theAdministratorCreatesPendingSubscription(String name) throws Exception {
    theAdministratorSelectsPlan(name);
    assertThat(context.statusCode()).isEqualTo(201);
  }

  @Y("la suscripción consultada tiene el estado {string}")
  public void queriedSubscriptionHasStatus(String status) throws Exception {
    assertThat(context.body().get("status").asText()).isEqualTo(status);
  }

  @Cuando("un administrador de otra empresa consulta la suscripción creada")
  public void anotherCompanyAdministratorGetsSubscription() throws Exception {
    context.record(
        subscriptionApi.getSubscription(
            context.currentAdministratorToken(), context.subscriptionIdOf("creada")));
  }

  @Y("no se crea una suscripción para la empresa")
  public void noSubscriptionIsCreatedForTenant() {
    UUID tenantId = UUID.fromString(context.tenantIdOf(TENANT_RUC));
    assertThat(subscriptionRepository.countByTenantId(tenantId)).isZero();
  }

  @Dado("que existe una suscripción activa de prueba con vencimiento {string}")
  public void anActiveSubscriptionFixtureExists(String expiration) {
    // Este dato prepara una consulta de estado; no representa un pago ni sustituye US39.
    Plan plan =
        planRepository.saveAndFlush(
            new Plan("Plan-Consulta-Prueba", new BigDecimal("37.50"), 5, true));
    UUID subscriptionId = UUID.randomUUID();
    UUID tenantId = UUID.fromString(context.tenantIdOf(TENANT_RUC));
    Instant expiresAt = RelativeDates.instant(expiration);
    Instant startsAt = expiresAt.minus(30, ChronoUnit.DAYS);
    LocalDateTime expiresAtUtc = LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC);
    LocalDateTime startsAtUtc = LocalDateTime.ofInstant(startsAt, ZoneOffset.UTC);
    jdbcTemplate.update(
        "insert into subscription.subscriptions"
            + " (subscription_id, tenant_id, plan_id, status, starts_at, expires_at)"
            + " values (?, ?, ?, 'ACTIVE', ?, ?)",
        subscriptionId,
        tenantId,
        plan.getPlanId(),
        startsAtUtc,
        expiresAtUtc);
    context.rememberLatestSubscriptionId(subscriptionId.toString());
  }

  @Cuando("el administrador consulta la suscripción de la empresa")
  public void theAdministratorGetsTenantSubscription() throws Exception {
    context.record(
        subscriptionApi.getSubscription(
            context.currentAdministratorToken(), context.latestSubscriptionId()));
  }

  @Cuando("una persona sin token consulta la suscripción de la empresa")
  public void anUnauthenticatedPersonGetsTenantSubscription() throws Exception {
    context.record(subscriptionApi.getSubscription(null, context.latestSubscriptionId()));
  }

  @Y("la suscripción consultada vence en {string}")
  public void queriedSubscriptionExpiresAt(String expiration) throws Exception {
    assertThat(Instant.parse(context.body().get("expiresAt").asText()))
        .isEqualTo(RelativeDates.instant(expiration));
  }

  @Cuando("el administrador inicia el checkout simulado de la suscripción creada")
  public void theAdministratorStartsSimulatedCheckout() throws Exception {
    context.record(
        subscriptionApi.startCheckout(
            context.currentAdministratorToken(), context.subscriptionIdOf("creada")));
    if (context.statusCode() == 201 || context.statusCode() == 200) {
      JsonNode checkout = context.body();
      latestProviderReference = checkout.get("providerReference").asText();
      latestPaymentAmount = checkout.get("amount").decimalValue().setScale(2).toPlainString();
    }
  }

  @Y("el checkout es simulado y no expone datos de tarjeta")
  public void checkoutIsSimulatedWithoutCardData() throws Exception {
    JsonNode body = context.body();
    assertThat(body.get("simulated").asBoolean()).isTrue();
    assertThat(body.get("status").asText()).isEqualTo("PENDING");
    assertThat(body.has("cardNumber")).isFalse();
    assertThat(body.has("cvv")).isFalse();
  }

  @Cuando("la pasarela confirma el último pago simulado")
  public void theGatewayConfirmsLatestPayment() throws Exception {
    sendPaymentWebhook(PaymentStatus.CONFIRMED, null, UUID.randomUUID().toString());
  }

  @Cuando("la pasarela rechaza el último pago simulado con motivo {string}")
  public void theGatewayRejectsLatestPayment(String reason) throws Exception {
    sendPaymentWebhook(PaymentStatus.REJECTED, reason, UUID.randomUUID().toString());
  }

  @Cuando("la pasarela repite el último evento de pago")
  public void theGatewayRepeatsLatestPaymentEvent() throws Exception {
    if (latestWebhook == null) {
      throw new AssertionError("No hay un webhook previo para repetir.");
    }
    context.record(
        subscriptionApi.paymentWebhook(
            latestWebhook, webhookSignature.signHex(latestWebhook)));
  }

  @Cuando("la pasarela envía un pago con una firma inválida")
  public void theGatewaySendsPaymentWithInvalidSignature() throws Exception {
    PaymentWebhookResource webhook =
        new PaymentWebhookResource(
            UUID.randomUUID().toString(),
            latestProviderReference,
            PaymentStatus.CONFIRMED,
            latestPaymentAmount,
            null);
    context.record(subscriptionApi.paymentWebhook(webhook, "00"));
  }

  @Cuando("el administrador reintenta el checkout simulado")
  public void theAdministratorRetriesSimulatedCheckout() throws Exception {
    String previousReference = latestProviderReference;
    theAdministratorStartsSimulatedCheckout();
    if (context.statusCode() == 201 || context.statusCode() == 200) {
      assertThat(latestProviderReference).isNotEqualTo(previousReference);
    }
  }

  @Y("la confirmación de pago activa la suscripción y asigna vigencia mensual")
  public void paymentConfirmationActivatesSubscription() throws Exception {
    JsonNode result = context.body();
    assertThat(result.get("paymentStatus").asText()).isEqualTo("CONFIRMED");
    assertThat(result.get("subscriptionStatus").asText()).isEqualTo("ACTIVE");
    assertThat(result.get("startsAt").isNull()).isFalse();
    assertThat(result.get("expiresAt").isNull()).isFalse();
    confirmedExpiresAt = Instant.parse(result.get("expiresAt").asText()).truncatedTo(ChronoUnit.MICROS);
    assertThat(confirmedExpiresAt).isAfter(Instant.parse(result.get("startsAt").asText()));
  }

  @Y("el webhook repetido se reconoce sin extender la vigencia")
  public void repeatedWebhookDoesNotExtendPaidPeriod() throws Exception {
    JsonNode result = context.body();
    assertThat(result.get("duplicate").asBoolean()).isTrue();
    assertThat(result.get("subscriptionStatus").asText()).isEqualTo("ACTIVE");
    long persistedPrecisionDifference =
        Math.abs(
            ChronoUnit.MICROS.between(
                confirmedExpiresAt, Instant.parse(result.get("expiresAt").asText())));
    assertThat(persistedPrecisionDifference).isLessThanOrEqualTo(1L);
  }

  @Y("el pago rechazado deja la suscripción pendiente e informa el motivo {string}")
  public void rejectedPaymentLeavesSubscriptionPending(String reason) throws Exception {
    JsonNode result = context.body();
    assertThat(result.get("paymentStatus").asText()).isEqualTo("REJECTED");
    assertThat(result.get("subscriptionStatus").asText()).isEqualTo("PENDING_PAYMENT");
    assertThat(result.get("failureReason").asText()).isEqualTo(reason);
  }

  @Y("la consulta de suscripción informa el último rechazo {string}")
  public void subscriptionQueryShowsLatestRejection(String reason) throws Exception {
    JsonNode result = context.body();
    assertThat(result.get("status").asText()).isEqualTo("PENDING_PAYMENT");
    assertThat(result.get("latestPaymentStatus").asText()).isEqualTo("REJECTED");
    assertThat(result.get("lastPaymentFailureReason").asText()).isEqualTo(reason);
  }

  @Y("la consulta de suscripción informa el estado {string} y ningún pago confirmado")
  public void subscriptionQueryShowsNoConfirmedPayment(String status) throws Exception {
    JsonNode result = context.body();
    assertThat(result.get("status").asText()).isEqualTo(status);
    assertThat(result.get("latestPaymentStatus").asText()).isEqualTo("PENDING");
    assertThat(result.get("startsAt").isNull()).isTrue();
    assertThat(result.get("expiresAt").isNull()).isTrue();
  }

  @Y("hay un único comprobante de pago simulado para el administrador")
  public void exactlyOneSimulatedPaymentReceiptExists() {
    UUID tenantId = UUID.fromString(context.tenantIdOf(TENANT_RUC));
    Long count =
        jdbcTemplate.queryForObject(
            "select count(*) from notification.email_outbox"
                + " where tenant_id = ? and subject = 'Comprobante de suscripción RINDE'"
                + " and recipient_email = 'ana@andes.pe' and delivery_status = 'SIMULATED_SENT'",
            Long.class,
            tenantId);
    assertThat(count).isEqualTo(1L);
  }

  @Dado("que la suscripción activa de la empresa vence en cinco días")
  public void theActiveSubscriptionExpiresInFiveDays() {
    createActiveSubscriptionFixture(
        clock.instant().plus(SubscriptionLifecycleService.REMINDER_WINDOW).minusSeconds(30));
  }

  @Cuando("el proceso diario de suscripciones se ejecuta dos veces")
  public void theDailySubscriptionLifecycleRunsTwice() {
    lifecycleService.advanceSubscriptions();
    lifecycleService.advanceSubscriptions();
  }

  @Y("la consulta muestra el aviso de renovación y el estado {string}")
  public void theSubscriptionShowsRenewalNotice(String status) throws Exception {
    assertThat(context.body().get("renewalNotice").asBoolean()).isTrue();
    assertThat(context.body().get("status").asText()).isEqualTo(status);
  }

  @Y("se registra una sola notificación de vencimiento para el administrador")
  public void oneSimulatedExpiryNotificationExists() {
    UUID tenantId = UUID.fromString(context.tenantIdOf(TENANT_RUC));
    Long count =
        jdbcTemplate.queryForObject(
            "select count(*) from notification.email_outbox"
                + " where tenant_id = ? and subject = 'Tu suscripción vence pronto'"
                + " and recipient_email = 'ana@andes.pe' and delivery_status = 'SIMULATED_SENT'",
            Long.class,
            tenantId);
    assertThat(count).isEqualTo(1L);
  }

  @Dado("que existe una suscripción activa vencida hace más de tres días")
  public void anActiveSubscriptionExpiredMoreThanThreeDaysAgo() {
    createActiveSubscriptionFixture(
        clock.instant().minus(SubscriptionLifecycleService.GRACE_PERIOD).minusSeconds(1));
  }

  @Cuando("el proceso diario de suscripciones avanza los estados")
  public void theDailySubscriptionLifecycleAdvancesStatuses() {
    lifecycleService.advanceSubscriptions();
  }

  /**
   * Core solo registra la suspensión y publica su evento; el estado de la empresa lo aplica IAM
   * al consumirlo, y esa comprobación vive en services/iam.
   */
  @Entonces("la suscripción queda suspendida y se publica el evento de suspensión")
  public void theSubscriptionIsSuspendedAndTheEventIsPublished() {
    assertThat(capturedEvents.last(SubscriptionSuspended.class)).isPresent();
    var subscription =
        subscriptionRepository
            .findById(UUID.fromString(context.latestSubscriptionId()))
            .orElseThrow();
    assertThat(subscription.getStatus().name()).isEqualTo("SUSPENDED");
  }

  private void sendPaymentWebhook(PaymentStatus status, String failureReason, String eventId)
      throws Exception {
    latestWebhook =
        new PaymentWebhookResource(
            eventId, latestProviderReference, status, latestPaymentAmount, failureReason);
    context.record(
        subscriptionApi.paymentWebhook(
            latestWebhook, webhookSignature.signHex(latestWebhook)));
  }

  private void createActiveSubscriptionFixture(Instant expiresAt) {
    Plan plan =
        planRepository.saveAndFlush(
            new Plan("Plan-Consulta-Prueba", new BigDecimal("37.50"), 5, true));
    UUID subscriptionId = UUID.randomUUID();
    UUID tenantId = UUID.fromString(context.tenantIdOf(TENANT_RUC));
    Instant startsAt = expiresAt.minus(30, ChronoUnit.DAYS);
    jdbcTemplate.update(
        "insert into subscription.subscriptions"
            + " (subscription_id, tenant_id, plan_id, status, starts_at, expires_at)"
            + " values (?, ?, ?, 'ACTIVE', ?, ?)",
        subscriptionId,
        tenantId,
        plan.getPlanId(),
        LocalDateTime.ofInstant(startsAt, ZoneOffset.UTC),
        LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
    context.rememberLatestSubscriptionId(subscriptionId.toString());
  }

  private java.util.List<JsonNode> arrayElements(JsonNode array) {
    java.util.List<JsonNode> values = new java.util.ArrayList<>();
    array.forEach(values::add);
    return values;
  }
}
