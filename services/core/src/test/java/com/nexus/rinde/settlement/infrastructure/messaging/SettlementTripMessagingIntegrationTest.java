package com.nexus.rinde.settlement.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** trip.finished abre una liquidación; repetir el mismo eventId no crea otra. */
class SettlementTripMessagingIntegrationTest extends AbstractIntegrationTest {

  @Autowired private SettlementTripMessaging messaging;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void tripFinishedOpensOneSettlementAndIgnoresTheSameEventId() {
    UUID tenantId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    var event = new SettlementTripMessaging.Event(UUID.randomUUID(), tenantId, tripId, driverId);

    messaging.receive(event);
    messaging.receive(event);

    assertThat(countSettlements(tripId)).isEqualTo(1L);
    assertThat(jdbc.queryForObject(
            "select tenant_id from settlement.settlements where trip_id = ?", UUID.class, tripId))
        .isEqualTo(tenantId);
    assertThat(jdbc.queryForObject(
            "select driver_id from settlement.settlements where trip_id = ?", UUID.class, tripId))
        .isEqualTo(driverId);
    assertThat(jdbc.queryForObject(
            "select count(*) from settlement.processed_trip_events where event_id = ?",
            Long.class,
            event.eventId()))
        .isEqualTo(1L);
  }

  @Test
  void aDifferentEventForTheSameTripDoesNotOpenASecondSettlement() {
    UUID tenantId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();

    messaging.receive(new SettlementTripMessaging.Event(UUID.randomUUID(), tenantId, tripId, driverId));
    messaging.receive(new SettlementTripMessaging.Event(UUID.randomUUID(), tenantId, tripId, driverId));

    assertThat(countSettlements(tripId)).isEqualTo(1L);
  }

  private long countSettlements(UUID tripId) {
    return jdbc.queryForObject(
        "select count(*) from settlement.settlements where trip_id = ?", Long.class, tripId);
  }
}
