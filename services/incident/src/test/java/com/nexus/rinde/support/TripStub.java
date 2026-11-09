package com.nexus.rinde.support;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Doble de Trip respaldado por WireMock para la consulta interna de un viaje. */
@Component
public class TripStub {

  private final WireMockServer server = WireMockServices.server();

  /** Un viaje sin asignar llega con vehículo y conductor nulos. */
  public void setTrip(UUID tenantId, UUID tripId, String status, UUID vehicleId, UUID driverId) {
    String path = "/api/internal/v1/trips/tenants/" + tenantId + "/" + tripId;
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
                      "vehicleId": %s,
                      "driverId": %s
                    }
                    """
                        .formatted(tripId, status, quoted(vehicleId), quoted(driverId)))));
  }

  private static String quoted(UUID id) {
    return id == null ? "null" : "\"" + id + "\"";
  }
}
