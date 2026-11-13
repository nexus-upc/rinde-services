package com.nexus.rinde.acceptance;

import com.nexus.rinde.settlement.infrastructure.messaging.SettlementTripMessaging;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.util.UUID;

/**
 * Pasos de viajes en el contexto del servicio Settlement. Las operaciones sobre viajes (crear,
 * asignar, iniciar) son no-ops aquí; solo "finalizar" importa porque dispara la creación del
 * borrador de liquidación llamando directamente al consumidor de mensajería.
 */
public class TripSteps {

  private final ScenarioContext context;
  private final SettlementTripMessaging settlementTripMessaging;

  public TripSteps(ScenarioContext context, SettlementTripMessaging settlementTripMessaging) {
    this.context = context;
    this.settlementTripMessaging = settlementTripMessaging;
  }

  @Dado("que existe el vehículo {string}")
  public void vehicleExists(String vehicleName) {
    // no-op: los vehículos no se gestionan en este servicio
  }

  @Dado("que el administrador programa el viaje {string} de {string} a {string} con carga {string} y salida {string}")
  public void tripIsScheduled(String tripName, String origin, String destination, String cargo, String departure) {
    context.rememberTripId(tripName, UUID.randomUUID().toString());
  }

  @Dado("que el administrador asigna el viaje {string} al vehículo {string} y al conductor {string}")
  public void tripIsAssigned(String tripName, String vehicleName, String driverEmail) {
    context.rememberTripDriver(tripName, driverEmail);
  }

  @Dado("que el conductor inicia el viaje {string}")
  public void tripIsStarted(String tripName) {
    // no-op: el inicio del viaje no genera liquidación
  }

  @Dado("que el conductor {string} finaliza el viaje {string}")
  public void tripIsFinished(String driverEmail, String tripName) {
    String tripId = context.tripIdOf(tripName);
    UUID driverId = context.userIdOf(driverEmail);
    UUID tenantId = context.currentTenantId();
    settlementTripMessaging.receive(
        new SettlementTripMessaging.Event(UUID.randomUUID(), tenantId, UUID.fromString(tripId), driverId));
  }

  @Y("que el conductor {string} finaliza el viaje {string}")
  public void tripIsFinishedY(String driverEmail, String tripName) {
    tripIsFinished(driverEmail, tripName);
  }
}
