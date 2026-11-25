package com.nexus.rinde.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Doble de Fleet respaldado por WireMock. Un usuario sin ficha de conductor responde 404, igual
 * que Fleet.
 */
@Component
public class FleetStub {

  private final WireMockServer server = WireMockServices.server();

  public void setDriverForUser(UUID tenantId, UUID userId, UUID driverId) {
    String path = "/api/internal/v1/fleet/tenants/" + tenantId + "/users/" + userId + "/driver";
    server.stubFor(get(urlEqualTo(path)).willReturn(okJson("{\"driverId\": \"" + driverId + "\"}")));
  }

  public void setDriverNotFound(UUID tenantId, UUID userId) {
    String path = "/api/internal/v1/fleet/tenants/" + tenantId + "/users/" + userId + "/driver";
    server.stubFor(get(urlEqualTo(path)).willReturn(notFound()));
  }

  public void setUnavailable() {
    server.stubFor(
        get(urlPathMatching("/api/internal/v1/fleet/.*"))
            .atPriority(1)
            .willReturn(aResponse().withStatus(503)));
  }
}
