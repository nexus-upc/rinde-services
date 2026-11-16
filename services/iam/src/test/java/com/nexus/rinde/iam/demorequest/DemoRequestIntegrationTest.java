package com.nexus.rinde.iam.demorequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexus.rinde.shared.infrastructure.security.JwtClaims;
import com.nexus.rinde.support.AbstractIntegrationTest;
import io.jsonwebtoken.Jwts;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Flujo completo de US06 en IAM: envío público, listado del dueño y control de acceso. */
class DemoRequestIntegrationTest extends AbstractIntegrationTest {

  private static final String OWNER_KEY = "demo-request-owner-test-key-at-least-32-characters";

  @Autowired JdbcTemplate jdbc;

  @DynamicPropertySource
  static void platform(DynamicPropertyRegistry registry) {
    registry.add("rinde.platform.jwt.secret", () -> OWNER_KEY);
    registry.add("rinde.platform.bootstrap.enabled", () -> false);
  }

  @Test
  void acceptsTheFormWithoutTokenAndStoresNormalizedValues() throws Exception {
    Map<String, Object> body = form("Ana@Transportes.pe");
    body.put("phone", "");
    body.put("message", "   ");

    mockMvc
        .perform(
            post("/api/v1/public/demo-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("RECEIVED"))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.receivedAt").isNotEmpty());

    Map<String, Object> row =
        jdbc.queryForMap("select email, phone, message from iam.demo_requests");
    assertThat(row.get("email")).isEqualTo("ana@transportes.pe");
    assertThat(row.get("phone")).isNull();
    assertThat(row.get("message")).isNull();
  }

  @Test
  void listsRequestsNewestFirstWithPagination() throws Exception {
    submit("primera@ejemplo.pe");
    submit("segunda@ejemplo.pe");
    submit("tercera@ejemplo.pe");
    setReceivedAt("primera@ejemplo.pe", "2026-11-01T10:00:00Z");
    setReceivedAt("segunda@ejemplo.pe", "2026-11-02T10:00:00Z");
    setReceivedAt("tercera@ejemplo.pe", "2026-11-03T10:00:00Z");

    JsonNode firstPage = page(0, 2);
    assertThat(firstPage.get("total").asLong()).isEqualTo(3);
    assertThat(emails(firstPage))
        .containsExactly("tercera@ejemplo.pe", "segunda@ejemplo.pe");

    JsonNode secondPage = page(1, 2);
    assertThat(emails(secondPage)).containsExactly("primera@ejemplo.pe");
  }

  @Test
  void keepsTheListingBehindThePlatformOwner() throws Exception {
    mockMvc
        .perform(get("/api/v1/platform/demo-requests"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            get("/api/v1/platform/demo-requests")
                .header("Authorization", "Bearer " + owner())
                .param("size", "1000"))
        .andExpect(status().isBadRequest());
  }

  private void submit(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/public/demo-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form(email))))
        .andExpect(status().isCreated());
  }

  private JsonNode page(int page, int size) throws Exception {
    String body =
        mockMvc
            .perform(
                get("/api/v1/platform/demo-requests")
                    .header("Authorization", "Bearer " + owner())
                    .param("page", String.valueOf(page))
                    .param("size", String.valueOf(size)))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body);
  }

  private void setReceivedAt(String email, String instant) {
    jdbc.update(
        "update iam.demo_requests set received_at = ? where email = ?",
        Timestamp.from(Instant.parse(instant)),
        email);
  }

  private static List<String> emails(JsonNode page) {
    List<String> result = new ArrayList<>();
    page.get("items").forEach(item -> result.add(item.get("email").asText()));
    return result;
  }

  private static Map<String, Object> form(String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("fullName", "Ana Rojas Paredes");
    body.put("companyName", "Transportes Andes SAC");
    body.put("email", email);
    body.put("fleetSize", 12);
    return body;
  }

  private static String owner() {
    return Jwts.builder()
        .subject(UUID.randomUUID().toString())
        .issuer("rinde-platform")
        .audience()
        .add("rinde-platform-api")
        .and()
        .claim("scope", "PLATFORM")
        .claim("role", "PLATFORM_OWNER")
        .claim("email", "owner@example.invalid")
        .expiration(Date.from(Instant.now().plusSeconds(300)))
        .signWith(JwtClaims.signingKey(OWNER_KEY))
        .compact();
  }
}
