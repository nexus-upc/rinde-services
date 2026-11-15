package com.nexus.rinde.subscription.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.nexus.rinde.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

/** El endpoint de simulación no existe si no se habilitó expresamente. */
@SpringBootTest(properties = "rinde.billing.simulation.enabled=false")
class DisabledSimulationIntegrationTest extends AbstractIntegrationTest {
  @Test
  void doesNotExposeSimulationInNormalMode() throws Exception {
    api.registerAndVerifyTenant("ana@andes.pe");
    String token = api.accessTokenFor("ana@andes.pe");
    mockMvc.perform(post("/api/v1/subscriptions/{id}/payments/{payment}/simulation",
        UUID.randomUUID(), UUID.randomUUID()).header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
        .andExpect(status().isNotFound());
  }
}
