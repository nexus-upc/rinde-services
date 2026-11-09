package com.nexus.rinde.expense.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.valueobjects.EvidenceFile;
import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseCategory;
import com.nexus.rinde.expense.domain.model.valueobjects.Money;
import com.nexus.rinde.expense.domain.services.EvidenceStorage;
import com.nexus.rinde.expense.infrastructure.persistence.jpa.repositories.ExpenseRepository;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import com.nexus.rinde.support.AbstractIntegrationTest;
import com.nexus.rinde.support.EvidenceFiles;
import com.nexus.rinde.support.RelativeDates;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** Subida, lectura y autorización de comprobantes contra la base y el almacenamiento reales. */
class ExpenseEvidenceIntegrationTest extends AbstractIntegrationTest {

  private static final String RECEIPT = "recibo.jpg";

  @Autowired private ExpenseRepository expenseRepository;
  @Autowired private EvidenceStorage evidenceStorage;
  @Autowired private JdbcTemplate jdbcTemplate;

  private UUID tenantId;
  private UUID tripId;
  private UUID driverId;
  private UUID otherDriverId;
  private UUID adminId;
  private UUID otherTenantId;
  private UUID otherTenantDriverId;
  private UUID otherTenantAdminId;
  private String driverToken;
  private String otherDriverToken;
  private String adminToken;
  private String otherTenantDriverToken;
  private String otherTenantAdminToken;
  private Expense expense;

  @BeforeEach
  void setUpExpense() {
    tenantId = UUID.randomUUID();
    tripId = UUID.randomUUID();
    driverId = UUID.randomUUID();
    otherDriverId = UUID.randomUUID();
    adminId = UUID.randomUUID();
    otherTenantId = UUID.randomUUID();
    otherTenantDriverId = UUID.randomUUID();
    otherTenantAdminId = UUID.randomUUID();
    driverToken = tokens.issue(driverId, tenantId, "DRIVER", "luis@andes.pe");
    otherDriverToken = tokens.issue(otherDriverId, tenantId, "DRIVER", "marta@andes.pe");
    adminToken = tokens.issue(adminId, tenantId, "ADMINISTRATOR", "ana@andes.pe");
    otherTenantDriverToken =
        tokens.issue(otherTenantDriverId, otherTenantId, "DRIVER", "marta@beta.pe");
    otherTenantAdminToken =
        tokens.issue(otherTenantAdminId, otherTenantId, "ADMINISTRATOR", "beto@beta.pe");
    expense =
        expenseRepository.saveAndFlush(
            Expense.create(
                tenantId,
                tripId,
                driverId,
                ExpenseCategory.FUEL,
                Money.of(new BigDecimal("180.50"), "PEN"),
                LocalDate.now(),
                "KEY-EVI-01"));
  }

  @Test
  @DisplayName("Subir el comprobante lo guarda con sus metadatos y deja el gasto REGISTERED")
  void shouldStoreEvidenceAndMarkExpenseRegistered() throws Exception {
    byte[] photo = EvidenceFiles.contentOf(RECEIPT);

    ResultActions upload = api.uploadEvidence(driverToken, expenseId(), RECEIPT, photo);

    upload
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("REGISTERED"))
        .andExpect(jsonPath("$.evidence.fileName").value(RECEIPT))
        .andExpect(jsonPath("$.evidence.contentType").value("image/jpeg"))
        .andExpect(jsonPath("$.evidence.fileSizeBytes").value(photo.length))
        .andExpect(jsonPath("$.evidence.sha256").value(EvidenceFile.sha256Hex(photo)))
        .andExpect(
            jsonPath("$.evidence.contentUrl")
                .value(
                    matchesPattern(
                        "/api/v1/expenses/" + expense.getId() + "/evidences/[0-9a-f-]+/content")));
    String storageKey = storageKeyOfExpense();
    assertThat(storageKey).startsWith(tenantId + "/" + expense.getId() + "/");
  }

  @Test
  @DisplayName("El conductor dueño lee su comprobante con el Content-Type guardado")
  void shouldServeStoredFileToOwnerWithItsContentType() throws Exception {
    byte[] photo = EvidenceFiles.contentOf(RECEIPT);
    String contentUrl = uploadAndGetContentUrl(driverToken, RECEIPT, photo);

    api.content(driverToken, contentUrl)
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/jpeg"))
        .andExpect(header().string("Content-Disposition", containsString("inline")))
        .andExpect(content().bytes(photo));
  }

  @Test
  @DisplayName("El administrador y el responsable de operaciones leen cualquier comprobante de la empresa")
  void shouldServeFileToCompanyReaders() throws Exception {
    String contentUrl = uploadAndGetContentUrl(driverToken, RECEIPT, EvidenceFiles.contentOf(RECEIPT));
    String operationsToken =
        tokens.issue(UUID.randomUUID(), tenantId, "OPERATIONS_MANAGER", "ops@andes.pe");

    api.content(adminToken, contentUrl).andExpect(status().isOk());
    api.content(operationsToken, contentUrl).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Otro conductor de la misma empresa no puede leer el comprobante")
  void shouldRejectReadFromOtherDriverOfSameCompany() throws Exception {
    String contentUrl = uploadAndGetContentUrl(driverToken, RECEIPT, EvidenceFiles.contentOf(RECEIPT));

    api.content(otherDriverToken, contentUrl)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.detail").value("No tiene permiso para ver este comprobante."));
  }

  @Test
  @DisplayName("Una empresa ajena recibe 404 al leer el comprobante")
  void shouldAnswerNotFoundToOtherCompanyReader() throws Exception {
    String contentUrl = uploadAndGetContentUrl(driverToken, RECEIPT, EvidenceFiles.contentOf(RECEIPT));

    api.content(otherTenantAdminToken, contentUrl)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("El gasto no existe."));
  }

  @Test
  @DisplayName("Una empresa ajena no puede adjuntar comprobantes a un gasto que no es suyo")
  void shouldAnswerNotFoundToOtherCompanyUploader() throws Exception {
    api.uploadEvidence(otherTenantDriverToken, expenseId(), RECEIPT, EvidenceFiles.contentOf(RECEIPT))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Otro conductor de la empresa no puede adjuntar comprobantes")
  void shouldRejectUploadFromOtherDriver() throws Exception {
    api.uploadEvidence(otherDriverToken, expenseId(), RECEIPT, EvidenceFiles.contentOf(RECEIPT))
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.detail")
                .value("Solo el conductor que registró el gasto puede adjuntar su comprobante."));
  }

  @Test
  @DisplayName("El administrador no adjunta comprobantes: solo el conductor del gasto")
  void shouldRejectUploadFromAdministrator() throws Exception {
    api.uploadEvidence(adminToken, expenseId(), RECEIPT, EvidenceFiles.contentOf(RECEIPT))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Subir un comprobante requiere autenticación")
  void shouldRequireAuthenticationToUpload() throws Exception {
    api.uploadEvidence(null, expenseId(), RECEIPT, EvidenceFiles.contentOf(RECEIPT))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Un formato no permitido responde 400 y el gasto sigue pendiente de sustento")
  void shouldRejectInvalidFormatAndKeepExpensePending() throws Exception {
    api.uploadEvidence(driverToken, expenseId(), "nota.txt", EvidenceFiles.contentOf("nota.txt"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value("Formato no permitido. Adjunte una foto en JPEG, PNG o WEBP, o un PDF."));

    api.detail(driverToken, expenseId())
        .andExpect(jsonPath("$.status").value("PENDING_SUPPORT"))
        .andExpect(jsonPath("$.evidence").value(nullValue()));
  }

  @Test
  @DisplayName("Un archivo que supera el tamaño máximo responde 413")
  void shouldRejectOversizedFile() throws Exception {
    byte[] oversized = EvidenceFiles.sized(RECEIPT, 5 * 1024 * 1024 + 1);

    api.uploadEvidence(driverToken, expenseId(), RECEIPT, oversized)
        .andExpect(status().isPayloadTooLarge())
        .andExpect(
            jsonPath("$.detail")
                .value("El comprobante supera el tamaño máximo permitido de 5 MB."));
  }

  @Test
  @DisplayName("Subir un comprobante nuevo reemplaza al anterior y borra su archivo")
  void shouldReplaceEvidenceAndDeletePreviousFile() throws Exception {
    uploadAndGetContentUrl(driverToken, "borroso.jpg", EvidenceFiles.contentOf("borroso.jpg"));
    String previousKey = storageKeyOfExpense();

    String contentUrl = uploadAndGetContentUrl(driverToken, "legible.png", EvidenceFiles.contentOf("legible.png"));

    assertThat(countEvidences()).isEqualTo(1);
    assertThatThrownBy(() -> evidenceStorage.load(previousKey))
        .isInstanceOf(ResourceNotFoundException.class);
    api.content(driverToken, contentUrl)
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"));
  }

  @Test
  @DisplayName("Un comprobante anterior sin archivo responde 404 y su contentUrl es nulo")
  void shouldAnswerNotFoundForLegacyEvidenceWithoutFile() throws Exception {
    UUID evidenceId = UUID.randomUUID();
    jdbcTemplate.update(
        "insert into expense.evidences (id, tenant_id, expense_id, image_url, file_size_bytes,"
            + " uploaded_at) values (?, ?, ?, ?, ?, ?)",
        evidenceId,
        tenantId,
        expense.getId(),
        "https://storage.rinde.pe/evidences/anterior.jpg",
        2048L,
        Timestamp.from(Instant.now()));
    String contentUrl = "/api/v1/expenses/%s/evidences/%s/content".formatted(expense.getId(), evidenceId);

    api.detail(driverToken, expenseId())
        .andExpect(jsonPath("$.evidence.contentUrl").value(nullValue()));
    api.content(driverToken, contentUrl)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("El comprobante no tiene archivo almacenado."));
  }

  @Test
  @DisplayName("Sincronizar un gasto con el campo evidence heredado lo guarda sin comprobante")
  void shouldSyncExpenseWithoutEvidenceWhenLegacyFieldIsSent() throws Exception {
    tripStub.setTripStatus(tenantId, tripId, "IN_ROUTE");
    Map<String, Object> evidence =
        Map.of("imageUrl", "https://storage.rinde.pe/evidences/heredada.jpg", "fileSizeBytes", 2048L);
    Map<String, Object> item =
        Map.of(
            "tripId", tripId.toString(),
            "category", "FOOD",
            "amount", new BigDecimal("25.00"),
            "currency", "PEN",
            "expenseDate", RelativeDates.resolve("HOY+5"),
            "idempotencyKey", "KEY-EVI-SYNC-LEGACY",
            "evidence", evidence);

    api.sync(driverToken, List.of(item))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.synchronizedCount").value(1))
        .andExpect(jsonPath("$.items[0].status").value("PENDING_SUPPORT"))
        .andExpect(jsonPath("$.items[0].evidence").value(nullValue()));
  }

  private String expenseId() {
    return expense.getId().toString();
  }

  private String uploadAndGetContentUrl(String token, String fileName, byte[] content)
      throws Exception {
    MvcResult result = api.uploadEvidence(token, expenseId(), fileName, content).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    return body.get("evidence").get("contentUrl").asText();
  }

  private String storageKeyOfExpense() {
    return jdbcTemplate.queryForObject(
        "select storage_key from expense.evidences where expense_id = ?",
        String.class,
        expense.getId());
  }

  private int countEvidences() {
    Integer count =
        jdbcTemplate.queryForObject("select count(*) from expense.evidences", Integer.class);
    return count == null ? 0 : count;
  }
}
