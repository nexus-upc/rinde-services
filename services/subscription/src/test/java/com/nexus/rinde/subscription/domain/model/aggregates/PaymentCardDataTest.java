package com.nexus.rinde.subscription.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.rinde.support.AbstractIntegrationTest;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Comprueba NF06 (sin datos de tarjeta) y la unicidad del evento de pago de NF05. */
class PaymentCardDataTest extends AbstractIntegrationTest {

  /** Fragmentos de nombre que delatan datos de tarjeta; se comparan en minúsculas. */
  private static final List<String> CARD_TERMS =
      List.of("card", "pan", "cvv", "cvc", "expir", "cardholder");

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void paymentEntityDeclaresNoCardFields() {
    List<String> fields =
        Arrays.stream(Payment.class.getDeclaredFields()).map(Field::getName).toList();

    assertThat(fields).contains("providerReference", "amount", "createdAt");
    assertThat(fields).noneMatch(PaymentCardDataTest::namesCardData);
  }

  @Test
  void paymentsTableHasNoCardColumns() {
    List<String> columns =
        jdbcTemplate.queryForList(
            "select column_name from information_schema.columns"
                + " where table_schema = 'subscription' and table_name = 'payments'",
            String.class);

    assertThat(columns).contains("provider_reference", "amount", "created_at");
    assertThat(columns).noneMatch(PaymentCardDataTest::namesCardData);
  }

  @Test
  void databaseRejectsASecondPaymentWithTheSameProviderEventId() {
    UUID tenantId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    UUID subscriptionId = UUID.randomUUID();
    jdbcTemplate.update(
        "insert into subscription.plans (plan_id, name, monthly_price, unit_limit, active)"
            + " values (?, ?, ?, ?, ?)",
        planId,
        "Plan-Unicidad",
        new BigDecimal("10.00"),
        1,
        true);
    jdbcTemplate.update(
        "insert into subscription.subscriptions (subscription_id, tenant_id, plan_id, status)"
            + " values (?, ?, ?, ?)",
        subscriptionId,
        tenantId,
        planId,
        "PENDING_PAYMENT");
    insertPayment(tenantId, subscriptionId, "evento-unico-1", "referencia-1");

    // Las referencias del proveedor son distintas: solo el evento debe provocar el choque.
    assertThatThrownBy(
            () -> insertPayment(tenantId, subscriptionId, "evento-unico-1", "referencia-2"))
        .as("Un segundo pago con el mismo provider_event_id debe fallar")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private void insertPayment(
      UUID tenantId, UUID subscriptionId, String providerEventId, String providerReference) {
    jdbcTemplate.update(
        "insert into subscription.payments"
            + " (payment_id, tenant_id, subscription_id, provider_event_id, provider_reference,"
            + " amount, status, created_at)"
            + " values (?, ?, ?, ?, ?, ?, 'CONFIRMED', CURRENT_TIMESTAMP)",
        UUID.randomUUID(),
        tenantId,
        subscriptionId,
        providerEventId,
        providerReference,
        new BigDecimal("10.00"));
  }

  private static boolean namesCardData(String name) {
    String lower = name.toLowerCase(Locale.ROOT);
    return CARD_TERMS.stream().anyMatch(lower::contains);
  }
}
