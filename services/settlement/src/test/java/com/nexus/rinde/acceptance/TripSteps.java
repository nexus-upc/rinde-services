package com.nexus.rinde.acceptance;

import com.nexus.rinde.settlement.infrastructure.messaging.SettlementTripMessaging;
import com.nexus.rinde.support.ExternalStubs;
import io.cucumber.java.es.Dado;
import java.util.UUID;

/**
 * Pasos de viajes en el contexto del servicio Settlement. Los vehículos no se gestionan aquí;
 * programar y asignar un viaje registran su id en el contexto y su estado (ASSIGNED) en el stub de
 * Trip. "Finalizar" dispara la creación del borrador de liquidación llamando directamente al
 * consumidor de mensajería.
 */
public class TripSteps {

  private final ScenarioContext context;
  private final SettlementTripMessaging settlementTripMessaging;
  private final ExternalStubs externalStubs;

  public TripSteps(
      ScenarioContext context,
      SettlementTripMessaging settlementTripMessaging,
      ExternalStubs externalStubs) {
    this.context = context;
    this.settlementTripMessaging = settlementTripMessaging;
    this.externalStubs = externalStubs;
  }

  @Dado("que existe el vehículo {string}")
  public void vehicleExists(String vehicleName) {
    // no-op: los vehículos no se gestionan en este servicio
  }

  @Dado(
      "que el administrador programa el viaje {string} de {string} a {string} con carga {string} y salida {string}")
  public void tripIsScheduled(
      String tripName, String origin, String destination, String cargo, String departure) {
    UUID tripId = UUID.randomUUID();
    context.rememberTripId(tripName, tripId.toString());
    externalStubs.setTripStatus(context.currentTenantId(), tripId, "SCHEDULED");
  }

  @Dado("que el administrador asigna el viaje {string} al vehículo {string} y al conductor {string}")
  public void tripIsAssigned(String tripName, String vehicleName, String driverEmail) {
    context.rememberTripDriver(tripName, driverEmail);
    externalStubs.setTripStatus(
        context.currentTenantId(), UUID.fromString(context.tripIdOf(tripName)), "ASSIGNED");
  }

  @Dado("que el conductor inicia el viaje {string}")
  public void tripIsStarted(String tripName) {
    externalStubs.setTripStatus(
        context.currentTenantId(), UUID.fromString(context.tripIdOf(tripName)), "IN_ROUTE");
  }

  @Dado("que el conductor {string} finaliza el viaje {string}")
  public void tripIsFinished(String driverEmail, String tripName) {
    UUID tripId = UUID.fromString(context.tripIdOf(tripName));
    UUID driverId = context.userIdOf(driverEmail);
    UUID tenantId = context.currentTenantId();
    externalStubs.setTripStatus(tenantId, tripId, "FINISHED");
    settlementTripMessaging.receive(
        new SettlementTripMessaging.Event(UUID.randomUUID(), tenantId, tripId, driverId));
  }
}
