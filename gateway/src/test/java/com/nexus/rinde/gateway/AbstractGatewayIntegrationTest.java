package com.nexus.rinde.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Base de las pruebas del gateway. Los servicios y el monolito son servidores WireMock; el secreto
 * JWT es de prueba y no es el de ningún entorno.
 */
abstract class AbstractGatewayIntegrationTest {

  static final String TEST_SECRET = "gateway-test-secret-with-at-least-32-chars";
  static final String OTHER_SECRET = "another-test-secret-with-at-least-32-chars";
  static final String ALLOWED_ORIGIN = "http://localhost:4200";
  static final String LANDING_ORIGIN = "http://localhost:4300";

  static final WireMockServer IAM = new WireMockServer(wireMockConfig().dynamicPort());
  static final WireMockServer MONOLITH = new WireMockServer(wireMockConfig().dynamicPort());
  static final WireMockServer TRIP = new WireMockServer(wireMockConfig().dynamicPort());
  static final WireMockServer FLEET = new WireMockServer(wireMockConfig().dynamicPort());
  static final WireMockServer EXPENSE = new WireMockServer(wireMockConfig().dynamicPort());
  static final WireMockServer INCIDENT = new WireMockServer(wireMockConfig().dynamicPort());

  static {
    IAM.start();
    MONOLITH.start();
    TRIP.start();
    FLEET.start();
    EXPENSE.start();
    INCIDENT.start();
  }

  @Autowired WebTestClient webTestClient;

  @DynamicPropertySource
  static void gatewayProperties(DynamicPropertyRegistry registry) {
    registry.add("IAM_SERVICE_URL", () -> "http://localhost:" + IAM.port());
    registry.add("MONOLITH_URL", () -> "http://localhost:" + MONOLITH.port());
    registry.add("TRIP_SERVICE_URL", () -> "http://localhost:" + TRIP.port());
    registry.add("FLEET_SERVICE_URL", () -> "http://localhost:" + FLEET.port());
    registry.add("EXPENSE_SERVICE_URL", () -> "http://localhost:" + EXPENSE.port());
    registry.add("INCIDENT_SERVICE_URL", () -> "http://localhost:" + INCIDENT.port());
    registry.add("JWT_SECRET", () -> TEST_SECRET);
    registry.add("CORS_ALLOWED_ORIGINS", () -> ALLOWED_ORIGIN + "," + LANDING_ORIGIN);
  }

  @BeforeEach
  void resetStubs() {
    IAM.resetAll();
    MONOLITH.resetAll();
    TRIP.resetAll();
    FLEET.resetAll();
    EXPENSE.resetAll();
    INCIDENT.resetAll();
  }

  static String validToken() {
    return token(TEST_SECRET, Instant.now().plus(Duration.ofHours(1)));
  }

  static String token(String secret, Instant expiresAt) {
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .subject(UUID.randomUUID().toString())
        .claim("tenantId", UUID.randomUUID().toString())
        .claim("role", "ADMINISTRADOR")
        .claim("tenantStatus", "ACTIVE")
        .claim("email", "admin@rinde.test")
        .expiration(Date.from(expiresAt))
        .signWith(key)
        .compact();
  }
}
