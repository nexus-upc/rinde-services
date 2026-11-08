package com.nexus.rinde.acceptance;

import com.nexus.rinde.support.AbstractIntegrationTest;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Levanta el contexto de Spring para Cucumber. Extiende la base de integración para compartir la
 * misma configuración (y el mismo contexto en caché) y apuntar a la base rinde_iam_test.
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
public class CucumberSpringConfiguration extends AbstractIntegrationTest {
  @DynamicPropertySource
  static void platformProperties(DynamicPropertyRegistry registry) {
    registry.add("rinde.platform.jwt.secret", () -> "platform-cucumber-test-secret-at-least-32-chars");
    registry.add("rinde.platform.bootstrap.enabled", () -> false);
  }
}
