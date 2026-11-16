package com.nexus.rinde.iam.demorequest.infrastructure;

import com.nexus.rinde.iam.demorequest.domain.DemoRequest;
import com.nexus.rinde.iam.demorequest.domain.DemoRequestStatus;
import com.nexus.rinde.shared.interfaces.rest.PageResource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DemoRequestRepository {

  private static final RowMapper<DemoRequest> ROW =
      (rs, index) ->
          new DemoRequest(
              rs.getObject("id", UUID.class),
              rs.getString("full_name"),
              rs.getString("company_name"),
              rs.getString("email"),
              rs.getString("phone"),
              rs.getInt("fleet_size"),
              rs.getString("message"),
              DemoRequestStatus.valueOf(rs.getString("status")),
              instant(rs, "received_at"));

  private final JdbcTemplate jdbc;

  public DemoRequestRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Toma un bloqueo de transacción por correo; se libera al confirmar o revertir. */
  public void lockEmail(String email) {
    jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?, 0))", email);
  }

  public boolean existsReceivedSince(String email, Instant since) {
    Boolean exists =
        jdbc.queryForObject(
            "select exists(select 1 from iam.demo_requests where email = ? and received_at >= ?)",
            Boolean.class,
            email,
            Timestamp.from(since));
    return Boolean.TRUE.equals(exists);
  }

  public void insert(DemoRequest request) {
    jdbc.update(
        "insert into iam.demo_requests(id,full_name,company_name,email,phone,fleet_size,message,status,received_at)"
            + " values(?,?,?,?,?,?,?,?,?)",
        request.id(),
        request.fullName(),
        request.companyName(),
        request.email(),
        request.phone(),
        request.fleetSize(),
        request.message(),
        request.status().name(),
        Timestamp.from(request.receivedAt()));
  }

  public PageResource<DemoRequest> list(int page, int size) {
    long total = jdbc.queryForObject("select count(*) from iam.demo_requests", Long.class);
    List<DemoRequest> items =
        jdbc.query(
            "select * from iam.demo_requests order by received_at desc, id desc limit ? offset ?",
            ROW,
            size,
            (long) page * size);
    return new PageResource<>(List.copyOf(items), page, size, total);
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
