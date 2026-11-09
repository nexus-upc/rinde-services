package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.support.SettlementApi;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.test.web.servlet.MockMvc;

/** Pasos de aceptación BDD para el bounded context Settlement. */
public class SettlementSteps {

  private final ScenarioContext context;
  private final SettlementApi api;

  public SettlementSteps(ScenarioContext context, MockMvc mockMvc, ObjectMapper objectMapper) {
    this.context = context;
    this.api = new SettlementApi(mockMvc, objectMapper);
  }

  @Dado("que existe la liquidación {string} del viaje {string}")
  public void settlementExistsForTrip(String settlementName, String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    String token = context.currentAdministratorToken();
    var res = api.getByTrip(token, tripId);
    JsonNode node = context.record(res).andReturn().getResponse().getContentAsString().isEmpty()
        ? null
        : context.body();
    if (node != null && node.has("id")) {
      context.rememberSettlementId(settlementName, node.get("id").asText());
    }
  }

  @Cuando(
      "el administrador registra un adelanto de {string} soles por {string} para el viaje {string}")
  public void adminRegistersAdvance(String amount, String deliveryMethod, String tripName)
      throws Exception {
    String tripId = context.tripIdOf(tripName);
    String token = context.currentAdministratorToken();
    context.record(
        api.registerAdvance(token, tripId, new BigDecimal(amount), "PEN", deliveryMethod));
  }

  @Cuando(
      "el administrador registra un adelanto de {string} soles por {string} para un viaje inexistente")
  public void adminRegistersAdvanceForUnknownTrip(String amount, String deliveryMethod)
      throws Exception {
    String token = context.currentAdministratorToken();
    context.record(
        api.registerAdvance(
            token, UUID.randomUUID().toString(), new BigDecimal(amount), "PEN", deliveryMethod));
  }

  @Cuando(
      "el conductor {string} registra un adelanto de {string} soles por {string} para el viaje {string}")
  public void driverRegistersAdvance(
      String email, String amount, String deliveryMethod, String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    String token = context.accessTokenOf(email);
    context.record(
        api.registerAdvance(token, tripId, new BigDecimal(amount), "PEN", deliveryMethod));
  }

  @Cuando(
      "una persona sin token registra un adelanto de {string} soles por {string} para el viaje {string}")
  public void unauthenticatedRegistersAdvance(String amount, String deliveryMethod, String tripName)
      throws Exception {
    String tripId = context.tripIdOf(tripName);
    context.record(
        api.registerAdvance(null, tripId, new BigDecimal(amount), "PEN", deliveryMethod));
  }

  @Cuando("el administrador consulta los adelantos del viaje {string}")
  public void adminListsAdvances(String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    String token = context.currentAdministratorToken();
    context.record(api.listAdvances(token, tripId));
  }

  @Cuando("el conductor {string} consulta los adelantos del viaje {string}")
  public void driverListsAdvances(String email, String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    String token = context.accessTokenOf(email);
    context.record(api.listAdvances(token, tripId));
  }

  @Cuando("el administrador consulta la liquidación {string}")
  public void adminQueriesSettlement(String settlementName) throws Exception {
    String settlementId = context.settlementIdOf(settlementName);
    String token = context.currentAdministratorToken();
    context.record(api.getById(token, settlementId));
  }

  @Cuando("el administrador consulta una liquidación con id inexistente")
  public void adminQueriesUnknownSettlement() throws Exception {
    String token = context.currentAdministratorToken();
    context.record(api.getById(token, UUID.randomUUID().toString()));
  }

  @Cuando("el conductor {string} consulta la liquidación {string}")
  public void driverQueriesSettlement(String email, String settlementName) throws Exception {
    String settlementId = context.settlementIdOf(settlementName);
    String token = context.accessTokenOf(email);
    context.record(api.getById(token, settlementId));
  }

  @Cuando("una persona sin token consulta la liquidación {string}")
  public void unauthenticatedQueriesSettlement(String settlementName) throws Exception {
    String settlementId = context.settlementIdOf(settlementName);
    context.record(api.getById(null, settlementId));
  }

  @Cuando("el administrador consulta la liquidación del viaje {string}")
  public void adminQueriesSettlementByTrip(String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    String token = context.currentAdministratorToken();
    context.record(api.getByTrip(token, tripId));
  }

  @Cuando("el administrador consulta la liquidación de un viaje sin liquidación")
  public void adminQueriesSettlementForUnknownTrip() throws Exception {
    String token = context.currentAdministratorToken();
    context.record(api.getByTrip(token, UUID.randomUUID().toString()));
  }

  @Cuando("el conductor {string} consulta la liquidación del viaje {string}")
  public void driverQueriesSettlementByTrip(String email, String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    String token = context.accessTokenOf(email);
    context.record(api.getByTrip(token, tripId));
  }

  @Cuando("una persona sin token consulta la liquidación del viaje {string}")
  public void unauthenticatedQueriesSettlementByTrip(String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    context.record(api.getByTrip(null, tripId));
  }

  @Y("la respuesta incluye el campo {string} con el id del viaje {string}")
  public void responseContainsTripId(String field, String tripName) throws Exception {
    String expectedTripId = context.tripIdOf(tripName);
    assertThat(context.body().get(field).asText()).isEqualTo(expectedTripId);
  }

  @Cuando("el administrador recalcula la liquidación {string}")
  public void adminRecalculatesSettlement(String settlementName) throws Exception {
    String settlementId = context.settlementIdOf(settlementName);
    String token = context.currentAdministratorToken();
    context.record(api.recalculate(token, settlementId));
  }

  @Cuando("el administrador recalcula una liquidación con id inexistente")
  public void adminRecalculatesUnknown() throws Exception {
    String token = context.currentAdministratorToken();
    context.record(api.recalculate(token, UUID.randomUUID().toString()));
  }

  @Cuando("el conductor {string} intenta recalcular la liquidación {string}")
  public void driverRecalculates(String email, String settlementName) throws Exception {
    String settlementId = context.settlementIdOf(settlementName);
    String token = context.accessTokenOf(email);
    context.record(api.recalculate(token, settlementId));
  }

  @io.cucumber.java.es.Dado("que el administrador cerró la liquidación {string}")
  public void adminAlreadyClosedSettlement(String settlementName) throws Exception {
    String settlementId = context.settlementIdOf(settlementName);
    String token = context.currentAdministratorToken();
    context.record(api.close(token, settlementId));
  }

  @Cuando("el administrador cierra la liquidación {string}")
  public void adminClosesSettlement(String settlementName) throws Exception {
    String settlementId = context.settlementIdOf(settlementName);
    String token = context.currentAdministratorToken();
    context.record(api.close(token, settlementId));
  }

  @Cuando("el administrador cierra una liquidación con id inexistente")
  public void adminClosesUnknown() throws Exception {
    String token = context.currentAdministratorToken();
    context.record(api.close(token, UUID.randomUUID().toString()));
  }

  @Cuando("el conductor {string} intenta cerrar la liquidación {string}")
  public void driverTriesToClose(String email, String settlementName) throws Exception {
    String settlementId = context.settlementIdOf(settlementName);
    String token = context.accessTokenOf(email);
    context.record(api.close(token, settlementId));
  }

  @Cuando("una persona sin token intenta cerrar la liquidación {string}")
  public void unauthenticatedCloses(String settlementName) throws Exception {
    String settlementId = context.settlementIdOf(settlementName);
    context.record(api.close(null, settlementId));
  }

  @Cuando("el administrador exporta la liquidación {string}")
  public void adminExportsSettlement(String settlementName) throws Exception {
    String settlementId = context.settlementIdOf(settlementName);
    String token = context.currentAdministratorToken();
    context.record(api.export(token, settlementId));
  }

  @Cuando("el administrador exporta una liquidación con id inexistente")
  public void adminExportsUnknown() throws Exception {
    String token = context.currentAdministratorToken();
    context.record(api.export(token, UUID.randomUUID().toString()));
  }

  @Cuando("una persona sin token exporta la liquidación {string}")
  public void unauthenticatedExports(String settlementName) throws Exception {
    String settlementId = context.settlementIdOf(settlementName);
    context.record(api.export(null, settlementId));
  }

  @Y("la respuesta CSV contiene {string}")
  public void csvContains(String expected) throws Exception {
    String content = context.lastResponseBody();
    assertThat(content).contains(expected);
  }
}
