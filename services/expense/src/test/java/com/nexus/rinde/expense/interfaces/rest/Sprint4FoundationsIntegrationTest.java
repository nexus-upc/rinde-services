package com.nexus.rinde.expense.interfaces.rest;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Sprint4FoundationsIntegrationTest extends AbstractIntegrationTest {

  @Test
  void expensesTrayReturns501ForManagersAnd403ForDriversAndIsInOpenApi() throws Exception {
    UUID tenantId = UUID.randomUUID();
    String managerToken = tokens.issue(UUID.randomUUID(), tenantId, "ADMINISTRATOR", "admin@rinde.pe");
    String driverToken = tokens.issue(UUID.randomUUID(), tenantId, "DRIVER", "driver@rinde.pe");

    mockMvc
        .perform(get("/api/v1/expenses").header("Authorization", "Bearer " + managerToken))
        .andExpect(status().isNotImplemented())
        .andExpect(jsonPath("$.detail").value("Funcionalidad pendiente de implementar en S4-12."));

    mockMvc
        .perform(get("/api/v1/expenses").header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/expenses")));
  }
}
