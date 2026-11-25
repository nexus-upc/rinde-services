package com.nexus.rinde.support;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Doble del microservicio Trip respaldado por WireMock para simular la consulta interna de viajes.
 */
@Component
public class TripStub {

  private final WireMockServer server = WireMockServices.server();

  public void setTripStatus(UUID tenantId, UUID tripId, String status) {
    setTrip(tenantId, tripId, status, null);
  }

  public void setTrip(UUID tenantId, UUID tripId, String status, UUID driverId) {
    String path = "/api/internal/v1/trips/tenants/" + tenantId + "/" + tripId;
    String quotedDriver = driverId == null ? "null" : "\"" + driverId + "\"";
    server.stubFor(
        get(urlEqualTo(path))
            .willReturn(
                okJson(
                    """
                    {
                      "tripId": "%s",
                      "code": "TRP-001",
                      "status": "%s",
                      "origin": "Lima",
                      "destination": "Arequipa",
                      "departureDate": "2026-11-01",
                      "driverId": %s
                    }
                    """
                        .formatted(tripId, status, quotedDriver))));
  }

  public void setTripNotFound(UUID tenantId, UUID tripId) {
    String path = "/api/internal/v1/trips/tenants/" + tenantId + "/" + tripId;
    server.stubFor(get(urlEqualTo(path)).willReturn(notFound()));
  }

  public void reset() {
    // Los mappings específicos se limpian en IamStub.reset() que limpia todo el server WireMock
  }
}
