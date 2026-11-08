package com.nexus.rinde.fleet.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.fleet.domain.model.aggregates.Vehicle;
import com.nexus.rinde.fleet.domain.model.valueobjects.VehicleStatus;
import com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories.VehicleRepository;
import com.nexus.rinde.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Consumidor de trip.started y trip.finished: un evento repetido por eventId no cambia la unidad. */
class FleetTripMessagingIntegrationTest extends AbstractIntegrationTest {

  private final UUID tenantId = UUID.randomUUID();

  @Autowired private FleetTripMessaging messaging;
  @Autowired private VehicleRepository vehicles;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void aRepeatedTripStartedWithTheSameEventIdChangesTheUnitOnce() {
    UUID vehicleId = availableVehicle();
    UUID startedEvent = UUID.randomUUID();

    receive(startedEvent, vehicleId, "trip.started");
    receive(startedEvent, vehicleId, "trip.started");

    assertThat(statusOf(vehicleId)).isEqualTo(VehicleStatus.IN_TRIP);
    assertThat(processedCount(startedEvent)).isEqualTo(1L);
  }

  @Test
  void tripFinishedReturnsTheUnitToAvailable() {
    UUID vehicleId = availableVehicle();
    receive(UUID.randomUUID(), vehicleId, "trip.started");

    receive(UUID.randomUUID(), vehicleId, "trip.finished");

    assertThat(statusOf(vehicleId)).isEqualTo(VehicleStatus.AVAILABLE);
  }

  @Test
  void aRepeatedTripStartedDoesNotOccupyTheUnitAgainAfterItWasReleased() {
    UUID vehicleId = availableVehicle();
    UUID startedEvent = UUID.randomUUID();
    receive(startedEvent, vehicleId, "trip.started");
    receive(UUID.randomUUID(), vehicleId, "trip.finished");

    receive(startedEvent, vehicleId, "trip.started");

    assertThat(statusOf(vehicleId)).isEqualTo(VehicleStatus.AVAILABLE);
    assertThat(processedCount(startedEvent)).isEqualTo(1L);
  }

  private UUID availableVehicle() {
    Vehicle vehicle = vehicles.saveAndFlush(
        new Vehicle(tenantId, "TRP-123", "Volvo", "FH", 2022, new BigDecimal("15000.00")));
    return vehicle.getId();
  }

  private void receive(UUID eventId, UUID vehicleId, String routingKey) {
    MessageProperties properties = new MessageProperties();
    properties.setReceivedRoutingKey(routingKey);
    messaging.receive(
        new FleetTripMessaging.Event(eventId, tenantId, vehicleId),
        new Message(new byte[0], properties));
  }

  private VehicleStatus statusOf(UUID vehicleId) {
    return vehicles.findById(vehicleId).orElseThrow().getStatus();
  }

  private long processedCount(UUID eventId) {
    return jdbc.queryForObject(
        "select count(*) from fleet.processed_trip_events where event_id = ?", Long.class, eventId);
  }
}
