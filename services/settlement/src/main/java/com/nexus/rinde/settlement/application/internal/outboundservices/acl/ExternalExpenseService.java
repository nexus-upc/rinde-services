package com.nexus.rinde.settlement.application.internal.outboundservices.acl;

import com.nexus.rinde.settlement.domain.model.valueobjects.ApprovedExpense;
import com.nexus.rinde.settlement.domain.model.valueobjects.TripExpenseReview;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador ACL que consulta por HTTP los gastos aprobados del viaje al microservicio Expense. Si
 * Expense no responde, la operación informa indisponibilidad.
 */
@Service
public class ExternalExpenseService {

  private static final Logger log = LoggerFactory.getLogger(ExternalExpenseService.class);
  private static final String APPROVED = "APPROVED";
  private static final String OBSERVED = "OBSERVED";
  private static final String APPROVED_PATH =
      "/api/internal/v1/expenses/tenants/{tenantId}/trips/{tripId}/approved";
  private static final String ALL_PATH =
      "/api/internal/v1/expenses/tenants/{tenantId}/trips/{tripId}";
  private static final ParameterizedTypeReference<List<ExpenseResponse>> LIST =
      new ParameterizedTypeReference<>() {};

  private final RestClient restClient;

  public ExternalExpenseService(
      @Value("${rinde.expense.base-url:http://localhost:8084}") String serviceUrl) {
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofSeconds(2)));
    this.restClient =
        RestClient.builder().baseUrl(serviceUrl).requestFactory(requestFactory).build();
  }

  public List<ApprovedExpense> findApprovedExpensesByTripId(UUID tenantId, UUID tripId) {
    List<ExpenseResponse> responses = fetch(APPROVED_PATH, tenantId, tripId);
    return responses.stream().map(ExternalExpenseService::toApprovedExpense).toList();
  }

  /** Gastos aprobados del viaje y cantidad de gastos que no están aprobados ni observados. */
  public TripExpenseReview reviewByTripId(UUID tenantId, UUID tripId) {
    List<ExpenseResponse> responses = fetch(ALL_PATH, tenantId, tripId);
    List<ApprovedExpense> approved =
        responses.stream()
            .filter(response -> APPROVED.equals(response.status()))
            .map(ExternalExpenseService::toApprovedExpense)
            .toList();
    int pending =
        (int)
            responses.stream()
                .filter(
                    response ->
                        !APPROVED.equals(response.status()) && !OBSERVED.equals(response.status()))
                .count();
    return new TripExpenseReview(approved, pending);
  }

  private List<ExpenseResponse> fetch(String path, UUID tenantId, UUID tripId) {
    try {
      List<ExpenseResponse> body =
          restClient.get().uri(path, tenantId, tripId).retrieve().body(LIST);
      return body == null ? List.of() : body;
    } catch (RestClientException ex) {
      log.warn("Expense no respondió los gastos del viaje: {}", ex.getMessage());
      throw new ExpenseServiceUnavailableException("No se pudo consultar los gastos en Expense.");
    }
  }

  private static ApprovedExpense toApprovedExpense(ExpenseResponse response) {
    return new ApprovedExpense(
        response.id(),
        response.category(),
        response.amount(),
        response.currency(),
        response.expenseDate(),
        response.status());
  }

  public record ExpenseResponse(
      UUID id,
      String category,
      BigDecimal amount,
      String currency,
      LocalDate expenseDate,
      String status) {}
}
