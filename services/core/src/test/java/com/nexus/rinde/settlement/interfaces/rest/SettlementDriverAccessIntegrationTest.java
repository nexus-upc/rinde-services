package com.nexus.rinde.settlement.interfaces.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.settlement.infrastructure.messaging.SettlementTripMessaging;
import com.nexus.rinde.support.AbstractIntegrationTest;
import com.nexus.rinde.support.ContextDoubles.Account;
import com.nexus.rinde.support.SettlementApi;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Comprueba que el conductor solo consulta liquidación y adelantos de viajes asignados a su ficha
 * en Fleet, incluso cuando driverId de Fleet difiere de userId de IAM.
 */
class SettlementDriverAccessIntegrationTest extends AbstractIntegrationTest {

  @Autowired private SettlementTripMessaging messaging;

  @Test
  @DisplayName("El conductor ve solo su viaje resolviendo driverId en Fleet; otro conductor recibe 404")
  void driverQueriesOwnTripViaFleetIdWhileOtherDriverGets404() throws Exception {
    UUID tenantId = UUID.fromString(api.registerAndVerifyTenant("ana@andes.pe"));
    String adminToken = api.accessTokenFor("ana@andes.pe");

    UUID opsUserId = UUID.randomUUID();
    contextDoubles.addAccount(new Account(opsUserId, tenantId, "OPERATIONS_MANAGER", "ops@andes.pe"));
    String opsToken = tokens.issue(opsUserId, tenantId, "OPERATIONS_MANAGER", "ops@andes.pe");

    UUID driverUserId = UUID.randomUUID();
    UUID fleetDriverId = UUID.randomUUID();
    contextDoubles.addAccount(new Account(driverUserId, tenantId, "DRIVER", "luis@andes.pe"));
    contextDoubles.addDriver(
        tenantId, fleetDriverId, driverUserId, "Luis Quispe", "Q1234567", "A-IIb");
    String driverToken = tokens.issue(driverUserId, tenantId, "DRIVER", "luis@andes.pe");

    UUID otherDriverUserId = UUID.randomUUID();
    UUID otherFleetDriverId = UUID.randomUUID();
    contextDoubles.addAccount(new Account(otherDriverUserId, tenantId, "DRIVER", "marta@andes.pe"));
    contextDoubles.addDriver(
        tenantId, otherFleetDriverId, otherDriverUserId, "Marta Ruiz", "Q7654321", "A-IIb");
    String otherDriverToken = tokens.issue(otherDriverUserId, tenantId, "DRIVER", "marta@andes.pe");

    UUID unlinkedDriverUserId = UUID.randomUUID();
    String unlinkedDriverToken =
        tokens.issue(unlinkedDriverUserId, tenantId, "DRIVER", "sinficha@andes.pe");

    UUID tripId = UUID.randomUUID();
    contextDoubles.scheduleTrip(
        tenantId, tripId, "Lima", "Arequipa", "Repuestos", LocalDate.now().plusDays(5));
    contextDoubles.assignTrip(tripId, UUID.randomUUID(), fleetDriverId);

    var settlementApi = new SettlementApi(mockMvc, objectMapper);
    settlementApi
        .registerAdvance(adminToken, tripId.toString(), new BigDecimal("250.00"), "PEN", "CASH")
        .andExpect(status().isCreated());

    contextDoubles.startTrip(tripId, Instant.now());
    contextDoubles.finishTrip(tripId, Instant.now());
    messaging.receive(
        new SettlementTripMessaging.Event(UUID.randomUUID(), tenantId, tripId, fleetDriverId));

    String settlementBody =
        settlementApi
            .getByTrip(driverToken, tripId.toString())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tripId").value(tripId.toString()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String settlementId = objectMapper.readTree(settlementBody).get("id").asText();

    settlementApi
        .getById(driverToken, settlementId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tripId").value(tripId.toString()));
    settlementApi
        .listAdvances(driverToken, tripId.toString())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    settlementApi
        .getByTrip(opsToken, tripId.toString())
        .andExpect(status().isOk());
    settlementApi
        .getById(opsToken, settlementId)
        .andExpect(status().isOk());
    settlementApi
        .listAdvances(opsToken, tripId.toString())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    settlementApi
        .getByTrip(otherDriverToken, tripId.toString())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("El viaje no existe."));
    settlementApi
        .getById(otherDriverToken, settlementId)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("La liquidación no existe."));
    settlementApi
        .listAdvances(otherDriverToken, tripId.toString())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("El viaje no existe."));

    settlementApi
        .getByTrip(unlinkedDriverToken, tripId.toString())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("El viaje no existe."));
    settlementApi
        .getById(unlinkedDriverToken, settlementId)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("La liquidación no existe."));
    settlementApi
        .listAdvances(unlinkedDriverToken, tripId.toString())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("El viaje no existe."));

    settlementApi
        .export(driverToken, settlementId)
        .andExpect(status().isForbidden());
    settlementApi
        .export(opsToken, settlementId)
        .andExpect(status().isOk());
  }
}
