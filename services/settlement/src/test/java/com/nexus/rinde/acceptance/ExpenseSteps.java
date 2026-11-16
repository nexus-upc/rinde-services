package com.nexus.rinde.acceptance;

import com.nexus.rinde.support.ExternalStubs;
import com.nexus.rinde.support.SettlementApi;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pasos de gastos en el contexto del servicio Settlement. Los gastos viven en el microservicio
 * Expense; aquí se simulan configurando el stub WireMock para que el servicio Settlement reciba la
 * lista de gastos aprobados correcta cuando cierra o recalcula una liquidación.
 */
public class ExpenseSteps {

  private final ScenarioContext context;
  private final ExternalStubs externalStubs;
  private final SettlementApi api;

  private final Map<String, UUID> expenseUuids = new HashMap<>();
  private final Map<String, String> expenseTrips = new HashMap<>();
  private final Map<String, BigDecimal> expenseAmounts = new HashMap<>();
  private final Map<String, String> expenseCategories = new HashMap<>();

  public ExpenseSteps(ScenarioContext context, ExternalStubs externalStubs,
      MockMvc mockMvc, ObjectMapper objectMapper) {
    this.context = context;
    this.externalStubs = externalStubs;
    this.api = new SettlementApi(mockMvc, objectMapper);
  }

  @Dado("que el conductor registró un gasto {string} de tipo {string} por el monto {string} con clave {string} en el viaje {string}")
  public void driverRegisteredExpense(String expenseName, String category, String amount,
      String key, String tripName) {
    UUID expenseId = UUID.randomUUID();
    context.rememberExpenseId(expenseName, expenseId.toString());
    expenseUuids.put(expenseName, expenseId);
    expenseTrips.put(expenseName, tripName);
    expenseAmounts.put(expenseName, new BigDecimal(amount));
    expenseCategories.put(expenseName, category);
    // No configuramos el stub aún: solo se agrega cuando el administrador aprueba el gasto
  }

  @Dado("que el administrador {string} aprobó el gasto {string}")
  public void adminApprovedExpense(String adminEmail, String expenseName) {
    UUID expenseId = expenseUuids.get(expenseName);
    String tripName = expenseTrips.get(expenseName);
    String tripId = context.tripIdOf(tripName);
    BigDecimal amount = expenseAmounts.get(expenseName);
    String category = expenseCategories.get(expenseName);
    UUID tenantId = context.currentTenantId();
    externalStubs.addApprovedExpense(tenantId, UUID.fromString(tripId), expenseId, category, amount, "PEN");
  }

  @Cuando("el conductor {string} intenta consultar la liquidación del viaje {string}")
  public void driverTriesToQuerySettlementByTrip(String email, String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    String token = context.accessTokenOf(email);
    context.record(api.getByTrip(token, tripId));
  }
}
