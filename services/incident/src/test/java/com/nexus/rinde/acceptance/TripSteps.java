package com.nexus.rinde.acceptance;

import com.nexus.rinde.support.FleetStub;
import com.nexus.rinde.support.TripStub;
import io.cucumber.java.es.Dado;
import java.util.UUID;

/** Viajes, vehículos y asignaciones simulados con TripStub y FleetStub. */
public class TripSteps {

  private final ScenarioContext context;
  private final TripStub tripStub;
  private final FleetStub fleetStub;

  public TripSteps(ScenarioContext context, TripStub tripStub, FleetStub fleetStub) {
    this.context = context;
    this.tripStub = tripStub;
    this.fleetStub = fleetStub;
  }

  @Dado("que existe el vehículo {string}")
  public void aVehicleExists(String vehicleName) {
    context.rememberVehicle(vehicleName, UUID.randomUUID());
  }

  @Dado(
      "que el administrador programa el viaje {string} de {string} a {string} con carga {string} y salida {string}")
  public void administratorSchedulesTrip(
      String tripName, String origin, String destination, String cargo, String departureDate) {
    UUID tripId = UUID.randomUUID();
    context.rememberTrip(tripName, tripId, null, null);
    tripStub.setTrip(context.currentTenantId(), tripId, "SCHEDULED", null, null);
  }

  @Dado("que el administrador asigna el viaje {string} al vehículo {string} y al conductor {string}")
  public void administratorAssignsTrip(String tripName, String vehicleName, String driverEmail) {
    UUID tripId = context.tripIdOf(tripName);
    UUID vehicleId = context.vehicleIdOf(vehicleName);
    UUID driverId = context.driverIdOf(driverEmail);
    context.rememberTrip(tripName, tripId, vehicleId, driverId);
    fleetStub.setDriverForUser(context.currentTenantId(), context.userIdOf(driverEmail), driverId);
    tripStub.setTrip(context.currentTenantId(), tripId, "ASSIGNED", vehicleId, driverId);
  }

  @Dado("que el conductor inicia el viaje {string}")
  public void driverStartsTrip(String tripName) {
    setTripStatus(tripName, "IN_ROUTE");
  }

  @Dado("que el conductor finaliza el viaje {string}")
  public void driverFinishesTrip(String tripName) {
    setTripStatus(tripName, "FINISHED");
  }

  @Dado("que existe otro viaje {string} en la otra empresa")
  public void anotherTripExists(String tripName) {
    UUID otherTenantId = context.tenantIdOf("20456789012");
    UUID tripId = UUID.randomUUID();
    UUID vehicleId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    context.rememberTrip(tripName, tripId, vehicleId, driverId);
    tripStub.setTrip(otherTenantId, tripId, "IN_ROUTE", vehicleId, driverId);
  }

  private void setTripStatus(String tripName, String status) {
    tripStub.setTrip(
        context.currentTenantId(),
        context.tripIdOf(tripName),
        status,
        context.tripVehicleIdOf(tripName),
        context.tripDriverIdOf(tripName));
  }
}
