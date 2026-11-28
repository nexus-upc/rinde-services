package com.nexus.rinde.trip.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifica la deduplicación ante concurrencia y el rechazo de fechas futuras en ubicaciones. */
class TripLocationsIntegrationTest extends AbstractIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ignoresDuplicateInsertedDirectlyBeforeBatchRequest() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID adminUserId = UUID.randomUUID();
    UUID driverUserId = UUID.randomUUID();
    UUID vehicleId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    String adminToken = tokens.issue(adminUserId, tenantId, "ADMINISTRATOR", "ana@andes.pe");
    String driverToken = tokens.issue(driverUserId, tenantId, "DRIVER", "luis@andes.pe");

    fleetStub.registerVehicle(vehicleId);
    fleetStub.registerDriver(driverId, driverUserId);

    String tripJson =
        api.schedule(
                adminToken, "Lima", "Trujillo", "Repuestos", null, LocalDate.now().plusDays(1), false)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String tripId = objectMapper.readTree(tripJson).get("id").asText();

    api.assign(adminToken, tripId, vehicleId.toString(), driverId.toString(), false)
        .andExpect(status().isOk());
    api.start(driverToken, tripId).andExpect(status().isOk());

    Instant existingRecordedAt = Instant.now().minusSeconds(600).truncatedTo(ChronoUnit.MICROS);
    Instant freshRecordedAt = Instant.now().minusSeconds(300).truncatedTo(ChronoUnit.MICROS);

    // Simula otra petición en paralelo que ya confirmó la fila antes de este lote.
    jdbcTemplate.update(
        "insert into trip.trip_locations "
            + "(id, tenant_id, trip_id, latitude, longitude, accuracy_meters, recorded_at, received_at) "
            + "values (?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        tenantId,
        UUID.fromString(tripId),
        new BigDecimal("-12.046374"),
        new BigDecimal("-77.042793"),
        new BigDecimal("12.50"),
        Timestamp.valueOf(LocalDateTime.ofInstant(existingRecordedAt, ZoneOffset.UTC)),
        Timestamp.valueOf(
            LocalDateTime.ofInstant(
                Instant.now().truncatedTo(ChronoUnit.MICROS), ZoneOffset.UTC)));

    Map<String, Object> pointExisting =
        point(-12.046374, -77.042793, 12.50, existingRecordedAt.toString());
    Map<String, Object> pointFresh =
        point(-11.981234, -77.061111, null, freshRecordedAt.toString());
    Map<String, Object> pointFreshDuplicate =
        point(-11.981234, -77.061111, null, freshRecordedAt.toString());

    api.recordLocations(
            driverToken,
            tripId,
            Map.of("points", List.of(pointExisting, pointFresh, pointFreshDuplicate)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.savedCount").value(1))
        .andExpect(jsonPath("$.duplicateCount").value(2));

    Integer rows =
        jdbcTemplate.queryForObject(
            "select count(*) from trip.trip_locations where trip_id = ?",
            Integer.class,
            UUID.fromString(tripId));
    assertThat(rows).isEqualTo(2);
  }

  @Test
  void rejectsFutureTimestampBeyondFiveMinutesWithFieldError() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID driverUserId = UUID.randomUUID();
    String driverToken = tokens.issue(driverUserId, tenantId, "DRIVER", "luis@andes.pe");
    String futureRecordedAt = Instant.now().plusSeconds(360).truncatedTo(ChronoUnit.SECONDS).toString();

    api.recordLocations(
            driverToken,
            UUID.randomUUID().toString(),
            Map.of("points", List.of(point(-12.046374, -77.042793, 10.00, futureRecordedAt))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("La solicitud tiene datos inválidos."))
        .andExpect(
            jsonPath("$.errors['points[0].recordedAt']")
                .value("La hora del punto no puede estar en el futuro."));
  }

  @Test
  void acceptsTimestampWithinFiveMinuteClockSkewTolerance() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID driverUserId = UUID.randomUUID();
    UUID vehicleId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    String adminToken = tokens.issue(UUID.randomUUID(), tenantId, "ADMINISTRATOR", "ana@andes.pe");
    String driverToken = tokens.issue(driverUserId, tenantId, "DRIVER", "luis@andes.pe");

    fleetStub.registerVehicle(vehicleId);
    fleetStub.registerDriver(driverId, driverUserId);

    String tripJson =
        api.schedule(
                adminToken, "Lima", "Ica", "Cajas", null, LocalDate.now().plusDays(1), false)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String tripId = objectMapper.readTree(tripJson).get("id").asText();

    api.assign(adminToken, tripId, vehicleId.toString(), driverId.toString(), false)
        .andExpect(status().isOk());
    api.start(driverToken, tripId).andExpect(status().isOk());

    String toleratedFuture = Instant.now().plusSeconds(240).truncatedTo(ChronoUnit.SECONDS).toString();
    api.recordLocations(
            driverToken,
            tripId,
            Map.of("points", List.of(point(-12.046374, -77.042793, 10.00, toleratedFuture))))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.savedCount").value(1))
        .andExpect(jsonPath("$.duplicateCount").value(0));
  }

  private static Map<String, Object> point(
      double latitude, double longitude, Double accuracyMeters, String recordedAt) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("latitude", latitude);
    map.put("longitude", longitude);
    if (accuracyMeters != null) {
      map.put("accuracyMeters", accuracyMeters);
    }
    map.put("recordedAt", recordedAt);
    return map;
  }
}
