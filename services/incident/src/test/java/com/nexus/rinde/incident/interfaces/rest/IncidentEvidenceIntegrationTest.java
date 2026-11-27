package com.nexus.rinde.incident.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexus.rinde.support.AbstractIntegrationTest;
import com.nexus.rinde.support.FleetStub;
import com.nexus.rinde.support.TestIncidentEvidenceStorage;
import com.nexus.rinde.support.TripStub;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

/** Pruebas de integración del contrato C4 en Incident (subida, reemplazo, lectura y errores). */
class IncidentEvidenceIntegrationTest extends AbstractIntegrationTest {

  private static final byte[] JPEG_BYTES =
      new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x10, 0x20, 0x30};
  private static final byte[] PNG_BYTES =
      new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x55};

  @Autowired private TripStub tripStub;
  @Autowired private FleetStub fleetStub;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void uploadsReplacesDeletesPreviousFileAndServesAuthorizedBinaryContent() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID reporterUserId = UUID.randomUUID();
    UUID otherDriverUserId = UUID.randomUUID();
    UUID driverFleetId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    UUID vehicleId = UUID.randomUUID();

    fleetStub.setDriverForUser(tenantId, reporterUserId, driverFleetId);
    tripStub.setTrip(tenantId, tripId, "IN_ROUTE", vehicleId, driverFleetId);

    String reporterToken = tokens.issue(reporterUserId, tenantId, "DRIVER", "luis@andes.pe");
    String otherDriverToken = tokens.issue(otherDriverUserId, tenantId, "DRIVER", "marta@andes.pe");
    String opsToken =
        tokens.issue(UUID.randomUUID(), tenantId, "OPERATIONS_MANAGER", "ops@andes.pe");

    String incidentId = createIncident(reporterToken, tripId);

    MockMultipartFile firstPhoto =
        new MockMultipartFile("file", "freno-roto.jpg", "image/jpeg", JPEG_BYTES);
    String uploadJson =
        mockMvc
            .perform(
                multipart("/api/v1/incidents/{id}/evidences", incidentId)
                    .file(firstPhoto)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterToken))
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.evidence.fileName").value("freno-roto.jpg"))
            .andExpect(jsonPath("$.evidence.contentType").value("image/jpeg"))
            .andExpect(jsonPath("$.evidence.fileSizeBytes").value(JPEG_BYTES.length))
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode uploaded = objectMapper.readTree(uploadJson).get("evidence");
    String evidenceId = uploaded.get("id").asText();
    String contentUrl = uploaded.get("contentUrl").asText();

    String firstStorageKey =
        jdbcTemplate.queryForObject(
            "SELECT storage_key FROM incident.incident_evidences WHERE id = ?::uuid",
            String.class,
            evidenceId);
    Path firstStoredFile = TestIncidentEvidenceStorage.directory().resolve(firstStorageKey);
    assertThat(Files.exists(firstStoredFile)).isTrue();

    mockMvc
        .perform(get(contentUrl).header(HttpHeaders.AUTHORIZATION, "Bearer " + opsToken))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_JPEG))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(content().bytes(JPEG_BYTES));

    mockMvc
        .perform(get(contentUrl).header(HttpHeaders.AUTHORIZATION, "Bearer " + otherDriverToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.detail").value("No tiene permiso para ver esta evidencia."));

    MockMultipartFile replacementPng =
        new MockMultipartFile("file", "freno-reparado.png", "image/png", PNG_BYTES);
    mockMvc
        .perform(
            multipart("/api/v1/incidents/{id}/evidences", incidentId)
                .file(replacementPng)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.evidence.id").value(evidenceId))
        .andExpect(jsonPath("$.evidence.fileName").value("freno-reparado.png"))
        .andExpect(jsonPath("$.evidence.contentType").value("image/png"));

    assertThat(Files.exists(firstStoredFile)).isFalse();

    mockMvc
        .perform(get(contentUrl).header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterToken))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(PNG_BYTES));
  }

  @Test
  void enforcesUploadValidationsMaxSizeMissingFileAndSha256Integrity() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID reporterUserId = UUID.randomUUID();
    UUID driverFleetId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();

    fleetStub.setDriverForUser(tenantId, reporterUserId, driverFleetId);
    tripStub.setTrip(tenantId, tripId, "IN_ROUTE", UUID.randomUUID(), driverFleetId);

    String reporterToken = tokens.issue(reporterUserId, tenantId, "DRIVER", "luis@andes.pe");
    String incidentId = createIncident(reporterToken, tripId);

    mockMvc
        .perform(
            multipart("/api/v1/incidents/{id}/evidences", incidentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Adjunte el comprobante en el campo file."));

    mockMvc
        .perform(
            multipart("/api/v1/incidents/{id}/evidences", incidentId)
                .file(new MockMultipartFile("file", "vacio.jpg", "image/jpeg", new byte[0]))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("El archivo del comprobante está vacío."));

    mockMvc
        .perform(
            multipart("/api/v1/incidents/{id}/evidences", incidentId)
                .file(new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[] {1, 2, 3, 4}))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterToken))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value("Formato no permitido. Adjunte una foto en JPEG, PNG o WEBP, o un PDF."));

    byte[] oversized = new byte[5 * 1024 * 1024 + 1];
    oversized[0] = (byte) 0xFF;
    oversized[1] = (byte) 0xD8;
    oversized[2] = (byte) 0xFF;
    mockMvc
        .perform(
            multipart("/api/v1/incidents/{id}/evidences", incidentId)
                .file(new MockMultipartFile("file", "enorme.jpg", "image/jpeg", oversized))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterToken))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(
            jsonPath("$.detail")
                .value("El comprobante supera el tamaño máximo permitido de 5 MB."));

    mockMvc
        .perform(
            get("/api/v1/incidents/{id}/evidences/{evidenceId}/content", incidentId, UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("La evidencia no existe."));

    String uploadJson =
        mockMvc
            .perform(
                multipart("/api/v1/incidents/{id}/evidences", incidentId)
                    .file(new MockMultipartFile("file", "foto.jpg", "image/jpeg", JPEG_BYTES))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterToken))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String contentUrl =
        objectMapper.readTree(uploadJson).get("evidence").get("contentUrl").asText();

    try (Stream<Path> walk = Files.walk(TestIncidentEvidenceStorage.directory())) {
      Path stored = walk.filter(Files::isRegularFile).findFirst().orElseThrow();
      Files.write(stored, new byte[] {0x00, 0x01});
      mockMvc
          .perform(get(contentUrl).header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterToken))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado."));

      Files.delete(stored);
      mockMvc
          .perform(get(contentUrl).header(HttpHeaders.AUTHORIZATION, "Bearer " + reporterToken))
          .andExpect(status().isNotFound())
          .andExpect(
              jsonPath("$.detail")
                  .value("El archivo del comprobante no está disponible en el almacenamiento."));
    }
  }

  private String createIncident(String driverToken, UUID tripId) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/v1/trips/{tripId}/incidents", tripId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "type", "BREAKDOWN",
                                "description", "Falla en el freno neumático"))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asText();
  }
}
