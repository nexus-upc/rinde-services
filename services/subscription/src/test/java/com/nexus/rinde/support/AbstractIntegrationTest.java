package com.nexus.rinde.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base de las pruebas de integración: contexto completo contra rinde_subscription_test, con datos
 * limpios. IAM y Fleet responden desde WireMock; RabbitMQ es un broker real, igual que en core.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

  /** Clave HMAC de prueba para el webhook simulado; no es la de ningún entorno. */
  public static final String WEBHOOK_SECRET = "webhook-simulacion-de-prueba-0123456789";

  private static final String MESSAGING_PREFIX = "test.subscription." + UUID.randomUUID();

  @Autowired protected TestDatabase testDatabase;
  @Autowired protected CapturedEvents capturedEvents;
  @Autowired protected IamStub iamStub;
  @Autowired protected FleetStub fleetStub;
  @Autowired protected TenantAccounts tenantAccounts;
  @Autowired protected TestTokens tokens;
  @Autowired protected MockMvc mockMvc;
  @Autowired protected ObjectMapper objectMapper;

  protected TenantAccess api;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    TestDatabase.registerUrl(registry);
    registry.add("rinde.iam.base-url", WireMockServices::iamBaseUrl);
    registry.add("rinde.fleet.base-url", WireMockServices::fleetBaseUrl);
    registry.add("rinde.billing.simulation.webhook-secret", () -> WEBHOOK_SECRET);
    registry.add("rinde.messaging.exchange", () -> MESSAGING_PREFIX + ".events");
    registry.add("rinde.messaging.subscription.queue", () -> MESSAGING_PREFIX + ".subscriptions");
    registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> false);
  }

  @BeforeEach
  void cleanData() {
    testDatabase.clean();
    capturedEvents.clear();
    tenantAccounts.reset();
    iamStub.reset();
    fleetStub.reset();
    api = new TenantAccess(tenantAccounts, tokens);
  }
}
