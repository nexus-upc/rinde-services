package com.nexus.rinde.expense.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.rinde.expense.domain.model.valueobjects.EvidenceFile;
import com.nexus.rinde.expense.domain.model.valueobjects.EvidenceFormat;
import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseCategory;
import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseStatus;
import com.nexus.rinde.expense.domain.model.valueobjects.Money;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExpenseTest {

  private static final long MAX_SIZE = 5L * 1024 * 1024;

  @Test
  @DisplayName("Un gasto recién creado queda pendiente de sustento")
  void shouldStartPendingSupportWithoutEvidence() {
    Expense expense = newExpense("KEY-TEST-00");

    assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.PENDING_SUPPORT);
    assertThat(expense.getEvidence()).isNull();
  }

  @Test
  @DisplayName("Adjuntar el comprobante deja el gasto en REGISTERED")
  void shouldRegisterExpenseWhenEvidenceIsAttached() {
    Expense expense = newExpense("KEY-TEST-01");
    UUID driverId = expense.getDriverId();

    String previousKey =
        expense.attachEvidence(jpeg("recibo.jpg"), "tenant/expense/a", driverId, Instant.now());

    assertThat(previousKey).isNull();
    assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.REGISTERED);
    assertThat(expense.getEvidence().getFileName()).isEqualTo("recibo.jpg");
    assertThat(expense.getEvidence().getStorageKey()).isEqualTo("tenant/expense/a");
    assertThat(expense.getEvidence().getUploadedBy()).isEqualTo(driverId);
  }

  @Test
  @DisplayName("Reemplazar el comprobante conserva su identificador y devuelve la clave anterior")
  void shouldReturnPreviousKeyWhenEvidenceIsReplaced() {
    Expense expense = newExpense("KEY-TEST-02");
    UUID evidenceId;
    expense.attachEvidence(jpeg("borroso.jpg"), "tenant/expense/old", UUID.randomUUID(), null);
    evidenceId = expense.getEvidence().getId();

    String previousKey =
        expense.attachEvidence(jpeg("legible.jpg"), "tenant/expense/new", UUID.randomUUID(), null);

    assertThat(previousKey).isEqualTo("tenant/expense/old");
    assertThat(expense.getEvidence().getId()).isEqualTo(evidenceId);
    assertThat(expense.getEvidence().getFileName()).isEqualTo("legible.jpg");
    assertThat(expense.getEvidence().getStorageKey()).isEqualTo("tenant/expense/new");
    assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.REGISTERED);
  }

  @Test
  @DisplayName("Un gasto bloqueado por liquidación no acepta comprobantes")
  void shouldRejectEvidenceWhenLocked() {
    Expense expense = newExpense("KEY-TEST-03");
    expense.lock();

    assertThatThrownBy(
            () -> expense.attachEvidence(jpeg("tarde.jpg"), "tenant/expense/x", UUID.randomUUID(), null))
        .isInstanceOf(ConflictException.class);
    assertThat(expense.getEvidence()).isNull();
  }

  @Test
  @DisplayName("Un gasto registrado puede ser aprobado")
  void shouldApproveRegisteredExpense() {
    Expense expense = newExpense("KEY-TEST-04");
    expense.attachEvidence(jpeg("recibo.jpg"), "tenant/expense/b", UUID.randomUUID(), null);

    assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.REGISTERED);
    expense.approve(UUID.randomUUID(), Instant.now());
    assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.APPROVED);
  }

  @Test
  @DisplayName("Un gasto puede ser observado indicando motivo")
  void shouldObserveExpenseWithReason() {
    Expense expense = newExpense("KEY-TEST-05");

    expense.observe(UUID.randomUUID(), "Comprobante borroso", Instant.now());
    assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.OBSERVED);
    assertThat(expense.getObservationReason()).isEqualTo("Comprobante borroso");
  }

  @Test
  @DisplayName("No se puede observar un gasto sin motivo")
  void shouldRejectObservationWithoutReason() {
    Expense expense = newExpense("KEY-TEST-06");

    assertThatThrownBy(() -> expense.observe(UUID.randomUUID(), null, Instant.now()))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> expense.observe(UUID.randomUUID(), "   ", Instant.now()))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  @DisplayName("Un gasto bloqueado no puede ser modificado")
  void shouldNotModifyLockedExpense() {
    Expense expense = newExpense("KEY-TEST-07");

    expense.lock();
    assertThat(expense.isLocked()).isTrue();
    assertThatThrownBy(() -> expense.approve(UUID.randomUUID(), Instant.now()))
        .isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> expense.observe(UUID.randomUUID(), "Motivo", Instant.now()))
        .isInstanceOf(ConflictException.class);
  }

  private static Expense newExpense(String idempotencyKey) {
    return Expense.create(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        ExpenseCategory.FUEL,
        Money.of(new BigDecimal("120.00"), "PEN"),
        LocalDate.now(),
        idempotencyKey);
  }

  private static EvidenceFile jpeg(String fileName) {
    byte[] content = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};
    return EvidenceFile.inspect(fileName, content, MAX_SIZE);
  }
}
