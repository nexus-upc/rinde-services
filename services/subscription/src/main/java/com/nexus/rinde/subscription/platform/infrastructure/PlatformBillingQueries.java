package com.nexus.rinde.subscription.platform.infrastructure;

import com.nexus.rinde.shared.interfaces.rest.PageResource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Proyecciones de lectura sobre el esquema de suscripciones del propio servicio. */
@Repository
public class PlatformBillingQueries {
  private final JdbcTemplate jdbc;
  private final PlatformCompanyDirectory companies;

  public PlatformBillingQueries(JdbcTemplate jdbc, PlatformCompanyDirectory companies) {
    this.jdbc = jdbc;
    this.companies = companies;
  }

  public Overview overview() {
    return new Overview(
        count("select count(*) from subscription.plans"),
        count("select count(*) from subscription.plans where active=true"),
        count("select count(*) from subscription.subscriptions"),
        count("select count(*) from subscription.subscriptions where status='PENDING_PAYMENT'"),
        count("select count(*) from subscription.payments where status='CONFIRMED'"),
        jdbc.queryForObject(
            "select coalesce(sum(amount),0) from subscription.payments where status='CONFIRMED'",
            BigDecimal.class),
        "PEN",
        "SIMULATED");
  }

  public PageResource<SubscriptionRow> subscriptions(UUID tenant, int page, int size) {
    String filter = " where (?::uuid is null or s.tenant_id=?::uuid)";
    long total =
        jdbc.queryForObject(
            "select count(*) from subscription.subscriptions s" + filter,
            Long.class,
            tenant,
            tenant);
    var rows =
        jdbc.query(
            "select s.*,p.name,p.monthly_price from subscription.subscriptions s join"
                + " subscription.plans p on p.plan_id=s.plan_id"
                + filter
                + " order by s.subscription_id limit ? offset ?",
            (rs, i) ->
                new SubscriptionRow(
                    rs.getObject("subscription_id", UUID.class),
                    rs.getObject("tenant_id", UUID.class),
                    null,
                    rs.getObject("plan_id", UUID.class),
                    rs.getString("name"),
                    rs.getString("status"),
                    instant(rs, "starts_at"),
                    instant(rs, "expires_at"),
                    rs.getBigDecimal("monthly_price")),
            tenant,
            tenant,
            size,
            (long) page * size);
    var names = companies.names(rows.stream().map(SubscriptionRow::tenantId).distinct().toList());
    return new PageResource<>(
        rows.stream()
            .map(
                r ->
                    new SubscriptionRow(
                        r.id(),
                        r.tenantId(),
                        names.getOrDefault(r.tenantId(), "Empresa no disponible"),
                        r.planId(),
                        r.planName(),
                        r.status(),
                        r.startsAt(),
                        r.expiresAt(),
                        r.monthlyPrice()))
            .toList(),
        page,
        size,
        total);
  }

  public PageResource<PaymentRow> payments(UUID tenant, int page, int size) {
    String filter = " where (?::uuid is null or p.tenant_id=?::uuid)";
    long total =
        jdbc.queryForObject(
            "select count(*) from subscription.payments p" + filter, Long.class, tenant, tenant);
    var rows =
        jdbc.query(
            "select p.*,pl.name from subscription.payments p join subscription.subscriptions s on"
                + " s.subscription_id=p.subscription_id join subscription.plans pl on"
                + " pl.plan_id=s.plan_id"
                + filter
                + " order by p.created_at desc,p.payment_id desc limit ? offset ?",
            (rs, i) ->
                new PaymentRow(
                    rs.getObject("payment_id", UUID.class),
                    rs.getObject("tenant_id", UUID.class),
                    null,
                    rs.getObject("subscription_id", UUID.class),
                    rs.getString("name"),
                    rs.getBigDecimal("amount"),
                    rs.getString("status"),
                    instant(rs, "created_at"),
                    rs.getString("provider_reference")),
            tenant,
            tenant,
            size,
            (long) page * size);
    var names = companies.names(rows.stream().map(PaymentRow::tenantId).distinct().toList());
    return new PageResource<>(
        rows.stream()
            .map(
                r ->
                    new PaymentRow(
                        r.id(),
                        r.tenantId(),
                        names.getOrDefault(r.tenantId(), "Empresa no disponible"),
                        r.subscriptionId(),
                        r.planName(),
                        r.amount(),
                        r.status(),
                        r.createdAt(),
                        r.reference()))
            .toList(),
        page,
        size,
        total);
  }

  private long count(String sql) {
    return jdbc.queryForObject(sql, Long.class);
  }

  private static Instant instant(ResultSet rs, String field) throws SQLException {
    Timestamp value = rs.getTimestamp(field);
    return value == null ? null : value.toInstant();
  }

  public record Overview(
      long plans,
      long activePlans,
      long subscriptions,
      long pendingSubscriptions,
      long confirmedPayments,
      BigDecimal confirmedAmount,
      String currency,
      String paymentMode) {}

  public record SubscriptionRow(
      UUID id,
      UUID tenantId,
      String companyName,
      UUID planId,
      String planName,
      String status,
      Instant startsAt,
      Instant expiresAt,
      BigDecimal monthlyPrice) {}

  public record PaymentRow(
      UUID id,
      UUID tenantId,
      String companyName,
      UUID subscriptionId,
      String planName,
      BigDecimal amount,
      String status,
      Instant createdAt,
      String reference) {}
}
