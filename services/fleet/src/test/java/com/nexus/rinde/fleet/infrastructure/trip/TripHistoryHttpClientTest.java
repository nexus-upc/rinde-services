package com.nexus.rinde.fleet.infrastructure.trip;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.fleet.application.internal.outboundservices.acl.ExternalTripSummary;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.RemoteLookupResult;
import com.nexus.rinde.support.AbstractIntegrationTest;
import com.nexus.rinde.support.WireMockServices;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Pruebas con WireMock para el cliente HTTP de historial de viajes de Fleet. */
class TripHistoryHttpClientTest extends AbstractIntegrationTest {

  @Autowired private TripHistoryHttpClient client;

  @Test
  @DisplayName("Filtra los viajes del vehículo indicado cuando Trip responde")
  void returnsTripsForRequestedVehicle() {
    UUID tenantId = UUID.randomUUID();
    UUID vehicleA = UUID.randomUUID();
    UUID vehicleB = UUID.randomUUID();
    UUID tripA = UUID.randomUUID();

    WireMockServices.server()
        .stubFor(
            get(urlEqualTo("/api/internal/v1/trips/tenants/" + tenantId))
                .willReturn(
                    okJson(
                        "[{\"tripId\":\""
                            + tripA
                            + "\",\"code\":\"TRP-000010\",\"status\":\"FINISHED\",\"origin\":\"Lima\","
                            + "\"destination\":\"Arequipa\",\"cargoDescription\":\"Carga\",\"cargoWeightKg\":1200,"
                            + "\"departureDate\":\"2026-11-20\",\"vehicleId\":\""
                            + vehicleA
                            + "\",\"driverId\":\""
                            + UUID.randomUUID()
                            + "\"},{\"tripId\":\""
                            + UUID.randomUUID()
                            + "\",\"code\":\"TRP-000011\",\"status\":\"ASSIGNED\",\"origin\":\"Lima\","
                            + "\"destination\":\"Ica\",\"cargoDescription\":\"Otro\",\"cargoWeightKg\":500,"
                            + "\"departureDate\":\"2026-11-21\",\"vehicleId\":\""
                            + vehicleB
                            + "\",\"driverId\":\""
                            + UUID.randomUUID()
                            + "\"}]")));

    RemoteLookupResult<List<ExternalTripSummary>> result =
        client.findByTenantAndVehicle(tenantId, vehicleA);

    assertThat(result.available()).isTrue();
    assertThat(result.data())
        .singleElement()
        .satisfies(
            trip -> {
              assertThat(trip.tripId()).isEqualTo(tripA);
              assertThat(trip.code()).isEqualTo("TRP-000010");
            });
  }

  @Test
  @DisplayName("Devuelve unavailable cuando Trip responde con error 503")
  void returnsUnavailableWhenTripFails() {
    UUID tenantId = UUID.randomUUID();
    WireMockServices.server()
        .stubFor(
            get(urlEqualTo("/api/internal/v1/trips/tenants/" + tenantId))
                .willReturn(aResponse().withStatus(503)));

    RemoteLookupResult<List<ExternalTripSummary>> result =
        client.findByTenantAndVehicle(tenantId, UUID.randomUUID());

    assertThat(result.available()).isFalse();
    assertThat(result.data()).isNull();
  }
}
