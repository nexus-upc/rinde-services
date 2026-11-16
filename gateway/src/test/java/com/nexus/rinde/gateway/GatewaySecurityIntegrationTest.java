package com.nexus.rinde.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityIntegrationTest extends AbstractGatewayIntegrationTest {

  private static final String TRIP_PATH = "/api/v1/trips/123";

  @Test
  void rejectsProtectedRouteWithoutToken() {
    webTestClient
        .get()
        .uri(TRIP_PATH)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        .expectHeader()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(401)
        .jsonPath("$.detail")
        .isEqualTo("Token ausente o inválido.");

    TRIP.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void rejectsTokenSignedWithAnotherSecret() {
    String forged = token(OTHER_SECRET, Instant.now().plus(Duration.ofHours(1)));

    assertRejected(forged);
  }

  @Test
  void rejectsExpiredToken() {
    String expired = token(TEST_SECRET, Instant.now().minus(Duration.ofMinutes(5)));

    assertRejected(expired);
  }

  @Test
  void rejectsMalformedAuthorizationHeaders() {
    webTestClient
        .get()
        .uri(TRIP_PATH)
        .header(AUTHORIZATION, "Bearer no-es-un-jwt")
        .exchange()
        .expectStatus()
        .isUnauthorized();
    webTestClient
        .get()
        .uri(TRIP_PATH)
        .header(AUTHORIZATION, "Basic " + "dXN1YXJpbzpjbGF2ZQ==")
        .exchange()
        .expectStatus()
        .isUnauthorized();

    TRIP.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void letsPublicTenantRegistrationThroughWithoutToken() {
    IAM.stubFor(post(urlEqualTo("/api/v1/tenants")).willReturn(aResponse().withStatus(201)));

    webTestClient
        .post()
        .uri("/api/v1/tenants")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isEqualTo(201);

    IAM.verify(1, anyRequestedFor(anyUrl()));
  }

  @Test
  void letsSignedBillingWebhookThroughWithoutToken() {
    SUBSCRIPTION.stubFor(post(urlEqualTo("/api/v1/billing/webhooks/payment")).willReturn(aResponse().withStatus(202)));

    webTestClient
        .post()
        .uri("/api/v1/billing/webhooks/payment")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isEqualTo(202);

    SUBSCRIPTION.verify(1, anyRequestedFor(anyUrl()));
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void keepsPrivateIamRoutesBehindTheToken() {
    IAM.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));

    webTestClient.get().uri("/api/v1/users/42").exchange().expectStatus().isUnauthorized();

    IAM.verify(0, anyRequestedFor(anyUrl()));
  }

  private void assertRejected(String token) {
    webTestClient
        .get()
        .uri(TRIP_PATH)
        .header(AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus()
        .isUnauthorized();

    TRIP.verify(0, anyRequestedFor(anyUrl()));
  }
}
