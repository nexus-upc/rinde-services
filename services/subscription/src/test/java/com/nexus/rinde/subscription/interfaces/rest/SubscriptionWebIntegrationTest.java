package com.nexus.rinde.subscription.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.nexus.rinde.subscription.domain.model.aggregates.Plan;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.nexus.rinde.support.AbstractIntegrationTest;
import com.nexus.rinde.support.SubscriptionApi;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

/** Cubre el flujo web y el aislamiento del pago simulado sin entregar secretos al cliente. */
@SpringBootTest(properties = "rinde.billing.simulation.enabled=true")
class SubscriptionWebIntegrationTest extends AbstractIntegrationTest {
  @Autowired private PlanRepository plans;

  private String administrator(String ruc, String email) throws Exception {
    api.registerAndVerifyTenant(email);
    return api.accessTokenFor(email);
  }

  private String[] checkout(String token) throws Exception {
    Plan plan = plans.saveAndFlush(new Plan("Plan web", new BigDecimal("99.00"), 10, true));
    SubscriptionApi subscriptions = new SubscriptionApi(mockMvc, objectMapper);
    var response = subscriptions.choosePlan(token, plan.getPlanId().toString())
        .andExpect(status().isCreated()).andReturn().getResponse();
    String id = objectMapper.readTree(response.getContentAsString()).get("subscriptionId").asText();
    var payment = subscriptions.startCheckout(token, id).andExpect(status().isCreated())
        .andReturn().getResponse();
    var json = objectMapper.readTree(payment.getContentAsString());
    assertThat(json.get("confirmationEnabled").asBoolean()).isTrue();
    return new String[] {id, json.get("paymentId").asText()};
  }

  @Test
  void currentSubscriptionIsEmptyForANewTenant() throws Exception {
    String token = administrator("20123456789", "ana@andes.pe");
    mockMvc.perform(get("/api/v1/subscriptions/current").header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
  }

  @Test
  void currentSubscriptionIsRestoredWithoutABrowserStoredId() throws Exception {
    String token = administrator("20123456789", "ana@andes.pe");
    String[] checkout = checkout(token);
    var response = mockMvc.perform(get("/api/v1/subscriptions/current")
        .header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn().getResponse();
    assertThat(objectMapper.readTree(response.getContentAsString()).get("subscriptionId").asText())
        .isEqualTo(checkout[0]);
  }

  @Test
  void anotherTenantCannotDiscoverOrConfirmThePayment() throws Exception {
    String token = administrator("20123456789", "ana@andes.pe");
    String[] checkout = checkout(token);
    String other = administrator("20987654321", "otra@andes.pe");
    mockMvc.perform(get("/api/v1/subscriptions/current").header("Authorization", "Bearer " + other))
        .andExpect(status().isNoContent());
    simulate(checkout, other, "CONFIRMED").andExpect(status().isNotFound());
  }

  @Test
  void administratorConfirmsAPaymentIdempotently() throws Exception {
    String token = administrator("20123456789", "ana@andes.pe");
    String[] checkout = checkout(token);
    String first = simulate(checkout, token, "CONFIRMED").andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    String repeated = simulate(checkout, token, "CONFIRMED").andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    var json = objectMapper.readTree(first);
    assertThat(json.get("status").asText()).isEqualTo("ACTIVE");
    assertThat(objectMapper.readTree(repeated).get("expiresAt")).isEqualTo(json.get("expiresAt"));
  }

  @Test
  void rejectedPaymentDoesNotActivateTheSubscription() throws Exception {
    String token = administrator("20123456789", "ana@andes.pe");
    String[] checkout = checkout(token);
    var json = objectMapper.readTree(simulate(checkout, token, "REJECTED")
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    assertThat(json.get("status").asText()).isEqualTo("PENDING_PAYMENT");
    assertThat(json.get("latestPaymentStatus").asText()).isEqualTo("REJECTED");
  }

  @Test
  void aDriverCannotSimulatePayment() throws Exception {
    mockMvc.perform(post("/api/v1/subscriptions/{id}/payments/{payment}/simulation",
        UUID.randomUUID(), UUID.randomUUID()).with(user("conductor").roles("DRIVER"))
        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
        .andExpect(status().isForbidden());
  }

  private org.springframework.test.web.servlet.ResultActions simulate(String[] ids, String token,
      String result) throws Exception {
    return mockMvc.perform(post("/api/v1/subscriptions/{id}/payments/{payment}/simulation", ids[0], ids[1])
        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"" + result + "\"}"));
  }
}
