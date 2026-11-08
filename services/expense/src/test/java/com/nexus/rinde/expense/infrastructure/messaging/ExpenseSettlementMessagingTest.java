package com.nexus.rinde.expense.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class ExpenseSettlementMessagingTest extends AbstractIntegrationTest {

  @Autowired private ExpenseSettlementMessaging messaging;
  @MockitoSpyBean private ExpenseRepository expenseRepository;
  @Autowired private JdbcTemplate jdbc;

  @Test
  @DisplayName("Recepción idempotente de evento settlement.closed bloquea los gastos")
  void shouldLockExpensesOnSettlementClosedIdempotently() {
    UUID tenantId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();

    Expense expense =
        Expense.create(
            tenantId,
            tripId,
            driverId,
            ExpenseCategory.FOOD,
            Money.of(new BigDecimal("45.00"), "PEN"),
            LocalDate.now(),
            "KEY-AMQP-01",
            null,
            null,
            Instant.now());
    expenseRepository.save(expense);
    assertThat(expense.isLocked()).isFalse();

    ExpenseSettlementMessaging.SettlementClosedEvent event =
        new ExpenseSettlementMessaging.SettlementClosedEvent(
            eventId, Instant.now(), tenantId, tripId, UUID.randomUUID());

    // Solo se cuentan las llamadas del consumidor, sin el guardado de la preparación.
    clearInvocations(expenseRepository);

    // Primera recepción: consulta los gastos del viaje, los bloquea y los guarda una vez
    messaging.receive(event, null);

    Expense locked = expenseRepository.findById(expense.getId()).orElseThrow();
    assertThat(locked.isLocked()).isTrue();
    verify(expenseRepository, times(1)).findByTenantIdAndTripId(tenantId, tripId);
    verify(expenseRepository, times(1)).save(any(Expense.class));

    // Segunda recepción (duplicado de red): se descarta antes de consultar ni guardar
    messaging.receive(event, null);

    Expense stillLocked = expenseRepository.findById(expense.getId()).orElseThrow();
    assertThat(stillLocked.isLocked()).isTrue();
    verify(expenseRepository, times(1)).findByTenantIdAndTripId(tenantId, tripId);
    verify(expenseRepository, times(1)).save(any(Expense.class));
    assertThat(jdbc.queryForObject(
            "select count(*) from expense.processed_settlement_events where event_id = ?",
            Long.class,
            eventId))
        .isEqualTo(1L);
  }
}
