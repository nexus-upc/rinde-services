package com.nexus.rinde.expense.application.internal.queryservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.queries.ListExpensesQuery;
import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseCategory;
import com.nexus.rinde.expense.domain.model.valueobjects.Money;
import com.nexus.rinde.expense.domain.model.valueobjects.PagedExpenses;
import com.nexus.rinde.expense.domain.services.EvidenceStorage;
import com.nexus.rinde.expense.infrastructure.persistence.jpa.repositories.ExpenseRepository;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/** Pruebas unitarias de validaciones y paginación de ExpenseQueryServiceImpl para S4-12 (C2). */
class ExpenseQueryServiceImplTest {

  private ExpenseRepository expenseRepository;
  private ExpenseQueryServiceImpl service;
  private UUID tenantId;

  @BeforeEach
  void setUp() {
    expenseRepository = mock(ExpenseRepository.class);
    EvidenceStorage evidenceStorage = mock(EvidenceStorage.class);
    service = new ExpenseQueryServiceImpl(expenseRepository, evidenceStorage);
    tenantId = UUID.randomUUID();
  }

  @Test
  void rejectsNegativePageNumber() {
    ListExpensesQuery query =
        new ListExpensesQuery(tenantId, null, null, null, null, null, -1, 20);

    assertThatThrownBy(() -> service.handle(query))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El número de página no puede ser negativo.");
  }

  @Test
  void rejectsPageSizeOutsideBounds() {
    ListExpensesQuery zeroSize =
        new ListExpensesQuery(tenantId, null, null, null, null, null, 0, 0);
    ListExpensesQuery overMaxSize =
        new ListExpensesQuery(tenantId, null, null, null, null, null, 0, 101);

    assertThatThrownBy(() -> service.handle(zeroSize))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El tamaño de página debe estar entre 1 y 100.");
    assertThatThrownBy(() -> service.handle(overMaxSize))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El tamaño de página debe estar entre 1 y 100.");
  }

  @Test
  void rejectsInvalidDateFormatsAndInvertedRange() {
    ListExpensesQuery badFrom =
        new ListExpensesQuery(tenantId, null, null, null, "2026/11/01", null, 0, 20);
    ListExpensesQuery badTo =
        new ListExpensesQuery(tenantId, null, null, null, "2026-11-01", "2026-2-1", 0, 20);
    ListExpensesQuery inverted =
        new ListExpensesQuery(tenantId, null, null, null, "2026-11-20", "2026-11-10", 0, 20);

    assertThatThrownBy(() -> service.handle(badFrom))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La fecha de inicio debe tener el formato AAAA-MM-DD.");
    assertThatThrownBy(() -> service.handle(badTo))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La fecha de fin debe tener el formato AAAA-MM-DD.");
    assertThatThrownBy(() -> service.handle(inverted))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La fecha final no puede ser anterior a la inicial.");
  }

  @Test
  void rejectsUnknownOrEmptyStatusValues() {
    ListExpensesQuery unknown =
        new ListExpensesQuery(tenantId, "REGISTERED,UNKNOWN", null, null, null, null, 0, 20);
    ListExpensesQuery trailingComma =
        new ListExpensesQuery(tenantId, "REGISTERED,", null, null, null, null, 0, 20);

    assertThatThrownBy(() -> service.handle(unknown))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El parámetro status contiene un estado de gasto inválido.");
    assertThatThrownBy(() -> service.handle(trailingComma))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El parámetro status contiene un estado de gasto inválido.");
  }

  @Test
  @SuppressWarnings("unchecked")
  void delegatesFilteredPageOrderedByExpenseDateDescThenCreatedAtDesc() {
    Expense sample =
        Expense.create(
            tenantId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            ExpenseCategory.FUEL,
            new Money(new BigDecimal("180.50"), "PEN"),
            LocalDate.of(2026, 11, 20),
            "KEY-UNIT-01");
    when(expenseRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenAnswer(
            inv -> {
              Pageable pageable = inv.getArgument(1);
              return new PageImpl<>(List.of(sample), pageable, 57);
            });

    ListExpensesQuery query =
        new ListExpensesQuery(
            tenantId,
            "REGISTERED,PENDING_SUPPORT",
            sample.getTripId(),
            sample.getDriverId(),
            "2026-11-01",
            "2026-11-30",
            0,
            20);

    PagedExpenses result = service.handle(query);

    assertThat(result.items()).containsExactly(sample);
    assertThat(result.page()).isEqualTo(0);
    assertThat(result.size()).isEqualTo(20);
    assertThat(result.totalItems()).isEqualTo(57L);
    assertThat(result.totalPages()).isEqualTo(3);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(expenseRepository).findAll(any(Specification.class), pageableCaptor.capture());
    Sort sort = pageableCaptor.getValue().getSort();
    assertThat(sort.getOrderFor("expenseDate"))
        .isNotNull()
        .extracting(Sort.Order::getDirection)
        .isEqualTo(Sort.Direction.DESC);
    assertThat(sort.getOrderFor("createdAt"))
        .isNotNull()
        .extracting(Sort.Order::getDirection)
        .isEqualTo(Sort.Direction.DESC);
  }
}
