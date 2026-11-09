package com.nexus.rinde.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayRoutingIntegrationTest extends AbstractGatewayIntegrationTest {

  private static final String INTERNAL_STATUS =
      "/api/internal/v1/tenants/00000000-0000-0000-0000-000000000000/status";

  @Autowired private SwaggerUiConfigProperties swaggerUi;

  @Test
  void sendsSignInWithoutTokenToIamAndKeepsItsErrorBody() {
    IAM.stubFor(
        post(urlEqualTo("/api/v1/auth/sign-in"))
            .willReturn(
                aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/problem+json")
                    .withBody("{\"status\":401,\"detail\":\"Credenciales inválidas.\"}")));

    webTestClient
        .post()
        .uri("/api/v1/auth/sign-in")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"email\":\"admin@rinde.test\",\"password\":\"clave-incorrecta\"}")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.detail")
        .isEqualTo("Credenciales inválidas.");

    IAM.verify(postRequestedFor(urlEqualTo("/api/v1/auth/sign-in")));
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void routesTripsToTripServiceAndForwardsAuthorizationUnchanged() {
    String token = validToken();
    TRIP.stubFor(get(urlEqualTo("/api/v1/trips")).willReturn(okJson("[]")));
    TRIP.stubFor(
        get(urlEqualTo("/api/v1/trips/123")).willReturn(okJson("{\"tripId\":\"123\"}")));

    webTestClient
        .get()
        .uri("/api/v1/trips")
        .header(AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus()
        .isOk();
    webTestClient
        .get()
        .uri("/api/v1/trips/123")
        .header(AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.tripId")
        .isEqualTo("123");

    TRIP.verify(
        getRequestedFor(urlEqualTo("/api/v1/trips"))
            .withHeader(AUTHORIZATION, equalTo("Bearer " + token)));
    TRIP.verify(getRequestedFor(urlEqualTo("/api/v1/trips/123")));
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
    IAM.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void routesTripExpensesToExpenseServiceAndSettlementToMonolith() {
    String token = validToken();
    EXPENSE.stubFor(get(urlEqualTo("/api/v1/trips/123/expenses")).willReturn(okJson("[]")));
    EXPENSE.stubFor(get(urlEqualTo("/api/v1/expenses/456")).willReturn(okJson("{}")));
    MONOLITH.stubFor(get(urlEqualTo("/api/v1/trips/123/settlement")).willReturn(okJson("{}")));

    for (String path :
        new String[] {
          "/api/v1/trips/123/expenses", "/api/v1/expenses/456", "/api/v1/trips/123/settlement"
        }) {
      webTestClient
          .get()
          .uri(path)
          .header(AUTHORIZATION, "Bearer " + token)
          .exchange()
          .expectStatus()
          .isOk();
    }

    EXPENSE.verify(
        getRequestedFor(urlEqualTo("/api/v1/trips/123/expenses"))
            .withHeader(AUTHORIZATION, equalTo("Bearer " + token)));
    EXPENSE.verify(getRequestedFor(urlEqualTo("/api/v1/expenses/456")));
    MONOLITH.verify(1, anyRequestedFor(anyUrl()));
    MONOLITH.verify(getRequestedFor(urlEqualTo("/api/v1/trips/123/settlement")));
    TRIP.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void routesTripAdvancesToTheMonolithNextToTheSettlement() {
    String token = validToken();
    MONOLITH.stubFor(
        get(urlEqualTo("/api/v1/trips/123/advances")).willReturn(okJson("[]")));
    MONOLITH.stubFor(
        post(urlEqualTo("/api/v1/trips/123/advances"))
            .willReturn(
                aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"tripId\":\"123\"}")));

    webTestClient
        .get()
        .uri("/api/v1/trips/123/advances")
        .header(AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus()
        .isOk();
    webTestClient
        .post()
        .uri("/api/v1/trips/123/advances")
        .header(AUTHORIZATION, "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"amount\":500.00,\"deliveryMethod\":\"CASH\"}")
        .exchange()
        .expectStatus()
        .isCreated();

    MONOLITH.verify(
        getRequestedFor(urlEqualTo("/api/v1/trips/123/advances"))
            .withHeader(AUTHORIZATION, equalTo("Bearer " + token)));
    MONOLITH.verify(postRequestedFor(urlEqualTo("/api/v1/trips/123/advances")));
    TRIP.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void routesVehiclesDriversAndMaintenancesToFleetService() {
    String token = validToken();
    for (String path : new String[] {"/api/v1/vehicles", "/api/v1/drivers", "/api/v1/maintenances"}) {
      FLEET.stubFor(get(urlEqualTo(path)).willReturn(okJson("[]")));
      webTestClient
          .get()
          .uri(path)
          .header(AUTHORIZATION, "Bearer " + token)
          .exchange()
          .expectStatus()
          .isOk();
      FLEET.verify(getRequestedFor(urlEqualTo(path)));
    }
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
  }


  @Test
  void servesTripOpenApiDocsThroughRewrittenRoute() {
    TRIP.stubFor(get(urlEqualTo("/v3/api-docs")).willReturn(okJson("{\"openapi\":\"3.1.0\"}")));

    webTestClient
        .get()
        .uri("/v3/api-docs/trip")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.openapi")
        .exists();

    TRIP.verify(1, getRequestedFor(urlEqualTo("/v3/api-docs")));
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void sendsPublicDemoRequestsToIamWithoutToken() {
    String path = "/api/v1/public/demo-requests";
    IAM.stubFor(
        post(urlEqualTo(path))
            .willReturn(
                aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\":\"RECEIVED\"}")));

    webTestClient
        .post()
        .uri(path)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"fullName\":\"Ana Rojas Paredes\"}")
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("RECEIVED");

    IAM.verify(postRequestedFor(urlEqualTo(path)));
    assertThat(IAM.findAll(postRequestedFor(urlEqualTo(path))).get(0).containsHeader(AUTHORIZATION))
        .isFalse();
    webTestClient.get().uri(path).exchange().expectStatus().isUnauthorized();
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void routesUsersAndTenantsToIam() {
    String token = validToken();
    IAM.stubFor(
        get(urlEqualTo("/api/v1/users/42")).willReturn(okJson("{\"id\":\"42\"}")));

    webTestClient
        .get()
        .uri("/api/v1/users/42")
        .header(AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus()
        .isOk();

    IAM.verify(getRequestedFor(urlEqualTo("/api/v1/users/42")));
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void servesOpenApiDocsOfBothServicesThroughRewrittenRoutes() {
    IAM.stubFor(
        get(urlEqualTo("/v3/api-docs")).willReturn(okJson("{\"openapi\":\"3.1.0\"}")));
    MONOLITH.stubFor(
        get(urlEqualTo("/v3/api-docs")).willReturn(okJson("{\"openapi\":\"3.1.0\"}")));

    webTestClient
        .get()
        .uri("/v3/api-docs/iam")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.openapi")
        .exists();
    webTestClient.get().uri("/v3/api-docs/core").exchange().expectStatus().isOk();

    IAM.verify(1, getRequestedFor(urlEqualTo("/v3/api-docs")));
    MONOLITH.verify(1, getRequestedFor(urlEqualTo("/v3/api-docs")));
  }

  @Test
  void listsTheCoreDocumentsInSwaggerUnderTheirName() {
    assertThat(swaggerUi.getUrls())
        .anySatisfy(
            url -> {
              assertThat(url.getName()).isEqualTo("core");
              assertThat(url.getUrl()).isEqualTo("/v3/api-docs/core");
            });
  }

  @Test
  void exposesSwaggerUiWithoutToken() {
    webTestClient.get().uri("/swagger-ui.html").exchange().expectStatus().is3xxRedirection();
  }

  @Test
  void neverForwardsInternalApiAndAnswersNotFound() {
    IAM.stubFor(get(anyUrl()).willReturn(okJson("{}")));

    webTestClient.get().uri(INTERNAL_STATUS).exchange().expectStatus().isNotFound();

    IAM.verify(0, anyRequestedFor(anyUrl()));
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void internalApiIsNotFoundEvenWithValidToken() {
    IAM.stubFor(get(anyUrl()).willReturn(okJson("{}")));

    webTestClient
        .get()
        .uri(INTERNAL_STATUS)
        .header(AUTHORIZATION, "Bearer " + validToken())
        .exchange()
        .expectStatus()
        .isNotFound();

    IAM.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void rejectsRelativeSegmentsThatWouldReachInternalApi() {
    IAM.stubFor(get(anyUrl()).willReturn(okJson("{}")));

    webTestClient
        .get()
        .uri(URI.create("/api/v1/%2e%2e/internal/v1/tenants/x/status"))
        .exchange()
        .expectStatus()
        .isBadRequest();
    webTestClient
        .get()
        .uri(URI.create("/api/v1/../internal/v1/tenants/x/status"))
        .exchange()
        .expectStatus()
        .isBadRequest();

    IAM.verify(0, anyRequestedFor(anyUrl()));
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void reportsHealthAsUp() {
    webTestClient
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("UP");
  }
}
