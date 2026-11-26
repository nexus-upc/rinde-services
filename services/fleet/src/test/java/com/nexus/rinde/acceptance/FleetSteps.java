package com.nexus.rinde.acceptance;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.fleet.infrastructure.messaging.FleetSubscriptionMessaging;
import com.nexus.rinde.fleet.interfaces.acl.PlanChanged;
import com.nexus.rinde.support.FleetApi;
import com.nexus.rinde.support.RelativeDates;
import com.nexus.rinde.support.WireMockServices;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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

  @Cuando(
      "el administrador registra el conductor {string} con documento {string} {string}, licencia {string} categoría {string} y vencimiento {string} vinculado al usuario {string}")
  public void registerDriverLinkedToUser(
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
    if (context.statusCode() == 201) {
      context.rememberDriverId(name, context.body().get("id").asText());
    }
  }

  /** El actor (administrador, responsable o conductor) vincula la cuenta indicada al conductor. */
  @Cuando("{string} asigna la cuenta {string} al conductor {string}")
  public void actorAssignsAccountToDriver(String actorEmail, String accountEmail, String driverName)
      throws Exception {
    context.record(
        api.updateDriverAccount(
            context.accessTokenOf(actorEmail),
            context.driverIdOf(driverName),
            context.userIdOf(accountEmail)));
  }

  @Cuando("{string} retira la cuenta del conductor {string}")
  public void actorRemovesAccountFromDriver(String actorEmail, String driverName)
      throws Exception {
    context.record(
        api.updateDriverAccount(
            context.accessTokenOf(actorEmail), context.driverIdOf(driverName), null));
  }

  @Cuando("el conductor {string} consulta su ficha de conductor")
  public void driverConsultsOwnDriver(String email) throws Exception {
    context.record(api.getCurrentDriver(context.accessTokenOf(email)));
  }

  @Cuando("el administrador consulta su ficha de conductor")
  public void administratorConsultsOwnDriver() throws Exception {
    context.record(api.getCurrentDriver(context.currentAdministratorToken()));
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

  @Y("la respuesta vincula la cuenta de {string}")
  public void responseLinksAccountOf(String email) throws Exception {
    assertThat(context.body().get("userId").asText())
        .isEqualTo(context.userIdOf(email).toString());
  }

  @Y("la respuesta no tiene cuenta vinculada")
  public void responseHasNoLinkedAccount() throws Exception {
    assertThat(context.body().get("userId").isNull()).isTrue();
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

  // --- HISTORIAL DEL VEHÍCULO (S4-13) ---

  @Dado("que Trip responde el viaje {string} para el vehículo {string} en fecha {string}")
  public void tripRespondsWithTripForVehicle(String code, String vehicleName, String dateToken) {
    UUID tenantId = context.currentTenantId();
    String vehicleId = context.vehicleIdOf(vehicleName);
    String tripDate = RelativeDates.resolve(dateToken);
    String body =
        """
        [{"tripId":"%s","code":"%s","status":"FINISHED","origin":"Lima","destination":"Trujillo",\
        "cargoDescription":"Materiales de construcción","cargoWeightKg":12000,\
        "departureDate":"%s","vehicleId":"%s","driverId":"%s","startedAt":"%sT07:30:00Z"}]"""
            .formatted(UUID.randomUUID(), code, tripDate, vehicleId, UUID.randomUUID(), tripDate);
    WireMockServices.server()
        .stubFor(get(urlEqualTo("/api/internal/v1/trips/tenants/" + tenantId)).willReturn(okJson(body)));
  }

  @Dado("que Trip no responde para la empresa de {string}")
  public void tripFailsForTenant(String ruc) {
    UUID tenantId = context.tenantIdOf(ruc);
    WireMockServices.server()
        .stubFor(
            get(urlEqualTo("/api/internal/v1/trips/tenants/" + tenantId))
                .willReturn(aResponse().withStatus(503)));
  }

  @Dado("que Incident responde la incidencia {string} para el vehículo {string} en fecha {string}")
  public void incidentRespondsWithIncidentForVehicle(
      String type, String vehicleName, String dateToken) {
    UUID tenantId = context.currentTenantId();
    String vehicleId = context.vehicleIdOf(vehicleName);
    String incidentDate = RelativeDates.resolve(dateToken);
    String body =
        """
        [{"id":"%s","tripId":"%s","vehicleId":"%s","type":"%s",\
        "description":"Falla en el freno neumático","status":"CLOSED","reportedAt":"%sT13:20:00Z"}]"""
            .formatted(UUID.randomUUID(), UUID.randomUUID(), vehicleId, type, incidentDate);
    WireMockServices.server()
        .stubFor(
            get(urlPathEqualTo("/api/internal/v1/incidents/tenants/" + tenantId))
                .willReturn(okJson(body)));
  }

  @Dado("que Incident no tiene incidencias para el vehículo {string}")
  public void incidentReturnsEmptyForVehicle(String vehicleName) {
    UUID tenantId = context.currentTenantId();
    WireMockServices.server()
        .stubFor(
            get(urlPathEqualTo("/api/internal/v1/incidents/tenants/" + tenantId))
                .willReturn(okJson("[]")));
  }

  @Cuando("el administrador consulta el historial del vehículo {string}")
  public void adminConsultsVehicleHistory(String vehicleName) throws Exception {
    context.record(
        api.getVehicleHistory(
            context.currentAdministratorToken(), context.vehicleIdOf(vehicleName), null, null));
  }

  @Cuando(
      "el administrador consulta el historial del vehículo {string} desde {string} hasta {string}")
  public void adminConsultsVehicleHistoryWithRange(String vehicleName, String from, String to)
      throws Exception {
    context.record(
        api.getVehicleHistory(
            context.currentAdministratorToken(),
            context.vehicleIdOf(vehicleName),
            RelativeDates.resolve(from),
            RelativeDates.resolve(to)));
  }

  @Cuando("el administrador {string} consulta el historial del vehículo {string}")
  public void specificAdminConsultsVehicleHistory(String email, String vehicleName)
      throws Exception {
    context.record(
        api.getVehicleHistory(
            context.accessTokenOf(email), context.vehicleIdOf(vehicleName), null, null));
  }

  @Y("el historial tiene {int} eventos ordenados de más reciente a más antiguo")
  public void historyHasOrderedItems(int count) throws Exception {
    JsonNode items = context.body().get("items");
    assertThat(items.isArray()).isTrue();
    assertThat(items.size()).isEqualTo(count);
    List<Instant> dates = new ArrayList<>();
    for (JsonNode item : items) {
      dates.add(Instant.parse(item.get("date").asText()));
    }
    assertThat(dates).isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Y("el historial no está marcado como parcial")
  public void historyIsNotPartial() throws Exception {
    assertThat(context.body().get("partial").asBoolean()).isFalse();
    assertThat(context.body().get("unavailableSources").size()).isZero();
  }

  @Y("el historial está marcado como parcial con la fuente {string} no disponible")
  public void historyIsPartialWithUnavailableSource(String source) throws Exception {
    assertThat(context.body().get("partial").asBoolean()).isTrue();
    List<String> sources = new ArrayList<>();
    for (JsonNode node : context.body().get("unavailableSources")) {
      sources.add(node.asText());
    }
    assertThat(sources).contains(source);
  }

  @Y("el historial contiene {int} evento de tipo {string} sin estado")
  public void historyContainsEventsWithoutStatus(int count, String type) throws Exception {
    JsonNode items = context.body().get("items");
    List<JsonNode> matching =
        StreamSupport.stream(items.spliterator(), false)
            .filter(node -> type.equals(node.get("type").asText()))
            .toList();
    assertThat(matching).hasSize(count);
    assertThat(matching).allMatch(node -> node.get("status").isNull());
  }
}
