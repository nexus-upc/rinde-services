package com.nexus.rinde.expense.interfaces.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.support.AbstractIntegrationTest;
import com.nexus.rinde.support.RelativeDates;
import com.nexus.rinde.support.WireMockServices;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas de permisos de conductor, disponibilidad de Fleet y esquema OpenAPI en Expense. */
class ExpenseDriverPermissionsIntegrationTest extends AbstractIntegrationTest {

  @Test
  @DisplayName("Registro directo falla con 503 cuando Fleet no responde")
  void directRegisterReturns503WhenFleetIsUnavailable() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    UUID driverUserId = UUID.randomUUID();
    String token = tokens.issue(driverUserId, tenantId, "DRIVER", "luis@andes.pe");
    tripStub.setTrip(tenantId, tripId, "IN_ROUTE", UUID.randomUUID());
    fleetStub.setUnavailable();

    api.register(
            token,
            tripId.toString(),
            "FUEL",
            new BigDecimal("100.00"),
            "PEN",
            RelativeDates.date("HOY"),
            "KEY-FLEET-503")
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.detail").value("No se pudo consultar el conductor en Fleet."));
  }

  @Test
  @DisplayName("Sincronización falla con 503 cuando Fleet no responde para mantener el reintento pendiente")
  void syncReturns503WhenFleetIsUnavailable() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    UUID driverUserId = UUID.randomUUID();
    String token = tokens.issue(driverUserId, tenantId, "DRIVER", "luis@andes.pe");
    tripStub.setTrip(tenantId, tripId, "IN_ROUTE", UUID.randomUUID());
    fleetStub.setUnavailable();

    api.sync(
            token,
            List.of(
                Map.of(
                    "tripId", tripId.toString(),
                    "category", "TOLL",
                    "amount", new BigDecimal("25.00"),
                    "currency", "PEN",
                    "expenseDate", RelativeDates.resolve("HOY"),
                    "idempotencyKey", "KEY-SYNC-FLEET-503")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.detail").value("No se pudo consultar el conductor en Fleet."));
  }

  @Test
  @DisplayName("Sincronización consulta Fleet una sola vez por petición sin importar el tamaño del lote")
  void syncQueriesFleetOnlyOncePerRequest() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID ownTripId = UUID.randomUUID();
    UUID otherTripId = UUID.randomUUID();
    UUID driverUserId = UUID.randomUUID();
    UUID fleetDriverId = UUID.randomUUID();
    String token = tokens.issue(driverUserId, tenantId, "DRIVER", "luis@andes.pe");
    fleetStub.setDriverForUser(tenantId, driverUserId, fleetDriverId);
    tripStub.setTrip(tenantId, ownTripId, "IN_ROUTE", fleetDriverId);
    tripStub.setTrip(tenantId, otherTripId, "IN_ROUTE", UUID.randomUUID());

    api.sync(
            token,
            List.of(
                syncItem(ownTripId, "KEY-ONCE-1"),
                syncItem(ownTripId, "KEY-ONCE-2"),
                syncItem(otherTripId, "KEY-ONCE-3")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.synchronizedCount").value(2))
        .andExpect(jsonPath("$.rejectedCount").value(1))
        .andExpect(jsonPath("$.rejected[0].code").value("TRIP_NOT_ASSIGNED"));

    String driverPath =
        "/api/internal/v1/fleet/tenants/" + tenantId + "/users/" + driverUserId + "/driver";
    WireMockServices.server().verify(1, getRequestedFor(urlEqualTo(driverPath)));
  }

  @Test
  @DisplayName("Conductor sin ficha en Fleet recibe 403 en registro y TRIP_NOT_ASSIGNED en sync")
  void driverWithoutFleetProfileIsRejected() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    UUID driverUserId = UUID.randomUUID();
    String token = tokens.issue(driverUserId, tenantId, "DRIVER", "nuevo@andes.pe");
    fleetStub.setDriverNotFound(tenantId, driverUserId);
    tripStub.setTrip(tenantId, tripId, "IN_ROUTE", UUID.randomUUID());

    api.register(
            token,
            tripId.toString(),
            "FUEL",
            new BigDecimal("80.00"),
            "PEN",
            RelativeDates.date("HOY"),
            "KEY-NO-FLEET-1")
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.detail")
                .value("Solo el conductor asignado al viaje puede registrar gastos."));

    api.sync(token, List.of(syncItem(tripId, "KEY-NO-FLEET-2")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.synchronizedCount").value(0))
        .andExpect(jsonPath("$.rejectedCount").value(1))
        .andExpect(jsonPath("$.rejected[0].code").value("TRIP_NOT_ASSIGNED"));
  }

  @Test
  @DisplayName("OpenAPI incluye TRIP_NOT_ASSIGNED en SyncRejectedItemResource")
  void openApiIncludesTripNotAssignedRejectionCode() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.components.schemas.SyncRejectedItemResource.properties.code.enum",
                hasItem("TRIP_NOT_ASSIGNED")));
  }

  private static Map<String, Object> syncItem(UUID tripId, String idempotencyKey) {
    return Map.of(
        "tripId", tripId.toString(),
        "category", "TOLL",
        "amount", new BigDecimal("25.00"),
        "currency", "PEN",
        "expenseDate", RelativeDates.resolve("HOY"),
        "idempotencyKey", idempotencyKey);
  }
}
