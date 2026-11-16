package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.notification.application.events.TripAssignedNotification;
import com.nexus.rinde.notification.infrastructure.messaging.TripAssignedListener;
import com.nexus.rinde.settlement.infrastructure.messaging.SettlementTripMessaging;
import com.nexus.rinde.support.ContextDoubles;
import com.nexus.rinde.support.ContextDoubles.Account;
import com.nexus.rinde.support.ExpenseDouble;
import com.nexus.rinde.support.RelativeDates;
import com.nexus.rinde.support.TenantAccess;
import com.nexus.rinde.support.TestTokens;
import com.nexus.rinde.trip.interfaces.acl.TripSummary;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Preparación de los escenarios con los contextos separados. Empresas, cuentas, flota, viajes y
 * gastos se cargan en ContextDoubles. La liquidación y la notificación se crean con los mismos
 * consumidores que reciben trip.finished y trip.assigned por RabbitMQ.
 */
public class SetupSteps {

  private final ScenarioContext context;
  private final ContextDoubles doubles;
  private final ExpenseDouble expenses;
  private final TestTokens tokens;
  private final SettlementTripMessaging settlementMessaging;
  private final TripAssignedListener tripAssignedListener;

  public SetupSteps(
      ScenarioContext context,
      ContextDoubles doubles,
      ExpenseDouble expenses,
      TestTokens tokens,
      SettlementTripMessaging settlementMessaging,
      TripAssignedListener tripAssignedListener) {
    this.context = context;
    this.doubles = doubles;
    this.expenses = expenses;
    this.tokens = tokens;
    this.settlementMessaging = settlementMessaging;
    this.tripAssignedListener = tripAssignedListener;
  }

  // Empresas y cuentas

  @Dado("que existe una empresa verificada con RUC {string} y administrador {string}")
  public void aVerifiedTenantExists(String ruc, String administratorEmail) {
    String tenantId = access().registerAndVerifyTenant(administratorEmail);
    context.rememberTenantId(ruc, tenantId);
    context.setCurrentTenantId(tenantId);
    rememberUser(administratorEmail);
  }

  @Dado("que el administrador {string} inició sesión")
  public void theAdministratorSignsIn(String email) {
    String token = access().accessTokenFor(email);
    context.rememberAccessToken(email, token);
    context.setCurrentAdministratorToken(token);
    context.setCurrentTenantId(doubles.accountByEmail(email).orElseThrow().tenantId().toString());
  }

  @Dado("que el conductor {string} inició sesión")
  public void theDriverSignsIn(String email) {
    context.rememberAccessToken(email, access().accessTokenFor(email));
  }

  @Dado("que el administrador invitó a {string} con el correo {string} y el rol {string}")
  public void theAdministratorInvitesUser(String fullName, String email, String role) {
    UUID userId = UUID.randomUUID();
    doubles.addAccount(new Account(userId, currentTenant(), role, email));
    context.rememberUserId(email, userId.toString());
  }

  /** Al definir la contraseña la cuenta queda activa y el usuario puede usar su token. */
  @Dado("que {string} definió la contraseña {string} con su enlace de invitación")
  public void theInvitedUserDefinesPassword(String email, String password) {
    context.rememberAccessToken(email, access().accessTokenFor(email));
  }

  // Flota y conductores

  @Dado("que existe el vehículo {string}")
  public void aVehicleExists(String name) {
    registerVehicle(name, "PLT-" + UUID.randomUUID().toString().substring(0, 6), "Volvo", "FH");
  }

  @Dado(
      "que el administrador registró el vehículo {string} con placa {string}, marca {string},"
          + " modelo {string}, año {int} y capacidad {string}")
  public void theAdministratorRegistersVehicle(
      String name, String plate, String brand, String model, int year, String capacity) {
    registerVehicle(name, plate, brand, model);
  }

  @Dado(
      "que el administrador registró el conductor {string} con documento {string} {string},"
          + " licencia {string} categoría {string} y vencimiento {string}")
  public void theAdministratorRegistersDriver(
      String fullName,
      String documentType,
      String documentNumber,
      String license,
      String category,
      String expiry) {
    UUID driverId = UUID.randomUUID();
    doubles.addDriver(currentTenant(), driverId, fullName, license, category);
    context.rememberDriverId(fullName, driverId.toString());
  }

  // Viajes

  @Dado(
      "que el administrador programa el viaje {string} de {string} a {string} con carga {string}"
          + " y salida {string}")
  public void theAdministratorSchedulesTrip(
      String name, String origin, String destination, String cargo, String departure) {
    scheduleTrip(name, origin, destination, cargo, departure);
  }

  @Cuando(
      "el administrador programa el viaje {string} de {string} a {string} con carga {string}"
          + " y salida {string}")
  public void theAdministratorSchedulesTripInAction(
      String name, String origin, String destination, String cargo, String departure) {
    scheduleTrip(name, origin, destination, cargo, departure);
  }

  @Dado("que el administrador asigna el viaje {string} al vehículo {string} y al conductor {string}")
  public void theAdministratorAssignsTrip(String tripName, String vehicleName, String driverEmail) {
    assignTrip(tripName, vehicleName, driverEmail);
  }

  @Cuando("el administrador asigna el viaje {string} al vehículo {string} y al conductor {string}")
  public void theAdministratorAssignsTripInAction(
      String tripName, String vehicleName, String driverEmail) {
    assignTrip(tripName, vehicleName, driverEmail);
  }

  @Dado("que el conductor inicia el viaje {string}")
  public void theDriverStartsTrip(String tripName) {
    doubles.startTrip(tripId(tripName), Instant.now());
  }

  /** Finalizar el viaje publica trip.finished y el consumidor de Settlement abre la liquidación. */
  @Dado("que el conductor {string} finaliza el viaje {string}")
  public void theDriverFinishesTrip(String driverEmail, String tripName) {
    UUID tripId = tripId(tripName);
    doubles.finishTrip(tripId, Instant.now());
    TripSummary trip = doubles.trip(tripId).orElseThrow();
    assertThat(trip.driverId()).isEqualTo(UUID.fromString(context.userIdOf(driverEmail)));
    settlementMessaging.receive(
        new SettlementTripMessaging.Event(
            UUID.randomUUID(), doubles.tenantOfTrip(tripId), tripId, trip.driverId()));
  }

  // Gastos

  @Dado(
      "que el conductor registró un gasto {string} de tipo {string} por el monto {string}"
          + " con clave {string} en el viaje {string}")
  public void theDriverRegistersExpense(
      String name, String category, String amount, String idempotencyKey, String tripName) {
    UUID tripId = tripId(tripName);
    UUID expenseId = UUID.randomUUID();
    expenses.addExpense(
        doubles.tenantOfTrip(tripId),
        tripId,
        expenseId,
        category,
        new BigDecimal(amount),
        LocalDate.now(),
        "REGISTERED");
    context.rememberExpenseId(name, expenseId.toString());
  }

  @Dado("que el administrador {string} aprobó el gasto {string}")
  public void theAdministratorApprovesExpense(String email, String expenseName) {
    assertThat(doubles.accountByEmail(email).orElseThrow().role()).isEqualTo("ADMINISTRATOR");
    expenses.approveExpense(UUID.fromString(context.expenseIdOf(expenseName)));
  }

  @Dado("que el administrador {string} observó el gasto {string}")
  public void theAdministratorObservesExpense(String email, String expenseName) {
    assertThat(doubles.accountByEmail(email).orElseThrow().role()).isEqualTo("ADMINISTRATOR");
    expenses.changeStatus(UUID.fromString(context.expenseIdOf(expenseName)), "OBSERVED");
  }

  /** Un gasto aprobado con fecha propia, para probar el gasto del periodo. */
  @Dado("que existe un gasto aprobado de {string} soles con fecha {string} en el viaje {string}")
  public void anApprovedExpenseExists(String amount, String date, String tripName) {
    UUID tripId = tripId(tripName);
    expenses.addExpense(
        doubles.tenantOfTrip(tripId),
        tripId,
        UUID.randomUUID(),
        "FUEL",
        new BigDecimal(amount),
        RelativeDates.date(date),
        "APPROVED");
  }

  // Auxiliares

  private TenantAccess access() {
    return new TenantAccess(doubles, tokens);
  }

  private UUID currentTenant() {
    return UUID.fromString(context.currentTenantId());
  }

  private UUID tripId(String tripName) {
    return UUID.fromString(context.tripIdOf(tripName));
  }

  private void rememberUser(String email) {
    doubles
        .accountByEmail(email)
        .ifPresent(account -> context.rememberUserId(email, account.userId().toString()));
  }

  private void registerVehicle(String name, String plate, String brand, String model) {
    UUID vehicleId = UUID.randomUUID();
    doubles.addVehicle(currentTenant(), vehicleId, plate, brand, model);
    context.rememberVehicleId(name, vehicleId.toString());
  }

  private void scheduleTrip(
      String name, String origin, String destination, String cargo, String departure) {
    UUID tripId = UUID.randomUUID();
    doubles.scheduleTrip(
        currentTenant(), tripId, origin, destination, cargo, RelativeDates.date(departure));
    context.rememberTripId(name, tripId.toString());
  }

  /** La asignación se entrega al consumidor de Notifications, como lo haría el broker. */
  private void assignTrip(String tripName, String vehicleName, String driverEmail) {
    UUID tripId = tripId(tripName);
    UUID vehicleId = UUID.fromString(context.vehicleIdOf(vehicleName));
    UUID driverId = UUID.fromString(context.userIdOf(driverEmail));
    doubles.assignTrip(tripId, vehicleId, driverId);
    TripSummary trip = doubles.trip(tripId).orElseThrow();
    var assignment =
        new TripAssignedNotification(
            UUID.randomUUID(),
            Instant.now(),
            doubles.tenantOfTrip(tripId),
            tripId,
            trip.code(),
            trip.destination(),
            trip.departureDate(),
            vehicleId,
            driverId);
    context.rememberTripAssignment(assignment);
    tripAssignedListener.on(assignment);
  }
}
