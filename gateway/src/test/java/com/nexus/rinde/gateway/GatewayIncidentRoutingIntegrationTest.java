package com.nexus.rinde.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayIncidentRoutingIntegrationTest extends AbstractGatewayIntegrationTest {

  @Test
  void routesTripIncidentsAndIncidentListToIncidentServiceOnly() {
    String token = validToken();
    INCIDENT.stubFor(get(urlEqualTo("/api/v1/trips/123/incidents")).willReturn(okJson("[]")));
    INCIDENT.stubFor(get(urlEqualTo("/api/v1/incidents")).willReturn(okJson("[]")));

    for (String path : new String[] {"/api/v1/trips/123/incidents", "/api/v1/incidents"}) {
      webTestClient
          .get()
          .uri(path)
          .header(AUTHORIZATION, "Bearer " + token)
          .exchange()
          .expectStatus()
          .isOk();
    }

    INCIDENT.verify(
        getRequestedFor(urlEqualTo("/api/v1/trips/123/incidents"))
            .withHeader(AUTHORIZATION, equalTo("Bearer " + token)));
    INCIDENT.verify(getRequestedFor(urlEqualTo("/api/v1/incidents")));
    TRIP.verify(0, anyRequestedFor(anyUrl()));
    MONOLITH.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void doesNotExposeInternalIncidentEndpoints() {
    webTestClient
        .get()
        .uri("/api/internal/v1/incidents/tenants/00000000-0000-0000-0000-000000000000/open-count")
        .exchange()
        .expectStatus()
        .isNotFound();

    INCIDENT.verify(0, anyRequestedFor(anyUrl()));
  }
}
