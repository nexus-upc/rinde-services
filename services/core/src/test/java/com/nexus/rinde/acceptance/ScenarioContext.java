package com.nexus.rinde.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.notification.application.events.TripAssignedNotification;
import io.cucumber.spring.ScenarioScope;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** Estado que comparten los pasos de un mismo escenario: última respuesta, tokens e ids creados. */
@Component
@ScenarioScope
public class ScenarioContext {

  public static final String PASSWORD = "Clave-Segura-2026";

  private final ObjectMapper objectMapper;
  private final Map<String, String> accessTokens = new HashMap<>();
  private final Map<String, String> tenantIds = new HashMap<>();
  private final Map<String, String> userIds = new HashMap<>();
  private final Map<String, String> tripIds = new HashMap<>();
  private final Map<String, String> vehicleIds = new HashMap<>();
  private final Map<String, String> driverIds = new HashMap<>();
  private final Map<String, String> expenseIds = new HashMap<>();
  private final Map<String, String> settlementIds = new HashMap<>();
  private final Map<String, String> planIds = new HashMap<>();
  private final Map<String, String> subscriptionIds = new HashMap<>();
  private MvcResult lastResult;
  private String currentAdministratorToken;
  private String currentTenantId;
  private TripAssignedNotification lastTripAssignment;

  public ScenarioContext(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Guarda la respuesta del último request para que los pasos "Entonces" la revisen. */
  public ResultActions record(ResultActions result) {
    this.lastResult = result.andReturn();
    return result;
  }

  public int statusCode() {
    return lastResult.getResponse().getStatus();
  }

  public JsonNode body() throws Exception {
    return objectMapper.readTree(lastResult.getResponse().getContentAsString());
  }

  public String lastResponseBody() throws Exception {
    return lastResult.getResponse().getContentAsString();
  }

  public void rememberAccessToken(String email, String token) {
    accessTokens.put(email, token);
  }

  public String accessTokenOf(String email) {
    return accessTokens.get(email);
  }

  public void rememberTenantId(String ruc, String tenantId) {
    tenantIds.put(ruc, tenantId);
  }

  public String tenantIdOf(String ruc) {
    return tenantIds.get(ruc);
  }

  public void rememberUserId(String email, String userId) {
    userIds.put(email, userId);
  }

  public String userIdOf(String email) {
    return userIds.get(email);
  }

  public void rememberTripId(String name, String tripId) {
    tripIds.put(name, tripId);
  }

  public String tripIdOf(String name) {
    return tripIds.get(name);
  }

  public void rememberVehicleId(String name, String vehicleId) {
    vehicleIds.put(name, vehicleId);
  }

  public String vehicleIdOf(String name) {
    return vehicleIds.get(name);
  }

  public void rememberDriverId(String name, String driverId) {
    driverIds.put(name, driverId);
  }

  public String driverIdOf(String name) {
    return driverIds.get(name);
  }

  public void rememberExpenseId(String name, String expenseId) {
    expenseIds.put(name, expenseId);
  }

  public String expenseIdOf(String name) {
    return expenseIds.get(name);
  }

  public void rememberSettlementId(String name, String settlementId) {
    settlementIds.put(name, settlementId);
  }

  public String settlementIdOf(String name) {
    return settlementIds.get(name);
  }

  public void rememberPlanId(String name, String planId) {
    planIds.put(name, planId);
  }

  public String planIdOf(String name) {
    return planIds.get(name);
  }

  public void rememberSubscriptionId(String name, String subscriptionId) {
    subscriptionIds.put(name, subscriptionId);
  }

  public String subscriptionIdOf(String name) {
    return subscriptionIds.get(name);
  }

  public void rememberLatestSubscriptionId(String subscriptionId) {
    rememberSubscriptionId("última", subscriptionId);
  }

  public String latestSubscriptionId() {
    return subscriptionIdOf("última");
  }

  public String emailOfUserId(String userId) {
    return userIds.entrySet().stream()
        .filter(entry -> entry.getValue().equals(userId))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  public String currentAdministratorToken() {
    return currentAdministratorToken;
  }

  public void setCurrentAdministratorToken(String token) {
    this.currentAdministratorToken = token;
  }

  /** Empresa sobre la que actúan los pasos de preparación (la última registrada o en sesión). */
  public String currentTenantId() {
    return currentTenantId;
  }

  public void setCurrentTenantId(String tenantId) {
    this.currentTenantId = tenantId;
  }

  /** Último aviso de asignación entregado al consumidor, para verificar la notificación. */
  public void rememberTripAssignment(TripAssignedNotification assignment) {
    this.lastTripAssignment = assignment;
  }

  public TripAssignedNotification lastTripAssignment() {
    return lastTripAssignment;
  }
}
