package com.nexus.rinde.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.support.TestTokens;
import io.cucumber.spring.ScenarioScope;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Estado que comparten los pasos de un mismo escenario: última respuesta, usuarios, tokens e ids creados.
 */
@Component
@ScenarioScope
public class ScenarioContext {

  private final ObjectMapper objectMapper;
  private final TestTokens tokens;
  private final Map<String, UUID> tenantIds = new HashMap<>();
  private final Map<String, UUID> userIds = new HashMap<>();
  private final Map<String, UUID> userTenants = new HashMap<>();
  private final Map<String, String> userRoles = new HashMap<>();
  private final Map<String, String> accessTokens = new HashMap<>();
  private final Map<String, String> tripIds = new HashMap<>();
  private final Map<String, String> expenseIds = new HashMap<>();
  private MvcResult lastResult;
  private String currentAdministratorToken;
  private UUID currentTenantId;

  public ScenarioContext(ObjectMapper objectMapper, TestTokens tokens) {
    this.objectMapper = objectMapper;
    this.tokens = tokens;
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

  public String lastResponseBody() {
    try {
      return lastResult != null ? lastResult.getResponse().getContentAsString() : "";
    } catch (Exception e) {
      return "";
    }
  }

  /** Bytes de la última respuesta, para comprobar la lectura de archivos. */
  public byte[] lastResponseBytes() {
    return lastResult.getResponse().getContentAsByteArray();
  }

  public String lastContentType() {
    return lastResult.getResponse().getContentType();
  }

  public JsonNode readJson(String json) throws Exception {
    return objectMapper.readTree(json);
  }

  public void rememberTenant(String ruc, UUID tenantId) {
    tenantIds.put(ruc, tenantId);
  }

  public UUID tenantIdOf(String ruc) {
    return tenantIds.get(ruc);
  }

  /** Registra un usuario de la empresa; sus datos se usan para emitir sus tokens. */
  public void rememberUser(String email, UUID userId, UUID tenantId, String role) {
    userIds.put(email, userId);
    userTenants.put(email, tenantId);
    userRoles.put(email, role);
  }

  public UUID userIdOf(String email) {
    return userIds.get(email);
  }

  /** Devuelve el token del usuario y lo emite la primera vez que se necesita. */
  public String accessTokenOf(String email) {
    return accessTokens.computeIfAbsent(
        email,
        key ->
            tokens.issue(
                userIds.get(key), userTenants.get(key), userRoles.get(key), key));
  }

  public void rememberAccessToken(String email, String token) {
    accessTokens.put(email, token);
  }

  public String emailOfUserId(String userId) {
    return userIds.entrySet().stream()
        .filter(entry -> entry.getValue().toString().equals(userId))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  public void rememberTripId(String name, String tripId) {
    tripIds.put(name, tripId);
  }

  public String tripIdOf(String name) {
    return tripIds.get(name);
  }

  public void rememberExpenseId(String key, String expenseId) {
    expenseIds.put(key, expenseId);
  }

  public String expenseIdOf(String key) {
    return expenseIds.get(key);
  }

  public String currentAdministratorToken() {
    return currentAdministratorToken;
  }

  public void setCurrentAdministratorToken(String token) {
    this.currentAdministratorToken = token;
  }

  public UUID currentTenantId() {
    return currentTenantId;
  }

  public void setCurrentTenantId(UUID tenantId) {
    this.currentTenantId = tenantId;
  }
}
