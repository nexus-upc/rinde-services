package com.nexus.rinde.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.nexus.rinde.trip.domain.model.valueobjects.MaintenanceState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Doble de Fleet respaldado por WireMock. En modo permisivo todo existe; en modo estricto solo
 * existen los recursos registrados en el escenario y el resto responde 404.
 */
@Component
public class FleetStub {

  private static final String TENANT_PATH = "/api/internal/v1/fleet/tenants/[^/]+";
  private static final String TENANT_STATUS_PATH = "/api/internal/v1/tenants/[^/]+/status";

  private final WireMockServer server = WireMockServices.server();
  private final Set<UUID> vehicles = new HashSet<>();
  private final Set<UUID> drivers = new HashSet<>();
  private final Map<UUID, UUID> driverByUser = new HashMap<>();
  private final Set<UUID> restrictedTenants = new HashSet<>();
  private MaintenanceState maintenance = MaintenanceState.UP_TO_DATE;
  private boolean driverEnabled = true;
  private boolean strict;
  private boolean unavailable;

  public FleetStub() {
    apply();
  }

  public synchronized void reset() {
    vehicles.clear();
    drivers.clear();
    driverByUser.clear();
    restrictedTenants.clear();
    maintenance = MaintenanceState.UP_TO_DATE;
    driverEnabled = true;
    strict = false;
    unavailable = false;
    apply();
  }

  public synchronized void registerVehicle(UUID vehicleId) {
    vehicles.add(vehicleId);
    apply();
  }

  /** Registra al conductor en Fleet y lo vincula con el usuario de la invitación. */
  public synchronized void registerDriver(UUID driverId, UUID userId) {
    drivers.add(driverId);
    driverByUser.put(userId, driverId);
    apply();
  }

  /** En modo permisivo, un usuario DRIVER invitado es conductor de Fleet con su mismo id. */
  public synchronized void userInvitedAsDriver(UUID userId) {
    if (!strict) {
      driverByUser.put(userId, userId);
    }
    apply();
  }

  /** Desde ahora solo existen los recursos registrados explícitamente. */
  public synchronized void useStrictRegistry() {
    strict = true;
    apply();
  }

  public synchronized void setMaintenance(MaintenanceState state) {
    maintenance = state;
    apply();
  }

  public synchronized void setDriverEnabled(boolean enabled) {
    driverEnabled = enabled;
    apply();
  }

  /** Simula que Fleet no responde: todas sus consultas devuelven 500. */
  public synchronized void setUnavailable(boolean value) {
    unavailable = value;
    apply();
  }

  /** IAM informa a la empresa como restringida por su suscripción vencida. */
  public synchronized void restrictTenant(UUID tenantId) {
    restrictedTenants.add(tenantId);
    apply();
  }

  private void apply() {
    server.resetMappings();
    String health =
        "{\"maintenanceState\":\""
            + maintenance.name()
            + "\",\"availableForTrip\":"
            + (maintenance != MaintenanceState.OVERDUE)
            + "}";
    String eligibility = "{\"eligible\":" + driverEnabled + ",\"reason\":\"\"}";

    for (UUID vehicleId : vehicles) {
      stub(TENANT_PATH + "/vehicles/" + vehicleId + "/health", 1, okJson(health));
    }
    stub(TENANT_PATH + "/vehicles/[^/]+/health", 5, strict ? missing() : okJson(health));

    for (UUID driverId : drivers) {
      stub(TENANT_PATH + "/drivers/" + driverId + "/eligibility", 1, okJson(eligibility));
    }
    stub(TENANT_PATH + "/drivers/[^/]+/eligibility", 5, strict ? missing() : okJson(eligibility));

    driverByUser.forEach(
        (userId, driverId) ->
            stub(
                TENANT_PATH + "/users/" + userId + "/driver",
                1,
                okJson("{\"driverId\":\"" + driverId + "\"}")));
    stub(TENANT_PATH + "/users/[^/]+/driver", 5, missing());

    if (unavailable) {
      stub(TENANT_PATH + "/.*", 0, serverError());
    }
    server.stubFor(
        get(urlPathMatching(TENANT_STATUS_PATH)).willReturn(okJson("{\"status\":\"ACTIVE\"}")));
    for (UUID tenantId : restrictedTenants) {
      stub(
          "/api/internal/v1/tenants/" + tenantId + "/status",
          1,
          okJson("{\"status\":\"RESTRICTED\"}"));
    }
  }

  private void stub(String pathPattern, int priority, ResponseDefinitionBuilder response) {
    server.stubFor(get(urlPathMatching(pathPattern)).atPriority(priority).willReturn(response));
  }

  private static ResponseDefinitionBuilder missing() {
    return aResponse()
        .withStatus(404)
        .withHeader("Content-Type", "application/problem+json")
        .withBody("{\"status\":404}");
  }
}
