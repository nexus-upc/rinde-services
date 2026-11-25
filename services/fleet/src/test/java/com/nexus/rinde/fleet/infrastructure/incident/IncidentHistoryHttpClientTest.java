package com.nexus.rinde.fleet.infrastructure.incident;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.fleet.application.internal.outboundservices.acl.ExternalIncidentSummary;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.RemoteLookupResult;
import com.nexus.rinde.support.AbstractIntegrationTest;
import com.nexus.rinde.support.WireMockServices;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Pruebas con WireMock para el cliente HTTP de historial de incidencias de Fleet. */
class IncidentHistoryHttpClientTest extends AbstractIntegrationTest {

  @Autowired private IncidentHistoryHttpClient client;

  @Test
  @DisplayName("Devuelve las incidencias del vehículo cuando Incident responde")
  void returnsIncidentsForVehicle() {
    UUID tenantId = UUID.randomUUID();
    UUID vehicleId = UUID.randomUUID();
    UUID incidentId = UUID.randomUUID();

    WireMockServices.server()
        .stubFor(
            get(urlEqualTo(
                    "/api/internal/v1/incidents/tenants/" + tenantId + "?vehicleId=" + vehicleId))
                .willReturn(
                    okJson(
                        "[{\"id\":\""
                            + incidentId
                            + "\",\"tripId\":\""
                            + UUID.randomUUID()
                            + "\",\"vehicleId\":\""
                            + vehicleId
                            + "\",\"type\":\"BREAKDOWN\",\"description\":\"Falla en freno\","
                            + "\"status\":\"CLOSED\",\"reportedAt\":\"2026-11-20T13:20:00Z\"}]")));

    RemoteLookupResult<List<ExternalIncidentSummary>> result =
        client.findByTenantAndVehicle(tenantId, vehicleId);

    assertThat(result.available()).isTrue();
    assertThat(result.data())
        .singleElement()
        .satisfies(
            inc -> {
              assertThat(inc.id()).isEqualTo(incidentId);
              assertThat(inc.description()).isEqualTo("Falla en freno");
              assertThat(inc.status()).isEqualTo("CLOSED");
            });
  }

  @Test
  @DisplayName("Devuelve unavailable cuando Incident responde con error 503")
  void returnsUnavailableWhenIncidentFails() {
    UUID tenantId = UUID.randomUUID();
    UUID vehicleId = UUID.randomUUID();
    WireMockServices.server()
        .stubFor(
            get(urlEqualTo(
                    "/api/internal/v1/incidents/tenants/" + tenantId + "?vehicleId=" + vehicleId))
                .willReturn(aResponse().withStatus(503)));

    RemoteLookupResult<List<ExternalIncidentSummary>> result =
        client.findByTenantAndVehicle(tenantId, vehicleId);

    assertThat(result.available()).isFalse();
    assertThat(result.data()).isNull();
  }
}
