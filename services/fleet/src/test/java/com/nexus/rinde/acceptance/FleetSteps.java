package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.fleet.infrastructure.messaging.FleetSubscriptionMessaging;
import com.nexus.rinde.fleet.interfaces.acl.PlanChanged;
import com.nexus.rinde.support.FleetApi;
import com.nexus.rinde.support.RelativeDates;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.springframework.test.web.servlet.MockMvc;

/** Pasos de aceptación BDD para vehículos, conductores y mantenimiento (Sprint 1). */
public class FleetSteps {

  private final ScenarioContext context;
  private final FleetApi api;
  private final FleetSubscriptionMessaging planChanges;

  public FleetSteps(
      ScenarioContext context,
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      FleetSubscriptionMessaging planChanges) {
    this.context = context;
    this.api = new FleetApi(mockMvc, objectMapper);
    this.planChanges = planChanges;
  }

  // --- PLAN ---

  /** El cambio llega por el mismo consumidor que recibe subscription.plan-changed en producción. */
  @Dado("que la empresa con RUC {string} tiene un plan con límite de {int} unidades")
  public void theCompanyPlanHasUnitLimit(String ruc, int unitLimit) {
    planChanges.receive(
        new PlanChanged(
            UUID.randomUUID(),
            Instant.now(),
            context.tenantIdOf(ruc),
            UUID.randomUUID(),
            unitLimit));
  }

  // --- VEHÍCULOS ---

  @Dado(
      "que el administrador registró el vehículo {string} con placa {string}, marca {string}, modelo {string}, año {int} y capacidad {string}")
  public void aVehicleWasRegistered(
      String name,
      String plateNumber,
      String brand,
      String model,
      int year,
      String capacity)
      throws Exception {
    String token = context.currentAdministratorToken();
    var res =
        api.registerVehicle(
            token, plateNumber, brand, model, year, new BigDecimal(capacity));
    context.record(res);
    String id = context.body().get("id").asText();
    context.rememberVehicleId(name, id);
  }

  @Cuando(
      "el administrador registra el vehículo {string} con placa {string}, marca {string}, modelo {string}, año {int} y capacidad {string}")
  public void theAdministratorRegistersAVehicle(
      String name,
      String plateNumber,
      String brand,
      String model,
      int year,
      String capacity)
      throws Exception {
    String token = context.currentAdministratorToken();
    var res =
        api.registerVehicle(
            token, plateNumber, brand, model, year, new BigDecimal(capacity));
    context.record(res);
    if (context.statusCode() == 201) {
      String id = context.body().get("id").asText();
      context.rememberVehicleId(name, id);
    }
  }

  @Cuando("el administrador intenta registrar un vehículo con capacidad {string}")
  public void tryRegisterWithInvalidCapacity(String capacity) throws Exception {
    String token = context.currentAdministratorToken();
    Map<String, Object> body = new HashMap<>();
    body.put("plateNumber", "ABC-999");
    body.put("brand", "Volvo");
    body.put("model", "FH");
    body.put("modelYear", 2022);
    body.put("payloadCapacityKg", new BigDecimal(capacity));
    context.record(api.registerVehicleRaw(token, body));
  }

  @Cuando("el administrador intenta registrar un vehículo sin placa")
  public void tryRegisterWithoutPlate() throws Exception {
    String token = context.currentAdministratorToken();
    Map<String, Object> body = new HashMap<>();
    body.put("plateNumber", "");
    body.put("brand", "Volvo");
    body.put("model", "FH");
    body.put("modelYear", 2022);
    body.put("payloadCapacityKg", new BigDecimal("5000"));
    context.record(api.registerVehicleRaw(token, body));
  }

  @Cuando("el conductor {string} intenta registrar un vehículo con placa {string}")
  public void aDriverTriesToRegisterAVehicle(String driverEmail, String plate) throws Exception {
    String token = context.accessTokenOf(driverEmail);
    var res =
        api.registerVehicle(
            token, plate, "Volvo", "FH", 2022, new BigDecimal("5000"));
    context.record(res);
  }

  @Cuando("el administrador lista los vehículos de su empresa")
  public void listVehicles() throws Exception {
    String token = context.currentAdministratorToken();
    context.record(api.listVehicles(token));
  }

  @Cuando("el administrador consulta el vehículo {string}")
  public void getVehicle(String name) throws Exception {
    String token = context.currentAdministratorToken();
    String vehicleId = context.vehicleIdOf(name);
    context.record(api.getVehicle(token, vehicleId));
  }

  @Cuando("el administrador consulta el estado de salud del vehículo {string}")
  public void getHealthStatus(String name) throws Exception {
    String token = context.currentAdministratorToken();
    String vehicleId = context.vehicleIdOf(name);
    context.record(api.getVehicleHealthStatus(token, vehicleId));
  }

  @Cuando(
      "un administrador de otra empresa consulta el estado de salud del vehículo {string}")
  public void otherAdminConsultsVehicleHealth(String name) throws Exception {
    String token = context.currentAdministratorToken();
    String vehicleId = context.vehicleIdOf(name);
    context.record(api.getVehicleHealthStatus(token, vehicleId));
  }

  // --- CONDUCTORES ---

  @Dado(
      "que el administrador registró el conductor {string} con documento {string} {string}, licencia {string} categoría {string} y vencimiento {string}")
  public void aDriverWasRegistered(
      String name,
      String docType,
      String docNumber,
      String licNumber,
      String category,
      String expDate)
      throws Exception {
    String token = context.currentAdministratorToken();
    var res =
        api.registerDriver(
            token,
            null,
            name,
            docType,
            docNumber,
            licNumber,
            category,
            RelativeDates.date(expDate));
    context.record(res);
    String id = context.body().get("id").asText();
    context.rememberDriverId(name, id);
  }

  @Cuando(
      "el administrador registra el conductor {string} con documento {string} {string}, licencia {string} categoría {string} y vencimiento {string}")
  public void registerDriver(
      String name,
      String docType,
      String docNumber,
      String licNumber,
      String category,
      String expDate)
      throws Exception {
    String token = context.currentAdministratorToken();
    var res =
        api.registerDriver(
            token,
            null,
            name,
            docType,
            docNumber,
            licNumber,
            category,
            RelativeDates.date(expDate));
    context.record(res);
    if (context.statusCode() == 201) {
      String id = context.body().get("id").asText();
      context.rememberDriverId(name, id);
    }
  }

  @Dado(
      "que el administrador registró el conductor {string} con documento {string} {string}, licencia {string} categoría {string} y vencimiento {string} vinculado al usuario {string}")
  public void aDriverWasRegisteredForUser(
      String name,
      String docType,
      String docNumber,
      String licNumber,
      String category,
      String expDate,
      String email)
      throws Exception {
    String token = context.currentAdministratorToken();
    var res =
        api.registerDriver(
            token,
            context.userIdOf(email),
            name,
            docType,
            docNumber,
            licNumber,
            category,
            RelativeDates.date(expDate));
    context.record(res);
    assertThat(context.statusCode()).isEqualTo(201);
    context.rememberDriverId(name, context.body().get("id").asText());
  }

  @Cuando("el conductor {string} intenta registrar un conductor con licencia {string}")
  public void driverTriesToRegisterDriver(String driverEmail, String lic) throws Exception {
    String token = context.accessTokenOf(driverEmail);
    var res =
        api.registerDriver(
            token,
            null,
            "Otro Chofer",
            "DNI",
            "44556677",
            lic,
            "A-IIb",
            LocalDate.now().plusYears(2));
    context.record(res);
  }

  @Cuando("el administrador lista los conductores de su empresa")
  public void listDrivers() throws Exception {
    String token = context.currentAdministratorToken();
    context.record(api.listDrivers(token));
  }

  @Cuando("el administrador consulta el conductor {string}")
  public void getDriver(String name) throws Exception {
    String token = context.currentAdministratorToken();
    String driverId = context.driverIdOf(name);
    context.record(api.getDriver(token, driverId));
  }

  @Cuando("el administrador consulta la elegibilidad del conductor {string}")
  public void checkEligibility(String name) throws Exception {
    String token = context.currentAdministratorToken();
    String driverId = context.driverIdOf(name);
    context.record(api.checkDriverEligibility(token, driverId));
  }

  // --- MANTENIMIENTOS Y ALERTAS ---

  @Dado(
      "que se registra un mantenimiento {string} para el vehículo {string} con ejecución {string}, costo {string} y próximo mantenimiento {string}")
  public void aMaintenanceWasRecorded(
      String type,
      String vehicleName,
      String execDate,
      String cost,
      String nextDate)
      throws Exception {
    String token = context.currentAdministratorToken();
    String vehicleId = context.vehicleIdOf(vehicleName);
    var res =
        api.recordMaintenance(
            token,
            vehicleId,
            type,
            RelativeDates.date(execDate),
            25000,
            new BigDecimal(cost),
            RelativeDates.date(nextDate),
            "Mantenimiento programado");
    context.record(res);
  }

  @Cuando(
      "el administrador registra un mantenimiento {string} para el vehículo {string} con ejecución {string}, costo {string} y próximo mantenimiento {string}")
  public void adminRecordsMaintenance(
      String type,
      String vehicleName,
      String execDate,
      String cost,
      String nextDate)
      throws Exception {
    String token = context.currentAdministratorToken();
    String vehicleId = context.vehicleIdOf(vehicleName);
    var res =
        api.recordMaintenance(
            token,
            vehicleId,
            type,
            RelativeDates.date(execDate),
            25000,
            new BigDecimal(cost),
            RelativeDates.date(nextDate),
            "Servicio de taller");
    context.record(res);
  }

  @Cuando("el administrador consulta las alertas de mantenimiento")
  public void getAlerts() throws Exception {
    String token = context.currentAdministratorToken();
    context.record(api.listMaintenanceAlerts(token));
  }

  // --- VERIFICACIONES ESPECÍFICAS ---

  @Y("la lista contiene {int} vehículos")
  public void listContainsVehicles(int count) throws Exception {
    assertThat(context.body().isArray()).isTrue();
    assertThat(context.body().size()).isEqualTo(count);
  }

  @Y("la lista incluye el vehículo con placa {string}")
  public void listIncludesVehicleWithPlate(String plate) throws Exception {
    assertThat(context.body().isArray()).isTrue();
    boolean found =
        StreamSupport.stream(context.body().spliterator(), false)
            .anyMatch(node -> plate.equalsIgnoreCase(node.get("plateNumber").asText()));
    assertThat(found).isTrue();
  }

  @Y("la lista no incluye el vehículo con placa {string}")
  public void listNotIncludesVehicleWithPlate(String plate) throws Exception {
    assertThat(context.body().isArray()).isTrue();
    boolean found =
        StreamSupport.stream(context.body().spliterator(), false)
            .anyMatch(node -> plate.equalsIgnoreCase(node.get("plateNumber").asText()));
    assertThat(found).isFalse();
  }

  @Y("la lista contiene {int} conductores")
  public void listContainsDrivers(int count) throws Exception {
    assertThat(context.body().isArray()).isTrue();
    assertThat(context.body().size()).isEqualTo(count);
  }

  @Y("la lista incluye el conductor con documento {string}")
  public void listIncludesDriverWithDoc(String doc) throws Exception {
    assertThat(context.body().isArray()).isTrue();
    boolean found =
        StreamSupport.stream(context.body().spliterator(), false)
            .anyMatch(node -> doc.equals(node.get("documentNumber").asText()));
    assertThat(found).isTrue();
  }

  @Y("la lista no incluye el conductor con documento {string}")
  public void listNotIncludesDriverWithDoc(String doc) throws Exception {
    assertThat(context.body().isArray()).isTrue();
    boolean found =
        StreamSupport.stream(context.body().spliterator(), false)
            .anyMatch(node -> doc.equals(node.get("documentNumber").asText()));
    assertThat(found).isFalse();
  }

  @Y("el conductor queda con el estado {string}")
  public void driverHasStatus(String status) throws Exception {
    assertThat(context.body().get("status").asText()).isEqualTo(status);
  }

  @Y("el conductor queda habilitado")
  public void driverIsEligible() throws Exception {
    assertThat(context.body().get("eligible").asBoolean()).isTrue();
  }

  @Y("el conductor no queda habilitado")
  public void driverIsNotEligible() throws Exception {
    assertThat(context.body().get("eligible").asBoolean()).isFalse();
  }

  @Y("el estado de mantenimiento es {string}")
  public void maintenanceStateIs(String state) throws Exception {
    assertThat(context.body().get("maintenanceState").asText()).isEqualTo(state);
  }

  @Y("el vehículo queda con el estado {string}")
  public void vehicleHasStatus(String status) throws Exception {
    assertThat(context.body().get("status").asText()).isEqualTo(status);
  }

  @Y("el vehículo está disponible para viaje")
  public void vehicleIsAvailableForTrip() throws Exception {
    assertThat(context.body().get("availableForTrip").asBoolean()).isTrue();
  }

  @Y("el vehículo no está disponible para viaje")
  public void vehicleIsNotAvailableForTrip() throws Exception {
    assertThat(context.body().get("availableForTrip").asBoolean()).isFalse();
  }

  @Y("la lista de alertas contiene al menos {int} elemento")
  public void alertListContainsAtLeast(int min) throws Exception {
    assertThat(context.body().isArray()).isTrue();
    assertThat(context.body().size()).isGreaterThanOrEqualTo(min);
  }

  @Y("la lista de alertas incluye la unidad con placa {string}")
  public void alertListIncludesPlate(String plate) throws Exception {
    assertThat(context.body().isArray()).isTrue();
    boolean found =
        StreamSupport.stream(context.body().spliterator(), false)
            .anyMatch(node -> plate.equalsIgnoreCase(node.get("plateNumber").asText()));
    assertThat(found).isTrue();
  }
}
