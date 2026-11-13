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
 * Doble de IAM y del servicio Expense respaldado por WireMock. Un único servidor atiende ambos
 * servicios ya que sus rutas no colisionan.
 */
@Component
public class ExternalStubs {

  private static final String IAM_STATUS_PATH = "/api/internal/v1/tenants/[^/]+/status";
  private static final String EXPENSE_APPROVED_PATH =
      "/api/internal/v1/expenses/tenants/.*/trips/.*/approved";

  private final WireMockServer server = WireMockServices.server();
  private final ObjectMapper objectMapper;

  private final Map<String, List<Map<String, Object>>> approvedByTrip = new HashMap<>();

  public ExternalStubs(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    applyDefaults();
  }

  public synchronized void reset() {
    approvedByTrip.clear();
    server.resetMappings();
    applyDefaults();
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
    updateExpenseStub(tenantId, tripId);
  }

  private void updateExpenseStub(UUID tenantId, UUID tripId) {
    String key = tenantId + "/" + tripId;
    List<Map<String, Object>> expenses = approvedByTrip.getOrDefault(key, List.of());
    try {
      server.stubFor(
          get(urlPathEqualTo(
                  "/api/internal/v1/expenses/tenants/" + tenantId + "/trips/" + tripId + "/approved"))
              .atPriority(1)
              .willReturn(okJson(objectMapper.writeValueAsString(expenses))));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private void applyDefaults() {
    server.stubFor(
        get(urlPathMatching(IAM_STATUS_PATH))
            .willReturn(okJson("{\"status\":\"ACTIVE\"}")));
    server.stubFor(
        get(urlPathMatching(EXPENSE_APPROVED_PATH))
            .willReturn(okJson("[]")));
  }
}
