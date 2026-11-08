package com.nexus.rinde.subscription.platform;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nexus.rinde.shared.infrastructure.security.JwtClaims;
import com.nexus.rinde.subscription.domain.model.aggregates.*;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.*;
import com.nexus.rinde.subscription.platform.infrastructure.PlatformCompanyDirectory;
import com.nexus.rinde.support.AbstractIntegrationTest;
import io.jsonwebtoken.Jwts;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PlatformBillingIntegrationTest extends AbstractIntegrationTest {
  private static final String KEY = "separate-platform-billing-integration-secret-12345";
  @Autowired PlanRepository plans;
  @Autowired SubscriptionRepository subscriptions;
  @Autowired PaymentRepository payments;
  @MockitoBean PlatformCompanyDirectory companies;

  @DynamicPropertySource
  static void platformProperties(DynamicPropertyRegistry registry) {
    registry.add("rinde.platform.jwt.secret", () -> KEY);
  }

  private String owner() {
    return Jwts.builder()
        .subject(UUID.randomUUID().toString())
        .issuer("rinde-platform")
        .audience()
        .add("rinde-platform-api")
        .and()
        .claim("scope", "PLATFORM")
        .claim("role", "PLATFORM_OWNER")
        .claim("email", "owner@example.invalid")
        .expiration(Date.from(Instant.now().plusSeconds(300)))
        .signWith(JwtClaims.signingKey(KEY))
        .compact();
  }

  @Test
  void createsAndWithdrawsAnImmutablePlanWithAudit() throws Exception {
    String token = owner();
    String body =
        mockMvc
            .perform(
                post("/api/v1/platform/plans")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Plan"
                            + " inicial\",\"monthlyPrice\":99.90,\"unitLimit\":10,\"reason\":\"Catálogo"
                            + " inicial\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(body).get("planId").asText());
    subscriptions.saveAndFlush(Subscription.pendingPayment(UUID.randomUUID(), id));
    mockMvc
        .perform(
            patch("/api/v1/platform/plans/" + id + "/availability")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false,\"reason\":\"Nueva versión comercial\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
    assertThat(plans.findById(id).orElseThrow().getMonthlyPrice()).isEqualByComparingTo("99.90");
    mockMvc
        .perform(get("/api/v1/platform/audit/billing").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2));
  }

  @Test
  void listsRealSubscriptionsAndPaymentsUsingTheCompanyDirectory() throws Exception {
    UUID tenant = UUID.randomUUID();
    Plan plan = plans.saveAndFlush(new Plan("Plan de prueba", new BigDecimal("80.00"), 5, true));
    Subscription subscription =
        subscriptions.saveAndFlush(Subscription.pendingPayment(tenant, plan.getPlanId()));
    Payment payment =
        Payment.pending(
            tenant,
            subscription.getSubscriptionId(),
            "fixture-payment",
            new BigDecimal("80.00"),
            Instant.now());
    payment.confirm("fixture-confirmation");
    payments.saveAndFlush(payment);
    when(companies.names(anyCollection())).thenReturn(Map.of(tenant, "Transportes de prueba"));
    String token = owner();
    mockMvc
        .perform(get("/api/v1/platform/subscriptions").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].companyName").value("Transportes de prueba"));
    mockMvc
        .perform(get("/api/v1/platform/payments").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].amount").value(80));
    mockMvc
        .perform(
            get("/api/v1/platform/billing/overview").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.confirmedAmount").value(80))
        .andExpect(jsonPath("$.paymentMode").value("SIMULATED"));
  }

  @Test
  void companyCannotManagePlansAndOwnerCannotUseCompanyApis() throws Exception {
    api.registerAndVerifyTenant("company@example.invalid");
    String company = api.accessTokenFor("company@example.invalid");
    mockMvc
        .perform(get("/api/v1/platform/plans").header("Authorization", "Bearer " + company))
        .andExpect(status().is4xxClientError());
    mockMvc
        .perform(get("/api/v1/plans").header("Authorization", "Bearer " + owner()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsUnreasonedOrInvalidPlanChanges() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/platform/plans")
                .header("Authorization", "Bearer " + owner())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Plan\",\"monthlyPrice\":0,\"unitLimit\":0,\"reason\":\" \"}"))
        .andExpect(status().isBadRequest());
    assertThat(plans.count()).isZero();
  }
}
