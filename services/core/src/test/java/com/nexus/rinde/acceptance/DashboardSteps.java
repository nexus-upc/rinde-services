package com.nexus.rinde.acceptance;

import com.nexus.rinde.support.DashboardApi;
import com.nexus.rinde.support.IncidentDouble;
import com.nexus.rinde.support.RelativeDates;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import java.util.UUID;
import org.springframework.test.web.servlet.MockMvc;

/** Pasos de aceptación BDD para el bounded context Dashboard. */
public class DashboardSteps {

  private final ScenarioContext context;
  private final DashboardApi api;
  private final IncidentDouble incidents;

  public DashboardSteps(ScenarioContext context, MockMvc mockMvc, IncidentDouble incidents) {
    this.context = context;
    this.api = new DashboardApi(mockMvc);
    this.incidents = incidents;
  }

  @Cuando("el administrador consulta el resumen del viaje {string}")
  public void adminQueriesTripSummary(String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    String token = context.currentAdministratorToken();
    context.record(api.getTripSummary(token, tripId));
  }

  @Cuando("el administrador consulta el resumen de un viaje inexistente")
  public void adminQueriesUnknownTripSummary() throws Exception {
    String token = context.currentAdministratorToken();
    context.record(api.getTripSummary(token, UUID.randomUUID().toString()));
  }

  @Cuando("una persona sin token consulta el resumen del viaje {string}")
  public void unauthenticatedQueriesTripSummary(String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    context.record(api.getTripSummary(null, tripId));
  }

  @Cuando("el administrador consulta el estado de la flota")
  public void adminQueriesFleetStatus() throws Exception {
    String token = context.currentAdministratorToken();
    context.record(api.getFleetStatus(token));
  }

  @Cuando("una persona sin token consulta el estado de la flota")
  public void unauthenticatedQueriesFleetStatus() throws Exception {
    context.record(api.getFleetStatus(null));
  }

  @Cuando("el administrador consulta las métricas operativas")
  public void adminQueriesMetrics() throws Exception {
    String token = context.currentAdministratorToken();
    context.record(api.getMetrics(token));
  }

  @Cuando("el administrador consulta las métricas operativas del periodo {string} a {string}")
  public void adminQueriesMetricsForPeriod(String from, String to) throws Exception {
    String token = context.currentAdministratorToken();
    context.record(
        api.getMetrics(token, RelativeDates.resolve(from), RelativeDates.resolve(to)));
  }

  @Cuando("una persona sin token consulta las métricas operativas")
  public void unauthenticatedQueriesMetrics() throws Exception {
    context.record(api.getMetrics(null));
  }

  @Dado("que la empresa tiene {int} incidencias abiertas")
  public void tenantHasOpenIncidents(int count) {
    incidents.setOpenCount(UUID.fromString(context.currentTenantId()), count);
  }

  @Dado("que el servicio de incidencias no responde")
  public void incidentServiceIsDown() {
    incidents.makeUnavailable();
  }
}
