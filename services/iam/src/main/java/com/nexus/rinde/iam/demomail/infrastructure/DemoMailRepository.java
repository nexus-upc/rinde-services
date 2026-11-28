package com.nexus.rinde.iam.demomail.infrastructure;

import com.nexus.rinde.iam.demomail.domain.DemoMail;
import com.nexus.rinde.iam.domain.model.valueobjects.TokenPurpose;
import com.nexus.rinde.shared.interfaces.rest.PageResource;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "rinde.demo.mailbox.enabled", havingValue = "true")
@ConditionalOnProperty(name = "rinde.mail.provider", havingValue = "demo", matchIfMissing = true)
public class DemoMailRepository {
  private final JdbcTemplate jdbc;
  private static final String SELECT =
      "select m.*,t.token_hash as existing_token,t.used_at,u.status as account_status from"
          + " iam.demo_mail_messages m left join iam.access_tokens t on t.tenant_id=m.tenant_id and"
          + " t.token_hash=m.token_hash left join iam.users u on u.id=t.user_id ";
  private static final RowMapper<DemoMail> ROW =
      (rs, index) ->
          new DemoMail(
              rs.getObject("id", UUID.class),
              rs.getObject("event_id", UUID.class),
              rs.getObject("tenant_id", UUID.class),
              rs.getString("recipient"),
              rs.getString("recipient_name"),
              rs.getString("company_name"),
              TokenPurpose.valueOf(rs.getString("purpose")),
              rs.getString("encrypted_token"),
              instant(rs, "created_at"),
              instant(rs, "expires_at"),
              instant(rs, "read_at"),
              rs.getString("existing_token") != null
                  && !"DISABLED".equals(rs.getString("account_status")),
              instant(rs, "used_at"));

  public DemoMailRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void append(
      UUID eventId,
      UUID tenantId,
      String recipient,
      String name,
      String company,
      TokenPurpose purpose,
      String tokenHash,
      String encryptedToken,
      Instant created,
      Instant expires) {
    jdbc.update(
        "insert into"
            + " iam.demo_mail_messages(id,event_id,tenant_id,recipient,recipient_name,company_name,purpose,token_hash,encrypted_token,created_at,expires_at)"
            + " values(?,?,?,?,?,?,?,?,?,?,?) on conflict(event_id) do nothing",
        UUID.randomUUID(),
        eventId,
        tenantId,
        recipient,
        name,
        company,
        purpose.name(),
        tokenHash,
        encryptedToken,
        Timestamp.from(created),
        Timestamp.from(expires));
  }

  public PageResource<DemoMail> list(String query, TokenPurpose purpose, int page, int size) {
    String filter =
        " where (lower(m.recipient) like ? escape '!' or lower(m.company_name) like ? escape '!') "
            + "and (?::text is null or m.purpose=?::text)";
    String search =
        "%"
            + query
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
            + "%";
    String kind = purpose == null ? null : purpose.name();
    long total =
        jdbc.queryForObject(
            "select count(*) from iam.demo_mail_messages m" + filter,
            Long.class,
            search,
            search,
            kind,
            kind);
    List<DemoMail> items =
        jdbc.query(
            SELECT + filter + " order by m.created_at desc,m.id desc limit ? offset ?",
            ROW,
            search,
            search,
            kind,
            kind,
            size,
            (long) page * size);
    return new PageResource<>(List.copyOf(items), page, size, total);
  }

  public Optional<DemoMail> find(UUID id) {
    return jdbc.query(SELECT + "where m.id=?", ROW, id).stream().findFirst();
  }

  public void markRead(UUID id, Instant now) {
    jdbc.update(
        "update iam.demo_mail_messages set read_at=coalesce(read_at,?) where id=?",
        Timestamp.from(now),
        id);
  }

  private static Instant instant(ResultSet rs, String name) throws SQLException {
    Timestamp value = rs.getTimestamp(name);
    return value == null ? null : value.toInstant();
  }
}
