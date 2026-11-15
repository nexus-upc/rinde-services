package com.nexus.rinde.support;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.stereotype.Component;

/**
 * Doble de IAM respaldado por WireMock para la consulta de estado de la empresa que hace el filtro
 * JWT en cada petición. Por defecto cualquier empresa figura como ACTIVE.
 */
@Component
public class IamStub {

  private static final String TENANT_STATUS_PATH = "/api/internal/v1/tenants/[^/]+/status";
  private final WireMockServer server = WireMockServices.iam();

  public IamStub() {
    apply();
  }

  public synchronized void reset() {
    server.resetMappings();
    apply();
  }

  private void apply() {
    server.stubFor(
        get(urlPathMatching(TENANT_STATUS_PATH)).willReturn(okJson("{\"status\":\"ACTIVE\"}")));
  }
}
