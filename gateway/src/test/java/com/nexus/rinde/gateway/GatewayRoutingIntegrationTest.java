package com.nexus.rinde.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
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
  void routesPlansSubscriptionsAndBillingToTheSubscriptionService() {
    String token = validToken();
    SUBSCRIPTION.stubFor(get(urlEqualTo("/api/v1/plans")).willReturn(okJson("[]")));
    SUBSCRIPTION.stubFor(
        get(urlEqualTo("/api/v1/subscriptions/current")).willReturn(okJson("{}")));

    for (String path : new String[] {"/api/v1/plans", "/api/v1/subscriptions/current"}) {
      webTestClient
          .get()
          .uri(path)
          .header(AUTHORIZATION, "Bearer " + token)
          .exchange()
          .expectStatus()
          .isOk();
    }

    SUBSCRIPTION.verify(
        getRequestedFor(urlEqualTo("/api/v1/plans"))
            .withHeader(AUTHORIZATION, equalTo("Bearer " + token)));
    SUBSCRIPTION.verify(getRequestedFor(urlEqualTo("/api/v1/subscriptions/current")));
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
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

  @Test
  void routesSprint4PublicEndpointsToEachOwningServiceAndRequiresJwtForCurrentTenant() {
    String token = validToken();

    webTestClient.get().uri("/api/v1/tenants/current").exchange().expectStatus().isUnauthorized();

    IAM.stubFor(get(urlEqualTo("/api/v1/tenants/current")).willReturn(okJson("{\"id\":\"t1\"}")));
    IAM.stubFor(patch(urlEqualTo("/api/v1/tenants/current")).willReturn(okJson("{\"id\":\"t1\"}")));
    IAM.stubFor(patch(urlEqualTo("/api/v1/users/me")).willReturn(okJson("{\"id\":\"u1\"}")));
    IAM.stubFor(post(urlEqualTo("/api/v1/users/me/password")).willReturn(aResponse().withStatus(204)));
    EXPENSE.stubFor(get(urlEqualTo("/api/v1/expenses")).willReturn(okJson("{\"items\":[]}")));
    FLEET.stubFor(get(urlEqualTo("/api/v1/vehicles/v1/history")).willReturn(okJson("{\"items\":[]}")));
    INCIDENT.stubFor(post(urlEqualTo("/api/v1/incidents/i1/evidences")).willReturn(aResponse().withStatus(201).withBody("{}")));
    INCIDENT.stubFor(get(urlEqualTo("/api/v1/incidents/i1/evidences/e1/content")).willReturn(aResponse().withStatus(200)));
    MONOLITH.stubFor(get(urlEqualTo("/api/v1/dashboard/charts")).willReturn(okJson("{}")));
    MONOLITH.stubFor(get(urlEqualTo("/api/v1/notifications")).willReturn(okJson("{\"items\":[]}")));
    MONOLITH.stubFor(get(urlEqualTo("/api/v1/notifications/unread-count")).willReturn(okJson("{\"count\":0}")));
    MONOLITH.stubFor(patch(urlEqualTo("/api/v1/notifications/n1/read")).willReturn(aResponse().withStatus(204)));
    MONOLITH.stubFor(post(urlEqualTo("/api/v1/notifications/read-all")).willReturn(aResponse().withStatus(204)));
    MONOLITH.stubFor(post(urlEqualTo("/api/v1/notifications/devices")).willReturn(aResponse().withStatus(204)));
    MONOLITH.stubFor(delete(urlEqualTo("/api/v1/notifications/devices/tok1")).willReturn(aResponse().withStatus(204)));

    webTestClient.get().uri("/api/v1/tenants/current").header(AUTHORIZATION, "Bearer " + token).exchange().expectStatus().isOk();
    webTestClient.patch().uri("/api/v1/tenants/current").header(AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange().expectStatus().isOk();
    webTestClient.patch().uri("/api/v1/users/me").header(AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON).bodyValue("{\"fullName\":\"Ana\"}").exchange().expectStatus().isOk();
    webTestClient.post().uri("/api/v1/users/me/password").header(AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON).bodyValue("{\"currentPassword\":\"12345678\",\"newPassword\":\"87654321\"}").exchange().expectStatus().isNoContent();

    webTestClient.get().uri("/api/v1/expenses").header(AUTHORIZATION, "Bearer " + token).exchange().expectStatus().isOk();
    webTestClient.get().uri("/api/v1/vehicles/v1/history").header(AUTHORIZATION, "Bearer " + token).exchange().expectStatus().isOk();
    webTestClient.post().uri("/api/v1/incidents/i1/evidences").header(AUTHORIZATION, "Bearer " + token).exchange().expectStatus().isCreated();
    webTestClient.get().uri("/api/v1/incidents/i1/evidences/e1/content").header(AUTHORIZATION, "Bearer " + token).exchange().expectStatus().isOk();

    webTestClient.get().uri("/api/v1/dashboard/charts").header(AUTHORIZATION, "Bearer " + token).exchange().expectStatus().isOk();
    webTestClient.get().uri("/api/v1/notifications").header(AUTHORIZATION, "Bearer " + token).exchange().expectStatus().isOk();
    webTestClient.get().uri("/api/v1/notifications/unread-count").header(AUTHORIZATION, "Bearer " + token).exchange().expectStatus().isOk();
    webTestClient.patch().uri("/api/v1/notifications/n1/read").header(AUTHORIZATION, "Bearer " + token).exchange().expectStatus().isNoContent();
    webTestClient.post().uri("/api/v1/notifications/read-all").header(AUTHORIZATION, "Bearer " + token).exchange().expectStatus().isNoContent();
    webTestClient.post().uri("/api/v1/notifications/devices").header(AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON).bodyValue("{\"token\":\"tok1\",\"platform\":\"ANDROID\"}").exchange().expectStatus().isNoContent();
    webTestClient.delete().uri("/api/v1/notifications/devices/tok1").header(AUTHORIZATION, "Bearer " + token).exchange().expectStatus().isNoContent();

    IAM.verify(getRequestedFor(urlEqualTo("/api/v1/tenants/current")));
    IAM.verify(patchRequestedFor(urlEqualTo("/api/v1/tenants/current")));
    IAM.verify(patchRequestedFor(urlEqualTo("/api/v1/users/me")));
    IAM.verify(postRequestedFor(urlEqualTo("/api/v1/users/me/password")));
    EXPENSE.verify(getRequestedFor(urlEqualTo("/api/v1/expenses")));
    FLEET.verify(getRequestedFor(urlEqualTo("/api/v1/vehicles/v1/history")));
    INCIDENT.verify(postRequestedFor(urlEqualTo("/api/v1/incidents/i1/evidences")));
    INCIDENT.verify(getRequestedFor(urlEqualTo("/api/v1/incidents/i1/evidences/e1/content")));
    MONOLITH.verify(getRequestedFor(urlEqualTo("/api/v1/dashboard/charts")));
    MONOLITH.verify(getRequestedFor(urlEqualTo("/api/v1/notifications")));
    MONOLITH.verify(getRequestedFor(urlEqualTo("/api/v1/notifications/unread-count")));
    MONOLITH.verify(patchRequestedFor(urlEqualTo("/api/v1/notifications/n1/read")));
    MONOLITH.verify(postRequestedFor(urlEqualTo("/api/v1/notifications/read-all")));
    MONOLITH.verify(postRequestedFor(urlEqualTo("/api/v1/notifications/devices")));
    MONOLITH.verify(deleteRequestedFor(urlEqualTo("/api/v1/notifications/devices/tok1")));
  }
}
