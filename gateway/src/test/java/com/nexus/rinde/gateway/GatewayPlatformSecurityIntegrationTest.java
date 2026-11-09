package com.nexus.rinde.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayPlatformSecurityIntegrationTest extends AbstractGatewayIntegrationTest {
  static final String PLATFORM_KEY = "separate-gateway-platform-secret-for-testing";

  @DynamicPropertySource
  static void platform(DynamicPropertyRegistry registry) {
    registry.add("RINDE_PLATFORM_JWT_SECRET", () -> PLATFORM_KEY);
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
        .signWith(Keys.hmacShaKeyFor(PLATFORM_KEY.getBytes(StandardCharsets.UTF_8)))
        .compact();
  }

  @Test
  void refusesTenantCredentialsAtThePlatformBoundary() {
    webTestClient
        .get()
        .uri("/api/v1/platform/companies")
        .headers(h -> h.setBearerAuth(validToken()))
        .exchange()
        .expectStatus()
        .isForbidden();
    IAM.verify(0, anyRequestedFor(urlMatching("/api/v1/platform/.*")));
  }

  @Test
  void exposesOnlyTheCapabilityPubliclyAndRoutesTheMailboxOnlyForTheOwner() {
    IAM.stubFor(get("/api/v1/demo/features").willReturn(okJson("{\"mailboxEnabled\":true}")));
    IAM.stubFor(
        get("/api/v1/platform/demo-mailbox/messages")
            .willReturn(okJson("{\"items\":[],\"total\":0}")));
    webTestClient
        .get()
        .uri("/api/v1/demo/features")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.mailboxEnabled")
        .isEqualTo(true);
    webTestClient
        .get()
        .uri("/api/v1/platform/demo-mailbox/messages")
        .exchange()
        .expectStatus()
        .isUnauthorized();
    webTestClient
        .get()
        .uri("/api/v1/platform/demo-mailbox/messages")
        .headers(h -> h.setBearerAuth(validToken()))
        .exchange()
        .expectStatus()
        .isForbidden();
    webTestClient
        .get()
        .uri("/api/v1/platform/demo-mailbox/messages")
        .headers(h -> h.setBearerAuth(owner()))
        .exchange()
        .expectStatus()
        .isOk();
    IAM.verify(1, getRequestedFor(urlEqualTo("/api/v1/platform/demo-mailbox/messages")));
  }

  @Test
  void refusesPlatformTokensAtTheTenantBoundary() {
    webTestClient
        .get()
        .uri("/api/v1/trips")
        .headers(h -> h.setBearerAuth(owner()))
        .exchange()
        .expectStatus()
        .isForbidden();
    TRIP.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void routesPlatformIdentityAndBillingOnlyAfterAuthentication() {
    IAM.stubFor(get("/api/v1/platform/companies").willReturn(okJson("{\"items\":[],\"total\":0}")));
    MONOLITH.stubFor(
        get("/api/v1/platform/plans").willReturn(okJson("{\"items\":[],\"total\":0}")));
    webTestClient
        .get()
        .uri("/api/v1/platform/companies")
        .headers(h -> h.setBearerAuth(owner()))
        .exchange()
        .expectStatus()
        .isOk();
    webTestClient
        .get()
        .uri("/api/v1/platform/plans")
        .headers(h -> h.setBearerAuth(owner()))
        .exchange()
        .expectStatus()
        .isOk();
    webTestClient.get().uri("/api/v1/platform/payments").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void routesPlatformDemoRequestsToIamOnlyForTheOwner() {
    IAM.stubFor(
        get("/api/v1/platform/demo-requests")
            .willReturn(okJson("{\"items\":[],\"total\":0}")));
    webTestClient
        .get()
        .uri("/api/v1/platform/demo-requests")
        .exchange()
        .expectStatus()
        .isUnauthorized();
    webTestClient
        .get()
        .uri("/api/v1/platform/demo-requests")
        .headers(h -> h.setBearerAuth(validToken()))
        .exchange()
        .expectStatus()
        .isForbidden();
    webTestClient
        .get()
        .uri("/api/v1/platform/demo-requests")
        .headers(h -> h.setBearerAuth(owner()))
        .exchange()
        .expectStatus()
        .isOk();
    IAM.verify(1, getRequestedFor(urlEqualTo("/api/v1/platform/demo-requests")));
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void platformSignInIsPublicButNotItsOtherRoutes() {
    IAM.stubFor(post("/api/v1/platform/auth/sign-in").willReturn(okJson("{}")));
    webTestClient.post().uri("/api/v1/platform/auth/sign-in").exchange().expectStatus().isOk();
    webTestClient.get().uri("/api/v1/platform/me").exchange().expectStatus().isUnauthorized();
  }
}
