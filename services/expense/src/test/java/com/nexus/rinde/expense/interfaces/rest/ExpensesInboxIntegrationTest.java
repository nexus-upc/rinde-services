package com.nexus.rinde.expense.interfaces.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexus.rinde.support.AbstractIntegrationTest;
import com.nexus.rinde.support.EvidenceFiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pruebas de integración del contrato C2 (GET /api/v1/expenses) contra PostgreSQL. */
class ExpensesInboxIntegrationTest extends AbstractIntegrationTest {

  @Test
  void ordersByExpenseDateDescThenCreatedAtDescAndIsolatesTenants() throws Exception {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    UUID driverUserA = UUID.randomUUID();
    UUID driverFleetA = UUID.randomUUID();
    UUID driverUserB = UUID.randomUUID();
    UUID driverFleetB = UUID.randomUUID();
    UUID tripA = UUID.randomUUID();
    UUID tripB = UUID.randomUUID();

    fleetStub.setDriverForUser(tenantA, driverUserA, driverFleetA);
    fleetStub.setDriverForUser(tenantB, driverUserB, driverFleetB);
    tripStub.setTrip(tenantA, tripA, "IN_ROUTE", driverFleetA);
    tripStub.setTrip(tenantB, tripB, "IN_ROUTE", driverFleetB);

    String driverTokenA = tokens.issue(driverUserA, tenantA, "DRIVER", "luis@andes.pe");
    String driverTokenB = tokens.issue(driverUserB, tenantB, "DRIVER", "otro@otra.pe");
    String adminTokenA = tokens.issue(UUID.randomUUID(), tenantA, "ADMINISTRATOR", "ana@andes.pe");

    String firstSameDayId =
        registerExpense(driverTokenA, tripA, "FUEL", "100.00", LocalDate.of(2026, 11, 20), "K-1");
    Thread.sleep(15);
    String secondSameDayId =
        registerExpense(driverTokenA, tripA, "TOLL", "25.00", LocalDate.of(2026, 11, 20), "K-2");
    String newerDayId =
        registerExpense(driverTokenA, tripA, "FOOD", "40.00", LocalDate.of(2026, 11, 22), "K-3");

    api.uploadEvidence(
            driverTokenA, secondSameDayId, "recibo.jpg", EvidenceFiles.contentOf("recibo.jpg"))
        .andExpect(status().isCreated());

    registerExpense(driverTokenB, tripB, "FUEL", "300.00", LocalDate.of(2026, 11, 25), "K-OTRA");

    api.listInbox(adminTokenA, Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalItems").value(3))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.items", hasSize(3)))
        .andExpect(jsonPath("$.items[0].id").value(newerDayId))
        .andExpect(jsonPath("$.items[1].id").value(secondSameDayId))
        .andExpect(jsonPath("$.items[1].status").value("REGISTERED"))
        .andExpect(jsonPath("$.items[1].evidence.fileName").value("recibo.jpg"))
        .andExpect(jsonPath("$.items[1].evidence.contentUrl").isNotEmpty())
        .andExpect(jsonPath("$.items[2].id").value(firstSameDayId))
        .andExpect(jsonPath("$.items[2].status").value("PENDING_SUPPORT"));
  }

  @Test
  void requiresAuthenticationAndReturnsProblemDetailOnInvalidQueryParameters() throws Exception {
    mockMvc.perform(get("/api/v1/expenses")).andExpect(status().isUnauthorized());

    String opsToken =
        tokens.issue(UUID.randomUUID(), UUID.randomUUID(), "OPERATIONS_MANAGER", "ops@andes.pe");

    mockMvc
        .perform(
            get("/api/v1/expenses")
                .param("status", "APPROVED,UNKNOWN")
                .header("Authorization", "Bearer " + opsToken))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value("El parámetro status contiene un estado de gasto inválido."))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  private String registerExpense(
      String token,
      UUID tripId,
      String category,
      String amount,
      LocalDate date,
      String idempotencyKey)
      throws Exception {
    String json =
        api.register(
                token, tripId.toString(), category, new BigDecimal(amount), "PEN", date, idempotencyKey)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode node = objectMapper.readTree(json);
    return node.get("id").asText();
  }
}
