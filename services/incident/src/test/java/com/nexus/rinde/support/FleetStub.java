package com.nexus.rinde.support;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Doble de Fleet respaldado por WireMock. Un usuario sin conductor no tiene mapping y WireMock
 * responde 404, igual que Fleet.
 */
@Component
public class FleetStub {

  private final WireMockServer server = WireMockServices.server();

  public void setDriverForUser(UUID tenantId, UUID userId, UUID driverId) {
    String path = "/api/internal/v1/fleet/tenants/" + tenantId + "/users/" + userId + "/driver";
    server.stubFor(get(urlEqualTo(path)).willReturn(okJson("{\"driverId\": \"" + driverId + "\"}")));
  }
}
