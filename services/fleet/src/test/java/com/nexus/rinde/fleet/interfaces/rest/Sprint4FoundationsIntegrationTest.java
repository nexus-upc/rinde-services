package com.nexus.rinde.fleet.interfaces.rest;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Sprint4FoundationsIntegrationTest extends AbstractIntegrationTest {

  @Test
  void vehicleHistoryReturns404ForMissingVehicleAnd403ForDriversAndIsInOpenApi() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID vehicleId = UUID.randomUUID();
    String managerToken =
        tokens.issue(UUID.randomUUID(), tenantId, "OPERATIONS_MANAGER", "ops@rinde.pe");
    String driverToken = tokens.issue(UUID.randomUUID(), tenantId, "DRIVER", "driver@rinde.pe");

    mockMvc
        .perform(
            get("/api/v1/vehicles/{id}/history", vehicleId)
                .header("Authorization", "Bearer " + managerToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("El vehículo no existe."));

    mockMvc
        .perform(
            get("/api/v1/vehicles/{id}/history", vehicleId)
                .header("Authorization", "Bearer " + driverToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/vehicles/{id}/history")));
  }
}
