package com.nexus.rinde.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Estado que comparten los pasos de un mismo escenario: última respuesta, tokens, empresas, planes e
 * ids de suscripción creados.
 */
@Component
@ScenarioScope
public class ScenarioContext {

  private final ObjectMapper objectMapper;
  private final Map<String, String> accessTokens = new HashMap<>();
  private final Map<String, String> tenantIds = new HashMap<>();
  private final Map<String, String> userIds = new HashMap<>();
  private final Map<String, String> planIds = new HashMap<>();
  private final Map<String, String> subscriptionIds = new HashMap<>();
  private MvcResult lastResult;
  private String currentAdministratorToken;
  private String currentTenantId;

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
}
