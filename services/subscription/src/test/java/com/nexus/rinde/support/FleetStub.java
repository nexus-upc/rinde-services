package com.nexus.rinde.support;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Doble de Fleet respaldado por WireMock para la cantidad de unidades que Subscriptions consulta
 * antes de validar un plan (GET /api/internal/v1/fleet/tenants/{tenantId}/vehicles/count). Por
 * defecto la empresa no tiene unidades registradas.
 */
@Component
public class FleetStub {

  private static final String COUNT_PATH = "/api/internal/v1/fleet/tenants/[^/]+/vehicles/count";
  private final WireMockServer server = WireMockServices.fleet();

  public FleetStub() {
    apply();
  }

  public synchronized void reset() {
    server.resetMappings();
    apply();
  }

  /** Desde ahora Fleet informa a la empresa la cantidad de unidades indicada. */
  public synchronized void setVehicleCount(UUID tenantId, long count) {
    server.stubFor(
        get(urlEqualTo("/api/internal/v1/fleet/tenants/" + tenantId + "/vehicles/count"))
            .atPriority(1)
            .willReturn(okJson("{\"count\":" + count + "}")));
  }

  private void apply() {
    server.stubFor(
        get(urlPathMatching(COUNT_PATH)).willReturn(okJson("{\"count\":0}")));
  }
}
