package com.nexus.rinde.support;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
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
 * Doble de IAM, Trip y Expense respaldado por WireMock. Un único servidor atiende los tres servicios
 * ya que sus rutas no colisionan.
 */
@Component
public class ExternalStubs {

  private static final String IAM_STATUS_PATH = "/api/internal/v1/tenants/[^/]+/status";
  private static final String TRIP_STATUS_PATH =
      "/api/internal/v1/trips/tenants/[^/]+/[^/]+/status";
  private static final String EXPENSE_APPROVED_PATH =
      "/api/internal/v1/expenses/tenants/.*/trips/.*/approved";
  private static final String EXPENSE_ALL_PATH =
      "/api/internal/v1/expenses/tenants/.*/trips/[^/]+$";

  private final WireMockServer server = WireMockServices.server();
  private final ObjectMapper objectMapper;

  private final Map<String, String> tripStatuses = new HashMap<>();
  private final Map<String, List<Map<String, Object>>> approvedByTrip = new HashMap<>();
  private final Map<String, List<Map<String, Object>>> allByTrip = new HashMap<>();

  public ExternalStubs(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    applyDefaults();
  }

  public synchronized void reset() {
    tripStatuses.clear();
    approvedByTrip.clear();
    allByTrip.clear();
    server.resetMappings();
    applyDefaults();
  }

  /** Guarda el estado del viaje y configura el stub del endpoint de Trip. */
  public synchronized void setTripStatus(UUID tenantId, UUID tripId, String status) {
    String key = tenantId + "/" + tripId;
    tripStatuses.put(key, status);
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

  public synchronized void addApprovedExpense(
      UUID tenantId, UUID tripId, UUID expenseId,
      String category, BigDecimal amount, String currency) {
    String key = tenantId + "/" + tripId;
    Map<String, Object> entry = new HashMap<>();
    entry.put("id", expenseId.toString());
    entry.put("category", category);
    entry.put("amount", amount);
    entry.put("currency", currency);
    entry.put("expenseDate", LocalDate.now().toString());
    entry.put("status", "APPROVED");
    approvedByTrip.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
    allByTrip.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
    updateExpenseStubs(tenantId, tripId);
  }

  public synchronized void addExpense(
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
    allByTrip.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
    if ("APPROVED".equals(status)) {
      approvedByTrip.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
    }
    updateExpenseStubs(tenantId, tripId);
  }

  private void updateExpenseStubs(UUID tenantId, UUID tripId) {
    String key = tenantId + "/" + tripId;
    List<Map<String, Object>> approved = approvedByTrip.getOrDefault(key, List.of());
    List<Map<String, Object>> all = allByTrip.getOrDefault(key, List.of());
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
    server.stubFor(
        get(urlPathMatching(TRIP_STATUS_PATH))
            .willReturn(okJson("{\"tripId\":\"00000000-0000-0000-0000-000000000000\",\"status\":\"ASSIGNED\"}")));
    server.stubFor(
        get(urlPathMatching(EXPENSE_APPROVED_PATH))
            .willReturn(okJson("[]")));
    server.stubFor(
        get(urlPathMatching(EXPENSE_ALL_PATH))
            .willReturn(okJson("[]")));
  }
}
