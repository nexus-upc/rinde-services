package com.nexus.rinde.expense.infrastructure.fleet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.rinde.support.AbstractIntegrationTest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class FleetDriverHttpClientTest extends AbstractIntegrationTest {

  @Autowired private FleetDriverHttpClient fleetClient;

  @Test
  @DisplayName("Consulta HTTP a Fleet retorna driverId cuando el usuario tiene ficha")
  void shouldReturnDriverIdWhenLinked() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();

    fleetStub.setDriverForUser(tenantId, userId, driverId);

    Optional<UUID> result = fleetClient.findDriverIdByUserId(tenantId, userId);
    assertThat(result).contains(driverId);
  }

  @Test
  @DisplayName("Consulta HTTP a Fleet retorna vacío cuando el usuario no tiene conductor (404)")
  void shouldReturnEmptyWhenDriverNotFound() {
    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    fleetStub.setDriverNotFound(tenantId, userId);

    Optional<UUID> result = fleetClient.findDriverIdByUserId(tenantId, userId);
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Consulta HTTP a Fleet informa 503 cuando el servicio no responde")
  void shouldThrowServiceUnavailableWhenFleetFails() {
    fleetStub.setUnavailable();

    assertThatThrownBy(() -> fleetClient.findDriverIdByUserId(UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }
}
