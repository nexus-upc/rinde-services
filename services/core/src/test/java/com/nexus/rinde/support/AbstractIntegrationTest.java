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

/** Base de las pruebas de integración: contexto completo contra rinde_test, con datos limpios. */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

  private static final String NOTIFICATION_QUEUE = "test.notification." + UUID.randomUUID();

  @Autowired protected TestDatabase testDatabase;
  @Autowired protected CapturedEvents capturedEvents;
  @Autowired protected ContextDoubles contextDoubles;
  @Autowired protected ExpenseDouble expenseDouble;
  @Autowired protected IncidentDouble incidentDouble;
  @Autowired protected TestTokens tokens;
  @Autowired protected MockMvc mockMvc;
  @Autowired protected ObjectMapper objectMapper;

  protected TenantAccess api;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    TestDatabase.registerUrl(registry);
    registry.add("rinde.iam.remote-enabled", () -> false);
    registry.add("rinde.trip.remote-enabled", () -> false);
    registry.add("rinde.fleet.remote-enabled", () -> false);
    registry.add("rinde.expense.remote-enabled", () -> false);
    registry.add("rinde.incident.remote-enabled", () -> false);
    registry.add("rinde.messaging.notification.queue", () -> NOTIFICATION_QUEUE);
    registry.add("rinde.messaging.exchange", () -> NOTIFICATION_QUEUE + ".events");
    registry.add("rinde.messaging.subscription.queue", () -> NOTIFICATION_QUEUE + ".subscriptions");
    registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> false);
  }

  @BeforeEach
  void cleanData() {
    testDatabase.clean();
    capturedEvents.clear();
    contextDoubles.reset();
    expenseDouble.reset();
    incidentDouble.reset();
    api = new TenantAccess(contextDoubles, tokens);
  }
}
