package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.incident.domain.model.events.IncidentClosed;
import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import com.nexus.rinde.support.CapturedEvents;
import com.nexus.rinde.support.IncidentApi;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Pasos de reporte, consulta, atención y cierre de incidencias. */
public class IncidentSteps {

  private final ScenarioContext context;
  private final CapturedEvents capturedEvents;
  private final IncidentApi api;

  public IncidentSteps(
      ScenarioContext context,
      CapturedEvents capturedEvents,
      MockMvc mockMvc,
      ObjectMapper objectMapper) {
    this.context = context;
    this.capturedEvents = capturedEvents;
    this.api = new IncidentApi(mockMvc, objectMapper);
  }

  // Reporte del conductor (US29)

  @Cuando(
      "el conductor {string} reporta la incidencia {string} de tipo {string} con la descripción {string} y la evidencia {string} en el viaje {string}")
  public void driverReportsWithEvidence(
      String driver,
      String name,
      String type,
      String description,
      String evidenceUrl,
      String tripName)
      throws Exception {
    report(driver, name, type, description, evidenceUrl, null, tripName);
  }

  @Cuando(
      "el conductor {string} reporta la incidencia {string} de tipo {string} con la descripción {string} sin evidencia en el viaje {string}")
  public void driverReportsWithoutEvidence(
      String driver, String name, String type, String description, String tripName)
      throws Exception {
    report(driver, name, type, description, null, null, tripName);
  }

  @Dado(
      "que el conductor {string} reportó la incidencia {string} de tipo {string} con la descripción {string} sin evidencia en el viaje {string}")
  public void driverReportedInBackground(
      String driver, String name, String type, String description, String tripName)
      throws Exception {
    report(driver, name, type, description, null, null, tripName);
  }

  @Cuando(
      "el conductor {string} sincroniza la incidencia {string} de tipo {string} con la descripción {string} y la clave {string} en el viaje {string}")
  public void driverSyncsOfflineReport(
      String driver,
      String name,
      String type,
      String description,
      String idempotencyKey,
      String tripName)
      throws Exception {
    report(driver, name, type, description, null, idempotencyKey, tripName);
  }

  @Cuando(
      "el conductor {string} reporta la incidencia {string} de tipo {string} con la descripción {string} sin evidencia en un viaje que no existe")
  public void driverReportsOnUnknownTrip(
      String driver, String name, String type, String description) throws Exception {
    ResultActions result =
        api.report(
            context.accessTokenOf(driver),
            UUID.randomUUID().toString(),
            type,
            description,
            null,
            null);
    context.record(result);
    rememberIfCreated(name);
  }

  @Cuando("una persona sin token reporta una incidencia en el viaje {string}")
  public void unauthenticatedReports(String tripName) throws Exception {
    context.record(
        api.report(null, context.tripIdOf(tripName).toString(), "OTHER", "Sin sesión", null, null));
  }

  // Consulta (US30)

  @Cuando("el administrador consulta las incidencias")
  public void administratorListsIncidents() throws Exception {
    context.record(api.list(administratorToken(), null));
  }

  @Cuando("el administrador consulta las incidencias en estado {string}")
  public void administratorListsByStatus(String status) throws Exception {
    context.record(api.list(administratorToken(), status));
  }

  @Cuando("el conductor {string} consulta las incidencias")
  public void driverListsIncidents(String driver) throws Exception {
    context.record(api.list(context.accessTokenOf(driver), null));
  }

  @Cuando("el administrador consulta el detalle de la incidencia {string}")
  public void administratorReadsDetail(String name) throws Exception {
    context.record(api.detail(administratorToken(), context.incidentIdOf(name).toString()));
  }

  @Cuando("el conductor {string} consulta el detalle de la incidencia {string}")
  public void driverReadsDetail(String driver, String name) throws Exception {
    context.record(api.detail(context.accessTokenOf(driver), context.incidentIdOf(name).toString()));
  }

  @Cuando("el administrador de la otra empresa {string} consulta el detalle de la incidencia {string}")
  public void otherAdministratorReadsDetail(String email, String name) throws Exception {
    context.record(api.detail(context.accessTokenOf(email), context.incidentIdOf(name).toString()));
  }

  // Atención y cierre (US31)

  @Cuando("el administrador pasa la incidencia {string} a atención")
  public void administratorStartsAttention(String name) throws Exception {
    startAttention(administratorToken(), name);
  }

  @Dado("que la incidencia {string} está en atención")
  public void incidentIsInAttention(String name) throws Exception {
    startAttention(administratorToken(), name);
    assertThat(context.statusCode()).isEqualTo(200);
  }

  @Cuando("el conductor {string} intenta pasar la incidencia {string} a atención")
  public void driverStartsAttention(String driver, String name) throws Exception {
    startAttention(context.accessTokenOf(driver), name);
  }

  @Cuando("el administrador intenta pasar la incidencia {string} al estado {string}")
  public void administratorChangesStatus(String name, String status) throws Exception {
    context.record(
        api.changeStatus(administratorToken(), context.incidentIdOf(name).toString(), status));
  }

  @Cuando("el administrador cierra la incidencia {string} con la acción {string}")
  public void administratorClosesIncident(String name, String action) throws Exception {
    close(administratorToken(), name, action);
  }

  @Dado("que la incidencia {string} fue cerrada con la acción {string}")
  public void incidentWasClosed(String name, String action) throws Exception {
    close(administratorToken(), name, action);
    assertThat(context.statusCode()).isEqualTo(200);
  }

  @Cuando("el administrador intenta cerrar la incidencia {string} sin acción")
  public void administratorClosesWithoutAction(String name) throws Exception {
    close(administratorToken(), name, null);
  }

  @Cuando("el conductor {string} intenta cerrar la incidencia {string} con la acción {string}")
  public void driverClosesIncident(String driver, String name, String action) throws Exception {
    close(context.accessTokenOf(driver), name, action);
  }

  @Cuando(
      "el administrador de la otra empresa {string} intenta cerrar la incidencia {string} con la acción {string}")
  public void otherAdministratorClosesIncident(String email, String name, String action)
      throws Exception {
    close(context.accessTokenOf(email), name, action);
  }

  // Respuestas y eventos

  @Y("la respuesta incluye la incidencia {string}")
  public void theResponseIncludesIncident(String name) throws Exception {
    assertThat(context.body().get("id").asText()).isEqualTo(context.incidentIdOf(name).toString());
  }

  @Y("la respuesta incluye el vehículo {string} y el viaje {string}")
  public void theResponseIncludesVehicleAndTrip(String vehicleName, String tripName)
      throws Exception {
    JsonNode body = context.body();
    assertThat(body.get("vehicleId").asText()).isEqualTo(context.tripVehicleIdOf(tripName).toString());
    assertThat(body.get("tripId").asText()).isEqualTo(context.tripIdOf(tripName).toString());
    assertThat(context.vehicleIdOf(vehicleName)).isEqualTo(context.tripVehicleIdOf(tripName));
  }

  @Y("la respuesta incluye la resolución con la acción {string} y cerrada por {string}")
  public void theResponseIncludesResolution(String action, String email) throws Exception {
    JsonNode resolution = context.body().get("resolution");
    assertThat(resolution.get("action").asText()).isEqualTo(action);
    assertThat(resolution.get("closedBy").asText()).isEqualTo(context.userIdOf(email).toString());
    assertThat(resolution.get("closedAt").isNull()).isFalse();
  }

  @Y("la lista tiene un total de {int}")
  public void theListHasTotal(int expected) throws Exception {
    assertThat(context.body().size()).isEqualTo(expected);
  }

  @Y("la lista incluye la incidencia {string}")
  public void theListIncludesIncident(String name) throws Exception {
    assertThat(ids(context.body())).contains(context.incidentIdOf(name).toString());
  }

  @Y("la lista no incluye la incidencia {string}")
  public void theListDoesNotIncludeIncident(String name) throws Exception {
    assertThat(ids(context.body())).doesNotContain(context.incidentIdOf(name).toString());
  }

  @Y("las incidencias de la lista están ordenadas de la más reciente a la más antigua")
  public void theListIsOrderedNewestFirst() throws Exception {
    JsonNode list = context.body();
    for (int i = 1; i < list.size(); i++) {
      Instant previous = Instant.parse(list.get(i - 1).get("reportedAt").asText());
      Instant current = Instant.parse(list.get(i).get("reportedAt").asText());
      assertThat(previous).isAfterOrEqualTo(current);
    }
  }

  @Y("la lista muestra el tipo, el viaje, el vehículo, el conductor y el estado de cada incidencia")
  public void theListShowsRequiredFields() throws Exception {
    for (JsonNode incident : context.body()) {
      assertThat(incident.get("type").asText()).isNotBlank();
      assertThat(incident.get("tripId").asText()).isNotBlank();
      assertThat(incident.get("vehicleId").asText()).isNotBlank();
      assertThat(incident.get("reportedBy").asText()).isNotBlank();
      assertThat(incident.get("status").asText()).isNotBlank();
    }
  }

  @Y("todas las incidencias de la lista tienen el estado {string}")
  public void allIncidentsHaveStatus(String status) throws Exception {
    for (JsonNode incident : context.body()) {
      assertThat(incident.get("status").asText()).isEqualTo(status);
    }
  }

  @Y("se publica una sola vez el evento {string}")
  public void theEventIsPublishedOnce(String type) {
    long count =
        capturedEvents.all(IntegrationEvent.class).stream()
            .filter(event -> event.type().equals(type))
            .count();
    assertThat(count).isEqualTo(1);
  }

  @Y("se publica el evento {string} del vehículo {string}")
  public void theClosedEventCarriesVehicle(String type, String vehicleName) {
    assertThat(type).isEqualTo("IncidentClosed");
    assertThat(capturedEvents.all(IncidentClosed.class))
        .anySatisfy(
            event ->
                assertThat(event.vehicleId()).isEqualTo(context.vehicleIdOf(vehicleName)));
  }

  private void report(
      String driver,
      String name,
      String type,
      String description,
      String evidenceUrl,
      String idempotencyKey,
      String tripName)
      throws Exception {
    ResultActions result =
        api.report(
            context.accessTokenOf(driver),
            context.tripIdOf(tripName).toString(),
            type,
            description,
            evidenceUrl,
            idempotencyKey);
    context.record(result);
    rememberIfCreated(name);
  }

  /** Recuerda el id de la incidencia cuando el reporte se aceptó, con 201 o con 200 en un reenvío. */
  private void rememberIfCreated(String name) throws Exception {
    if (context.statusCode() == 201 || context.statusCode() == 200) {
      context.rememberIncidentId(name, context.body().get("id").asText());
    }
  }

  private void startAttention(String token, String name) throws Exception {
    context.record(api.changeStatus(token, context.incidentIdOf(name).toString(), "IN_PROGRESS"));
  }

  private void close(String token, String name, String action) throws Exception {
    context.record(api.close(token, context.incidentIdOf(name).toString(), action));
  }

  private String administratorToken() {
    return context.accessTokenOf(context.currentAdminEmail());
  }

  private static List<String> ids(JsonNode list) {
    List<String> ids = new ArrayList<>();
    for (JsonNode incident : list) {
      ids.add(incident.get("id").asText());
    }
    return ids;
  }
}
