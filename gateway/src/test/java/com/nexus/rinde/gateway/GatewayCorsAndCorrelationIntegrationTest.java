package com.nexus.rinde.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.ORIGIN;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayCorsAndCorrelationIntegrationTest extends AbstractGatewayIntegrationTest {

  private static final String CORRELATION = "X-Correlation-Id";

  @LocalServerPort int port;

  @Test
  void answersPreflightFromAllowedOriginWithoutCallingServices() throws Exception {
    HttpResponse<Void> response = preflight(ALLOWED_ORIGIN);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue(ACCESS_CONTROL_ALLOW_ORIGIN))
        .contains(ALLOWED_ORIGIN);
    assertThat(response.headers().firstValue(ACCESS_CONTROL_ALLOW_HEADERS)).isPresent();
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void answersPreflightFromLandingOriginForPublicDemoRequests() throws Exception {
    HttpResponse<Void> response =
        preflight(LANDING_ORIGIN, "/api/v1/public/demo-requests");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue(ACCESS_CONTROL_ALLOW_ORIGIN))
        .contains(LANDING_ORIGIN);
    IAM.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void doesNotGrantCorsToOtherOrigins() throws Exception {
    HttpResponse<Void> response = preflight("http://sitio-ajeno.example");

    assertThat(response.statusCode()).isEqualTo(403);
    assertThat(response.headers().firstValue(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty();
  }

  private HttpResponse<Void> preflight(String origin) throws IOException, InterruptedException {
    return preflight(origin, "/api/v1/trips");
  }

  /**
   * Usa un cliente HTTP real: WebTestClient no envía el preflight igual que un navegador y el
   * procesador CORS lo rechaza.
   */
  private HttpResponse<Void> preflight(String origin, String path)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .header(ORIGIN, origin)
            .header(ACCESS_CONTROL_REQUEST_METHOD, "POST")
            .header(ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type,x-correlation-id")
            .build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
  }

  @Test
  void addsCorrelationIdWhenTheClientSendsNone() {
    webTestClient
        .get()
        .uri("/api/v1/trips/123")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .exists(CORRELATION);
  }

  @Test
  void propagatesValidCorrelationIdToServicesAndClient() {
    TRIP.stubFor(get(urlEqualTo("/api/v1/trips/123")).willReturn(okJson("{}")));

    webTestClient
        .get()
        .uri("/api/v1/trips/123")
        .header(AUTHORIZATION, "Bearer " + validToken())
        .header(CORRELATION, "pedido-2026.10.25_01")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals(CORRELATION, "pedido-2026.10.25_01");

    TRIP.verify(
        getRequestedFor(urlEqualTo("/api/v1/trips/123"))
            .withHeader(CORRELATION, equalTo("pedido-2026.10.25_01")));
  }

  @Test
  void replacesInvalidCorrelationId() {
    String invalid = "x".repeat(70);

    webTestClient
        .get()
        .uri("/api/v1/trips/123")
        .header(CORRELATION, invalid)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .value(CORRELATION, value -> org.assertj.core.api.Assertions.assertThat(value).isNotEqualTo(invalid));
  }
}
