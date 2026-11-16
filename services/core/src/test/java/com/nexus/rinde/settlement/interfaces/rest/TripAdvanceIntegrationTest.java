package com.nexus.rinde.settlement.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.settlement.infrastructure.messaging.SettlementTripMessaging;
import com.nexus.rinde.support.AbstractIntegrationTest;
import com.nexus.rinde.support.SettlementApi;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Adelantos de viáticos: una sola moneda por viaje y suma en la liquidación al finalizar. */
class TripAdvanceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private SettlementTripMessaging messaging;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void aSecondAdvanceInAnotherCurrencyIsRejectedAndNotStored() throws Exception {
    String tenant = api.registerAndVerifyTenant("ana@andes.pe");
    String token = api.accessTokenFor("ana@andes.pe");
    UUID tripId = scheduleTrip(tenant);
    var advances = new SettlementApi(mockMvc, objectMapper);

    advances
        .registerAdvance(token, tripId.toString(), new BigDecimal("100.00"), "PEN", "CASH")
        .andExpect(status().isCreated());
    advances
        .registerAdvance(token, tripId.toString(), new BigDecimal("50.00"), "USD", "TRANSFER")
        .andExpect(status().isConflict());

    assertThat(
            jdbc.queryForObject(
                "select count(*) from settlement.trip_advances where trip_id = ?",
                Long.class,
                tripId))
        .isEqualTo(1L);
  }

  @Test
  void advancesAreIncorporatedIntoTheSettlementWhenTheTripFinishes() throws Exception {
    String tenant = api.registerAndVerifyTenant("ana@andes.pe");
    String token = api.accessTokenFor("ana@andes.pe");
    UUID tripId = scheduleTrip(tenant);
    UUID driverId = UUID.randomUUID();
    var advances = new SettlementApi(mockMvc, objectMapper);
    advances
        .registerAdvance(token, tripId.toString(), new BigDecimal("300.00"), "PEN", "CASH")
        .andExpect(status().isCreated());
    advances
        .registerAdvance(token, tripId.toString(), new BigDecimal("200.00"), "PEN", "TRANSFER")
        .andExpect(status().isCreated());

    contextDoubles.assignTrip(tripId, UUID.randomUUID(), driverId);
    contextDoubles.startTrip(tripId, Instant.now());
    contextDoubles.finishTrip(tripId, Instant.now());
    messaging.receive(
        new SettlementTripMessaging.Event(
            UUID.randomUUID(), UUID.fromString(tenant), tripId, driverId));

    assertThat(
            jdbc.queryForObject(
                "select advance_amount from settlement.settlements where trip_id = ?",
                BigDecimal.class,
                tripId))
        .isEqualByComparingTo("500.00");
    assertThat(
            jdbc.queryForObject(
                "select advance_currency from settlement.settlements where trip_id = ?",
                String.class,
                tripId))
        .isEqualTo("PEN");
  }

  /** Programa un viaje de la empresa en los dobles de Trip y devuelve su identificador. */
  private UUID scheduleTrip(String tenantId) {
    UUID tripId = UUID.randomUUID();
    contextDoubles.scheduleTrip(
        UUID.fromString(tenantId), tripId, "Lima", "Arequipa", "Repuestos", LocalDate.now().plusDays(5));
    return tripId;
  }
}
