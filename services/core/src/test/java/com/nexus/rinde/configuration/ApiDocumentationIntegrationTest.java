package com.nexus.rinde.configuration;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class ApiDocumentationIntegrationTest extends AbstractIntegrationTest {

  @Test
  void healthCheckIsPublic() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void openApiDocumentListsOnlyTheContextsServedByCore() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/trips/{tripId}/advances")))
        .andExpect(jsonPath("$.paths", not(hasKey("/api/v1/settlements/advances"))))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/settlements/{id}/close")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/trips/{tripId}/settlement")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/dashboard/fleet-status")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/subscriptions/current")))
        .andExpect(jsonPath("$.paths", hasKey("/api/v1/platform/plans")))
        .andExpect(jsonPath("$.paths", not(hasKey("/api/v1/tenants"))))
        .andExpect(jsonPath("$.paths", not(hasKey("/api/v1/auth/sign-in"))))
        .andExpect(jsonPath("$.paths", not(hasKey("/api/v1/users/{id}"))))
        .andExpect(jsonPath("$.paths", not(hasKey("/api/v1/trips"))))
        .andExpect(jsonPath("$.paths", not(hasKey("/api/v1/trips/{id}/assignment"))))
        .andExpect(jsonPath("$.paths", not(hasKey("/api/v1/vehicles/{id}"))))
        .andExpect(jsonPath("$.paths", not(hasKey("/api/v1/drivers/{id}"))))
        .andExpect(jsonPath("$.paths", not(hasKey("/api/v1/maintenances/alerts"))))
        .andExpect(jsonPath("$.paths", not(hasKey("/api/v1/trips/{tripId}/expenses"))))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
  }

  @Test
  void swaggerUiIsReachableWithoutToken() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }
}
