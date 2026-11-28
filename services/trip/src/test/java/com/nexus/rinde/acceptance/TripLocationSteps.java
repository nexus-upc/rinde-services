package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.support.RelativeDates;
import com.nexus.rinde.support.TripApi;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Pasos de aceptación para el registro y consulta de ubicaciones de viajes en ruta. */
public class TripLocationSteps {

  private final ScenarioContext context;
  private final TripApi api;
  private final JdbcTemplate jdbcTemplate;

  public TripLocationSteps(
      ScenarioContext context,
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      JdbcTemplate jdbcTemplate) {
    this.context = context;
    this.api = new TripApi(mockMvc, objectMapper);
    this.jdbcTemplate = jdbcTemplate;
  }

  @Dado("que el conductor {string} registró ubicaciones para el viaje {string}:")
  @Dado("el conductor {string} registró ubicaciones para el viaje {string}:")
  @Cuando("el conductor {string} registra ubicaciones para el viaje {string}:")
  @Cuando("el usuario {string} intenta registrar ubicaciones para el viaje {string}:")
  @Cuando("el conductor {string} intenta registrar ubicaciones inválidas para el viaje {string}:")
  public void theUserRecordsLocations(String email, String tripName, DataTable table)
      throws Exception {
    List<Map<String, Object>> points = new ArrayList<>();
    for (Map<String, String> row : table.asMaps()) {
      Map<String, Object> point = new LinkedHashMap<>();
      point.put("latitude", new BigDecimal(row.get("latitude")));
      point.put("longitude", new BigDecimal(row.get("longitude")));
      String accuracy = row.get("accuracyMeters");
      if (accuracy != null && !accuracy.isBlank()) {
        point.put("accuracyMeters", new BigDecimal(accuracy));
      }
      point.put("recordedAt", RelativeDates.resolve(row.get("recordedAt"), context.scenarioNow()));
      points.add(point);
    }
    context.record(
        api.recordLocations(
            context.accessTokenOf(email), context.tripIdOf(tripName), Map.of("points", points)));
  }

  @Cuando("el usuario {string} consulta las últimas ubicaciones de viajes en ruta")
  public void theUserQueriesLatestLocations(String email) throws Exception {
    context.record(api.latestLocations(context.accessTokenOf(email)));
  }

  @Cuando("el administrador consulta las últimas ubicaciones de viajes en ruta")
  public void theAdministratorQueriesLatestLocations() throws Exception {
    context.record(api.latestLocations(context.currentAdministratorToken()));
  }

  @Y("el lote informa {int} guardados y {int} repetidos")
  public void theBatchReportsCounts(int savedCount, int duplicateCount) throws Exception {
    assertThat(context.body().get("savedCount").asInt()).isEqualTo(savedCount);
    assertThat(context.body().get("duplicateCount").asInt()).isEqualTo(duplicateCount);
  }

  @Y("en la base de datos hay {int} ubicaciones registradas para el viaje {string}")
  public void theDatabaseHasLocationCount(int expectedCount, String tripName) {
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from trip.trip_locations where trip_id = ?",
            Integer.class,
            UUID.fromString(context.tripIdOf(tripName)));
    assertThat(count).isEqualTo(expectedCount);
  }

  @Y("la respuesta incluye errores de validación en los campos {string}")
  public void theResponseIncludesValidationErrors(String fields) throws Exception {
    JsonNode errors = context.body().get("errors");
    assertThat(errors).isNotNull();
    for (String field : fields.split(",")) {
      assertThat(errors.has(field.trim()))
          .as("Se esperaba error de validación para %s", field)
          .isTrue();
    }
  }

  @Y("el campo {string} tiene el error de validación {string}")
  public void theFieldHasValidationError(String field, String message) throws Exception {
    JsonNode errors = context.body().get("errors");
    assertThat(errors).isNotNull();
    assertThat(errors.get(field).asText()).isEqualTo(message);
  }

  @Y("la consulta de ubicaciones devuelve {int} viajes")
  public void theLatestLocationsHasSize(int expected) throws Exception {
    assertThat(context.body().size()).isEqualTo(expected);
  }

  @Y("la lista de ubicaciones está vacía")
  public void theLatestLocationsListIsEmpty() throws Exception {
    assertThat(context.body()).isEmpty();
  }

  @Y("el viaje {string} tiene última ubicación {string}, {string} registrada en {string}")
  public void theTripHasLatestLocation(
      String tripName, String latitude, String longitude, String recordedAt) throws Exception {
    JsonNode item = findTripItem(tripName);
    assertThat(item.get("vehicleId").isNull()).isFalse();
    assertThat(item.get("latitude").decimalValue())
        .isEqualByComparingTo(new BigDecimal(latitude));
    assertThat(item.get("longitude").decimalValue())
        .isEqualByComparingTo(new BigDecimal(longitude));
    assertThat(item.get("recordedAt").asText())
        .isEqualTo(RelativeDates.resolve(recordedAt, context.scenarioNow()));
  }

  @Y("el viaje {string} aparece sin ubicación compartida")
  public void theTripAppearsWithoutSharedLocation(String tripName) throws Exception {
    JsonNode item = findTripItem(tripName);
    assertThat(item.get("vehicleId").isNull()).isFalse();
    assertThat(item.get("latitude").isNull()).isTrue();
    assertThat(item.get("longitude").isNull()).isTrue();
    assertThat(item.get("accuracyMeters").isNull()).isTrue();
    assertThat(item.get("recordedAt").isNull()).isTrue();
  }

  @Y("la consulta de ubicaciones no incluye el viaje {string}")
  public void theLatestLocationsExcludesTrip(String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    boolean present =
        StreamSupport.stream(context.body().spliterator(), false)
            .anyMatch(item -> tripId.equals(item.get("tripId").asText()));
    assertThat(present).isFalse();
  }

  private JsonNode findTripItem(String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    return StreamSupport.stream(context.body().spliterator(), false)
        .filter(item -> tripId.equals(item.get("tripId").asText()))
        .findFirst()
        .orElseThrow(
            () -> new AssertionError("No se encontró el viaje " + tripName + " en la respuesta."));
  }
}
