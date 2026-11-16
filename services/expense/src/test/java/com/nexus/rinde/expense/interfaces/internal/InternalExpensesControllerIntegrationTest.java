package com.nexus.rinde.expense.interfaces.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseCategory;
import com.nexus.rinde.expense.domain.model.valueobjects.Money;
import com.nexus.rinde.expense.infrastructure.persistence.jpa.repositories.ExpenseRepository;
import com.nexus.rinde.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InternalExpensesControllerIntegrationTest extends AbstractIntegrationTest {

  @Autowired private ExpenseRepository expenseRepository;

  @Test
  @DisplayName("Consulta interna retorna solo gastos aprobados de un viaje")
  void shouldReturnApprovedExpensesForTrip() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();

    Expense approvedExpense =
        Expense.create(
            tenantId,
            tripId,
            driverId,
            ExpenseCategory.FUEL,
            Money.of(new BigDecimal("150.00"), "PEN"),
            LocalDate.now(),
            "KEY-INT-01");
    approvedExpense.approve(UUID.randomUUID(), Instant.now());
    expenseRepository.save(approvedExpense);

    Expense pendingExpense =
        Expense.create(
            tenantId,
            tripId,
            driverId,
            ExpenseCategory.TOLL,
            Money.of(new BigDecimal("20.00"), "PEN"),
            LocalDate.now(),
            "KEY-INT-02");
    expenseRepository.save(pendingExpense);

    mockMvc
        .perform(get("/api/internal/v1/expenses/tenants/{tenantId}/trips/{tripId}/approved", tenantId, tripId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].category").value("FUEL"))
        .andExpect(jsonPath("$[0].amount").value(150.00))
        .andExpect(jsonPath("$[0].status").value("APPROVED"));
  }
}
