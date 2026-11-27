package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;
import com.nexus.rinde.subscription.infrastructure.services.PaymentWebhookSignature;
import com.nexus.rinde.subscription.interfaces.rest.resources.PaymentWebhookResource;
import com.nexus.rinde.support.SubscriptionApi;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Y;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** Pasos de NF05 (pago procesado una sola vez) y NF06 (el pago no guarda datos de tarjeta). */
public class PaymentIdempotencySteps {

  private final ScenarioContext context;
  private final JdbcTemplate jdbcTemplate;
  private final PaymentWebhookSignature webhookSignature;
  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  private final SubscriptionApi subscriptionApi;
  private MvcResult firstDelivery;
  private String providerEventId;
  private String providerReference;
  private String cardNumber;
  private String cvv;

  public PaymentIdempotencySteps(
      ScenarioContext context,
      JdbcTemplate jdbcTemplate,
      PaymentWebhookSignature webhookSignature,
      MockMvc mockMvc,
      ObjectMapper objectMapper) {
    this.context = context;
    this.jdbcTemplate = jdbcTemplate;
    this.webhookSignature = webhookSignature;
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.subscriptionApi = new SubscriptionApi(mockMvc, objectMapper);
  }

  @Cuando("la pasarela envía dos veces el mismo webhook de pago confirmado")
  public void theGatewaySendsSameConfirmedWebhookTwice() throws Exception {
    PaymentWebhookResource webhook =
        confirmedWebhookForPendingPayment(UUID.randomUUID().toString());
    providerEventId = webhook.providerEventId();
    String signature = webhookSignature.signHex(webhook);
    firstDelivery = subscriptionApi.paymentWebhook(webhook, signature).andReturn();
    context.record(subscriptionApi.paymentWebhook(webhook, signature));
  }

  @Y("la primera entrega confirma el pago sin marcarlo como duplicado")
  public void firstDeliveryConfirmsWithoutDuplicateFlag() throws Exception {
    assertThat(firstDelivery.getResponse().getStatus()).isEqualTo(200);
    JsonNode result = objectMapper.readTree(firstDelivery.getResponse().getContentAsString());
    assertThat(result.get("duplicate").asBoolean()).isFalse();
    assertThat(result.get("paymentStatus").asText()).isEqualTo("CONFIRMED");
  }

  @Y("la segunda entrega se reconoce como duplicada")
  public void secondDeliveryIsRecognizedAsDuplicate() throws Exception {
    JsonNode result = context.body();
    assertThat(result.get("duplicate").asBoolean()).isTrue();
    assertThat(result.get("subscriptionStatus").asText()).isEqualTo("ACTIVE");
  }

  @Y("la tabla de pagos tiene exactamente un registro para ese evento del proveedor")
  public void paymentsTableHasOneRowForProviderEvent() {
    assertThat(countPaymentsWithEvent(providerEventId)).isEqualTo(1L);
  }

  @Y("queda un único pago confirmado para el evento del proveedor del último webhook")
  public void oneConfirmedPaymentRemainsForLatestWebhook() {
    List<String> events =
        jdbcTemplate.queryForList(
            "select provider_event_id from subscription.payments"
                + " where subscription_id = ? and status = 'CONFIRMED'",
            String.class,
            UUID.fromString(context.latestSubscriptionId()));
    assertThat(events).hasSize(1);
    assertThat(countPaymentsWithEvent(events.get(0))).isEqualTo(1L);
  }

  @Cuando("la pasarela envía el pago confirmado con datos de tarjeta {string} y cvv {string}")
  public void theGatewaySendsConfirmedPaymentWithCardData(String cardNumber, String cvv)
      throws Exception {
    this.cardNumber = cardNumber;
    this.cvv = cvv;
    PaymentWebhookResource webhook =
        confirmedWebhookForPendingPayment(UUID.randomUUID().toString());
    providerReference = webhook.providerReference();
    // Los campos de tarjeta no forman parte de la firma canónica ni del contrato del webhook.
    ObjectNode body = objectMapper.valueToTree(webhook);
    body.put("cardNumber", cardNumber);
    body.put("cvv", cvv);
    context.record(postRawWebhook(body, webhookSignature.signHex(webhook)));
  }

  @Y("el pago guardado solo contiene la referencia del proveedor, el monto y las fechas")
  public void storedPaymentKeepsOnlyReferenceAmountAndDates() {
    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select provider_reference, amount, created_at from subscription.payments"
                + " where status = 'CONFIRMED' and provider_reference = ?",
            providerReference);
    assertThat(row.get("provider_reference")).isEqualTo(providerReference);
    assertThat(row.get("amount")).isInstanceOf(BigDecimal.class);
    assertThat(row.get("created_at")).isNotNull();
  }

  @Y("ninguna columna de la tabla de pagos contiene el número de tarjeta ni el cvv")
  public void noPaymentColumnHoldsCardData() {
    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select * from subscription.payments where status = 'CONFIRMED' and provider_reference = ?",
            providerReference);
    for (Map.Entry<String, Object> column : row.entrySet()) {
      String value = String.valueOf(column.getValue());
      assertThat(value)
          .as("La columna %s no debe contener el número de tarjeta", column.getKey())
          .doesNotContain(cardNumber);
      assertThat(value)
          .as("La columna %s no debe contener el cvv", column.getKey())
          .isNotEqualTo(cvv);
    }
  }

  /** Webhook confirmado para el intento pendiente de la última suscripción creada. */
  private PaymentWebhookResource confirmedWebhookForPendingPayment(String eventId) {
    PendingPayment pending =
        jdbcTemplate.queryForObject(
            "select provider_reference, amount from subscription.payments"
                + " where subscription_id = ? and status = 'PENDING'",
            (rs, rowNum) ->
                new PendingPayment(
                    rs.getString("provider_reference"),
                    rs.getBigDecimal("amount").setScale(2).toPlainString()),
            UUID.fromString(context.latestSubscriptionId()));
    return new PaymentWebhookResource(
        eventId, pending.providerReference(), PaymentStatus.CONFIRMED, pending.amount(), null);
  }

  private ResultActions postRawWebhook(JsonNode body, String signature) throws Exception {
    return mockMvc.perform(
        post("/api/v1/billing/webhooks/payment")
            .header("X-RINDE-Signature", signature)
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(objectMapper.writeValueAsString(body)));
  }

  private long countPaymentsWithEvent(String eventId) {
    Long count =
        jdbcTemplate.queryForObject(
            "select count(*) from subscription.payments where provider_event_id = ?",
            Long.class,
            eventId);
    return count == null ? 0L : count;
  }

  private record PendingPayment(String providerReference, String amount) {}
}
