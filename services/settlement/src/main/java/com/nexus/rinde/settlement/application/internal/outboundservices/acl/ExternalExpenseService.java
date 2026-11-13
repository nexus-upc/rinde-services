package com.nexus.rinde.settlement.application.internal.outboundservices.acl;

import com.nexus.rinde.settlement.domain.model.valueobjects.ApprovedExpense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Obtiene los gastos aprobados de un viaje llamando al servicio Expense por HTTP. */
@Service
public class ExternalExpenseService {

  private static final ParameterizedTypeReference<List<ApprovedExpenseResponse>> RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {};

  private final RestClient client;

  public ExternalExpenseService(@Qualifier("expenseRestClient") RestClient client) {
    this.client = client;
  }

  public List<ApprovedExpense> findApprovedExpensesByTripId(UUID tenantId, UUID tripId) {
    try {
      List<ApprovedExpenseResponse> body =
          client
              .get()
              .uri(
                  "/api/internal/v1/expenses/tenants/{tenantId}/trips/{tripId}/approved",
                  tenantId,
                  tripId)
              .retrieve()
              .body(RESPONSE_TYPE);
      if (body == null) return List.of();
      return body.stream().map(ExternalExpenseService::toApprovedExpense).toList();
    } catch (RestClientException ex) {
      throw new ExpenseServiceUnavailableException(ex);
    }
  }

  private static ApprovedExpense toApprovedExpense(ApprovedExpenseResponse r) {
    return new ApprovedExpense(r.id(), r.category(), r.amount(), r.currency(), r.expenseDate(), r.status());
  }

  public record ApprovedExpenseResponse(
      UUID id,
      String category,
      BigDecimal amount,
      String currency,
      LocalDate expenseDate,
      String status) {}
}
