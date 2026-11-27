package com.nexus.rinde.support;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Doble de IAM, Trip, Fleet y Expense respaldado por WireMock. Un único servidor atiende los
 * servicios ya que sus rutas no colisionan.
 */
@Component
public class ExternalStubs {

  private static final String IAM_STATUS_PATH = "/api/internal/v1/tenants/[^/]+/status";
  private static final String TRIP_STATUS_PATH =
      "/api/internal/v1/trips/tenants/[^/]+/[^/]+/status";
  private static final String TRIP_DETAIL_PATH = "/api/internal/v1/trips/tenants/[^/]+/[^/]+";
  private static final String FLEET_DRIVER_PATH =
      "/api/internal/v1/fleet/tenants/[^/]+/users/[^/]+/driver";
  private static final String EXPENSE_APPROVED_PATH =
      "/api/internal/v1/expenses/tenants/.*/trips/.*/approved";
  private static final String EXPENSE_ALL_PATH =
      "/api/internal/v1/expenses/tenants/.*/trips/[^/]+$";

  private final WireMockServer server = WireMockServices.server();
  private final ObjectMapper objectMapper;

  /** Expenses keyed by tenantId/tripId → expenseId → entry with mutable status. */
  private final Map<String, Map<UUID, Map<String, Object>>> expensesById = new HashMap<>();

  public ExternalStubs(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    applyDefaults();
  }

  public synchronized void reset() {
    expensesById.clear();
    server.resetMappings();
    applyDefaults();
  }

  /** Guarda el estado del viaje y configura el stub del endpoint de Trip /status. */
  public synchronized void setTripStatus(UUID tenantId, UUID tripId, String status) {
    String body;
    try {
      body = objectMapper.writeValueAsString(Map.of("tripId", tripId.toString(), "status", status));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    server.stubFor(
        get(urlPathEqualTo(
                "/api/internal/v1/trips/tenants/" + tenantId + "/" + tripId + "/status"))
            .atPriority(1)
            .willReturn(okJson(body)));
  }

  /** Configura el stub del endpoint de detalle de viaje (incluye driverId). */
  public synchronized void setTripDriver(UUID tenantId, UUID tripId, UUID driverId) {
    String body;
    try {
      body = objectMapper.writeValueAsString(
          Map.of("tripId", tripId.toString(), "driverId", driverId.toString()));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    server.stubFor(
        get(urlPathEqualTo("/api/internal/v1/trips/tenants/" + tenantId + "/" + tripId))
            .atPriority(1)
            .willReturn(okJson(body)));
  }

  /** Configura el stub del endpoint de Fleet que resuelve el conductor de un usuario. */
  public synchronized void setDriverForUser(UUID tenantId, UUID userId, UUID driverId) {
    String body;
    try {
      body = objectMapper.writeValueAsString(Map.of("driverId", driverId.toString()));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    server.stubFor(
        get(urlPathEqualTo(
                "/api/internal/v1/fleet/tenants/" + tenantId + "/users/" + userId + "/driver"))
            .atPriority(1)
            .willReturn(okJson(body)));
  }

  /**
   * Registra un gasto en el stub de Expense (o actualiza su estado si ya existe). Las stubs de
   * approved y all se reconstruyen automáticamente.
   */
  public synchronized void registerExpense(
      UUID tenantId, UUID tripId, UUID expenseId,
      String category, BigDecimal amount, String currency, String status) {
    String key = tenantId + "/" + tripId;
    Map<String, Object> entry = new HashMap<>();
    entry.put("id", expenseId.toString());
    entry.put("category", category);
    entry.put("amount", amount);
    entry.put("currency", currency);
    entry.put("expenseDate", LocalDate.now().toString());
    entry.put("status", status);
    expensesById.computeIfAbsent(key, k -> new HashMap<>()).put(expenseId, entry);
    rebuildExpenseStubs(tenantId, tripId);
  }

  /** Cambia el estado de un gasto ya registrado y reconstruye los stubs. */
  public synchronized void setExpenseStatus(
      UUID tenantId, UUID tripId, UUID expenseId, String newStatus) {
    String key = tenantId + "/" + tripId;
    Map<UUID, Map<String, Object>> forTrip = expensesById.get(key);
    if (forTrip != null && forTrip.containsKey(expenseId)) {
      forTrip.get(expenseId).put("status", newStatus);
      rebuildExpenseStubs(tenantId, tripId);
    }
  }

  /** Registra un gasto aprobado (alias conveniente). */
  public synchronized void addApprovedExpense(
      UUID tenantId, UUID tripId, UUID expenseId,
      String category, BigDecimal amount, String currency) {
    registerExpense(tenantId, tripId, expenseId, category, amount, currency, "APPROVED");
  }

  private void rebuildExpenseStubs(UUID tenantId, UUID tripId) {
    String key = tenantId + "/" + tripId;
    List<Map<String, Object>> all =
        new ArrayList<>(expensesById.getOrDefault(key, new HashMap<>()).values());
    List<Map<String, Object>> approved =
        all.stream().filter(e -> "APPROVED".equals(e.get("status"))).toList();
    try {
      server.stubFor(
          get(urlPathEqualTo(
                  "/api/internal/v1/expenses/tenants/" + tenantId + "/trips/" + tripId + "/approved"))
              .atPriority(1)
              .willReturn(okJson(objectMapper.writeValueAsString(approved))));
      server.stubFor(
          get(urlPathEqualTo("/api/internal/v1/expenses/tenants/" + tenantId + "/trips/" + tripId))
              .atPriority(1)
              .willReturn(okJson(objectMapper.writeValueAsString(all))));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private void applyDefaults() {
    server.stubFor(
        get(urlPathMatching(IAM_STATUS_PATH))
            .willReturn(okJson("{\"status\":\"ACTIVE\"}")));
    // Sin stubs por defecto para Trip y Fleet: las rutas no registradas devuelven 404,
    // lo que permite que los escenarios de viaje inexistente funcionen correctamente.
    server.stubFor(
        get(urlPathMatching(TRIP_STATUS_PATH))
            .willReturn(notFound()));
    server.stubFor(
        get(urlPathMatching(TRIP_DETAIL_PATH))
            .willReturn(notFound()));
    server.stubFor(
        get(urlPathMatching(FLEET_DRIVER_PATH))
            .willReturn(notFound()));
    server.stubFor(
        get(urlPathMatching(EXPENSE_APPROVED_PATH))
            .willReturn(okJson("[]")));
    server.stubFor(
        get(urlPathMatching(EXPENSE_ALL_PATH))
            .willReturn(okJson("[]")));
  }
}
