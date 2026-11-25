package com.nexus.rinde.acceptance;

import com.nexus.rinde.expense.infrastructure.messaging.ExpenseSettlementMessaging;
import com.nexus.rinde.support.TripStub;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.time.Instant;
import java.util.UUID;

/**
 * Pasos relacionados a viajes simulados en las pruebas de Expense a través de TripStub y mensajería AMQP.
 */
public class TripSteps {

  private final ScenarioContext context;
  private final TripStub tripStub;
  private final ExpenseSettlementMessaging settlementMessaging;

  public TripSteps(
      ScenarioContext context,
      TripStub tripStub,
      ExpenseSettlementMessaging settlementMessaging) {
    this.context = context;
    this.tripStub = tripStub;
    this.settlementMessaging = settlementMessaging;
  }

  @Dado("que existe el vehículo {string}")
  public void aVehicleExists(String vehicleName) {
    // Contexto previo de fleet
  }

  @Dado(
      "que el administrador programa el viaje {string} de {string} a {string} con carga {string} y salida {string}")
  public void administratorSchedulesTrip(
      String tripName, String origin, String destination, String cargo, String departureDate) {
    UUID tripId = UUID.randomUUID();
    context.rememberTripId(tripName, tripId.toString());
    UUID tenantId = context.currentTenantId();
    tripStub.setTripStatus(tenantId, tripId, "SCHEDULED");
  }

  @Dado("que el administrador asigna el viaje {string} al vehículo {string} y al conductor {string}")
  public void administratorAssignsTrip(String tripName, String vehicleName, String driverEmail) {
    UUID tripId = UUID.fromString(context.tripIdOf(tripName));
    UUID tenantId = context.currentTenantId();
    UUID driverId = context.driverIdOf(driverEmail);
    context.rememberTripDriverId(tripName, driverId);
    tripStub.setTrip(tenantId, tripId, "ASSIGNED", driverId);
  }

  @Dado("que el conductor inicia el viaje {string}")
  public void driverStartsTrip(String tripName) {
    UUID tripId = UUID.fromString(context.tripIdOf(tripName));
    UUID tenantId = context.currentTenantId();
    tripStub.setTrip(tenantId, tripId, "IN_ROUTE", context.tripDriverIdOf(tripName));
  }

  @Dado("que el conductor {string} inicia el viaje {string}")
  public void namedDriverStartsTrip(String driverEmail, String tripName) {
    UUID tripId = UUID.fromString(context.tripIdOf(tripName));
    UUID tenantId = context.currentTenantId();
    UUID driverId = context.driverIdOf(driverEmail);
    context.rememberTripDriverId(tripName, driverId);
    tripStub.setTrip(tenantId, tripId, "IN_ROUTE", driverId);
  }

  @Dado("que el conductor finaliza el viaje {string}")
  public void driverFinishesTrip(String tripName) {
    UUID tripId = UUID.fromString(context.tripIdOf(tripName));
    UUID tenantId = context.currentTenantId();
    tripStub.setTrip(tenantId, tripId, "FINISHED", context.tripDriverIdOf(tripName));
  }

  @Dado("que el conductor {string} finaliza el viaje {string}")
  public void namedDriverFinishesTrip(String driverEmail, String tripName) {
    UUID tripId = UUID.fromString(context.tripIdOf(tripName));
    UUID tenantId = context.currentTenantId();
    UUID driverId = context.driverIdOf(driverEmail);
    context.rememberTripDriverId(tripName, driverId);
    tripStub.setTrip(tenantId, tripId, "FINISHED", driverId);
  }

  @Dado("que el administrador cierra la liquidación del viaje {string}")
  public void administratorClosesSettlement(String tripName) {
    closeSettlementInternal(tripName, context.currentTenantId());
  }

  @Dado("que la liquidación del viaje {string} de la empresa con RUC {string} fue cerrada")
  public void settlementWasClosedForTenant(String tripName, String ruc) {
    closeSettlementInternal(tripName, context.tenantIdOf(ruc));
  }

  private void closeSettlementInternal(String tripName, UUID tenantId) {
    UUID tripId = UUID.fromString(context.tripIdOf(tripName));
    UUID adminId = context.userIdOf("ana@andes.pe");
    if (adminId == null) {
      adminId = UUID.randomUUID();
    }
    tripStub.setTrip(tenantId, tripId, "SETTLED", context.tripDriverIdOf(tripName));
    settlementMessaging.receive(
        new ExpenseSettlementMessaging.SettlementClosedEvent(
            UUID.randomUUID(), Instant.now(), tenantId, tripId, adminId),
        null);
  }

  @Dado("que existe otro viaje {string} en la otra empresa")
  public void anotherTripExists(String tripName) {
    UUID tripId = UUID.randomUUID();
    context.rememberTripId(tripName, tripId.toString());
    UUID otherTenantId = context.tenantIdOf("20987654321");
    if (otherTenantId == null) {
      otherTenantId = context.currentTenantId();
    }
    tripStub.setTripStatus(otherTenantId, tripId, "IN_ROUTE");
  }
}
