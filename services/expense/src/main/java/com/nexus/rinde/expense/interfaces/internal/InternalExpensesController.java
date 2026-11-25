package com.nexus.rinde.expense.interfaces.internal;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseStatus;
import com.nexus.rinde.expense.infrastructure.persistence.jpa.repositories.ExpenseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos consumidos por otros servicios (Settlement, Dashboard). No expuestos por el gateway.
 */
@RestController
@RequestMapping(
    value = "/api/internal/v1/expenses/tenants/{tenantId}",
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Internal", description = "Consultas entre servicios de RINDE (no públicas)")
@SecurityRequirements
public class InternalExpensesController {

  private final ExpenseRepository expenseRepository;

  public InternalExpensesController(ExpenseRepository expenseRepository) {
    this.expenseRepository = expenseRepository;
  }

  @GetMapping("/trips/{tripId}/approved")
  @Operation(summary = "Gastos aprobados de un viaje para liquidación")
  public List<InternalExpenseResource> listApproved(
      @PathVariable UUID tenantId, @PathVariable UUID tripId) {
    return expenseRepository
        .findByTenantIdAndTripIdAndStatus(tenantId, tripId, ExpenseStatus.APPROVED)
        .stream()
        .map(InternalExpensesController::toResource)
        .toList();
  }

  @GetMapping("/trips/{tripId}")
  @Operation(summary = "Todos los gastos de un viaje de la empresa")
  public List<InternalExpenseResource> listAll(
      @PathVariable UUID tenantId, @PathVariable UUID tripId) {
    return expenseRepository.findByTenantIdAndTripId(tenantId, tripId).stream()
        .map(InternalExpensesController::toResource)
        .toList();
  }

  @GetMapping
  @Operation(summary = "Todos los gastos de la empresa, para el tablero operativo")
  public List<InternalExpenseResource> listByTenant(@PathVariable UUID tenantId) {
    return expenseRepository.findByTenantId(tenantId).stream()
        .map(InternalExpensesController::toResource)
        .toList();
  }

  private static InternalExpenseResource toResource(Expense expense) {
    return new InternalExpenseResource(
        expense.getId(),
        expense.getTripId(),
        expense.getCategory().name(),
        expense.getAmount().getAmount(),
        expense.getAmount().getCurrency(),
        expense.getExpenseDate(),
        expense.getStatus().name());
  }

  public record InternalExpenseResource(
      UUID id,
      UUID tripId,
      String category,
      BigDecimal amount,
      String currency,
      LocalDate expenseDate,
      String status) {}
}
