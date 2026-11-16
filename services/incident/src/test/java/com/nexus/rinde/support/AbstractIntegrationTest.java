package com.nexus.rinde.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Base de las pruebas de integración: contexto completo contra rinde_incident_test con datos limpios. */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

  @Autowired protected TestDatabase testDatabase;
  @Autowired protected CapturedEvents capturedEvents;
  @Autowired protected IamStub iamStub;
  @Autowired protected TestTokens tokens;
  @Autowired protected MockMvc mockMvc;
  @Autowired protected ObjectMapper objectMapper;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    TestDatabase.registerUrl(registry);
    registry.add("rinde.iam.service-url", WireMockServices::baseUrl);
    registry.add("rinde.trip.service-url", WireMockServices::baseUrl);
    registry.add("rinde.fleet.service-url", WireMockServices::baseUrl);
    registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> false);
  }

  @BeforeEach
  void cleanData() {
    testDatabase.clean();
    capturedEvents.clear();
    iamStub.reset();
  }
}
