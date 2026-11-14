package com.nexus.rinde.subscription.infrastructure.fleet;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Cliente HTTP hacia Fleet, con un servidor WireMock propio y sin contexto de Spring. */
class FleetVehicleCountHttpClientTest {

  private static WireMockServer fleet;

  private FleetVehicleCountHttpClient client;
  private UUID tenantId;
  private String countPath;

  @BeforeAll
  static void startFleet() {
    fleet = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    fleet.start();
  }

  @AfterAll
  static void stopFleet() {
    fleet.stop();
  }

  @BeforeEach
  void prepareClient() {
    fleet.resetAll();
    client = new FleetVehicleCountHttpClient(fleet.baseUrl());
    tenantId = UUID.randomUUID();
    countPath = "/api/internal/v1/fleet/tenants/" + tenantId + "/vehicles/count";
  }

  @Test
  @DisplayName("Devuelve la cantidad de unidades que informa Fleet")
  void returnsTheVehicleCountReportedByFleet() {
    fleet.stubFor(get(urlEqualTo(countPath)).willReturn(okJson("{\"count\":4}")));

    assertThat(client.countVehicles(tenantId)).isEqualTo(4L);
  }

  @Test
  @DisplayName("Responde 503 cuando Fleet no conoce la empresa (404)")
  void answersServiceUnavailableWhenFleetReturnsNotFound() {
    fleet.stubFor(get(urlEqualTo(countPath)).willReturn(notFound()));

    assertServiceUnavailable();
  }

  @Test
  @DisplayName("Responde 503 cuando Fleet contesta sin cuerpo")
  void answersServiceUnavailableWhenFleetReturnsNoBody() {
    fleet.stubFor(get(urlEqualTo(countPath)).willReturn(aResponse().withStatus(200)));

    assertServiceUnavailable();
  }

  @Test
  @DisplayName("Responde 503 cuando Fleet falla internamente (500)")
  void answersServiceUnavailableWhenFleetFails() {
    fleet.stubFor(get(urlEqualTo(countPath)).willReturn(serverError()));

    assertServiceUnavailable();
  }

  private void assertServiceUnavailable() {
    assertThatThrownBy(() -> client.countVehicles(tenantId))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
  }
}
