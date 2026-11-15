package com.nexus.rinde.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.subscription.interfaces.rest.resources.PaymentWebhookResource;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Cliente de pruebas para el catálogo de planes y las suscripciones. */
public class SubscriptionApi {

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;

  public SubscriptionApi(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  public ResultActions listPlans(String token) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/plans"), token));
  }

  public ResultActions choosePlan(String token, String planId) throws Exception {
    return mockMvc.perform(
        authorized(post("/api/v1/subscriptions"), token)
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(objectMapper.writeValueAsString(Map.of("planId", planId))));
  }

  public ResultActions getSubscription(String token, String subscriptionId) throws Exception {
    return mockMvc.perform(
        authorized(get("/api/v1/subscriptions/" + subscriptionId), token));
  }

  public ResultActions startCheckout(String token, String subscriptionId) throws Exception {
    return mockMvc.perform(
        authorized(post("/api/v1/subscriptions/" + subscriptionId + "/checkout"), token));
  }

  public ResultActions paymentWebhook(PaymentWebhookResource resource, String signature)
      throws Exception {
    return mockMvc.perform(
        post("/api/v1/billing/webhooks/payment")
            .header("X-RINDE-Signature", signature)
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(objectMapper.writeValueAsString(resource)));
  }

  private MockHttpServletRequestBuilder authorized(
      MockHttpServletRequestBuilder request, String token) {
    return token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }
}
