package com.nexus.rinde.support;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;

/** Apunta las pruebas a la base rinde_settlement_test y vacía las tablas entre pruebas. */
@Component
public class TestDatabase {

  private static final String TEST_URL = "jdbc:postgresql://localhost:5432/rinde_settlement_test";

  private final JdbcTemplate jdbcTemplate;

  public TestDatabase(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public static void registerUrl(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> TEST_URL);
  }

  public void clean() {
    List<String> tables =
        jdbcTemplate.queryForList(
            "select quote_ident(table_schema) || '.' || quote_ident(table_name)"
                + " from information_schema.tables"
                + " where table_type = 'BASE TABLE'"
                + " and table_schema not in ('pg_catalog', 'information_schema', 'public')"
                + " and table_name <> 'flyway_schema_history'",
            String.class);
    if (!tables.isEmpty()) {
      jdbcTemplate.execute(
          "TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
    }
  }
}
