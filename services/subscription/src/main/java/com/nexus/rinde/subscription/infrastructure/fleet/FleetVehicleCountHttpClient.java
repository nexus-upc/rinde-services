package com.nexus.rinde.subscription.infrastructure.fleet;

import com.nexus.rinde.subscription.domain.services.FleetUnitCounter;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cliente HTTP que consulta la cantidad de unidades de la empresa al microservicio Fleet.
 * Reemplaza FleetContextFacade del monolito (GET /api/internal/v1/fleet/tenants/{tenantId}/vehicles/count).
 * Un 404 o una respuesta sin cuerpo se tratan como indisponibilidad: nunca se asume cero unidades.
 */
@Service
public class FleetVehicleCountHttpClient implements FleetUnitCounter {

  private static final Logger log = LoggerFactory.getLogger(FleetVehicleCountHttpClient.class);
  private static final String COUNT_PATH =
      "/api/internal/v1/fleet/tenants/{tenantId}/vehicles/count";
  private static final String UNAVAILABLE =
      "No se pudo consultar la cantidad de unidades en Fleet.";

  private final RestClient restClient;

  public FleetVehicleCountHttpClient(
      @Value("${rinde.fleet.base-url:http://localhost:8083}") String serviceUrl) {
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofSeconds(2)));
    this.restClient =
        RestClient.builder().baseUrl(serviceUrl).requestFactory(requestFactory).build();
  }

  @Override
  public long countVehicles(UUID tenantId) {
    VehicleCountResponse response;
    try {
      response =
          restClient.get().uri(COUNT_PATH, tenantId).retrieve().body(VehicleCountResponse.class);
    } catch (RestClientException ex) {
      log.warn("Fleet no respondió la cantidad de unidades de la empresa: {}", ex.getMessage());
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE);
    }
    if (response == null) {
      log.warn("Fleet respondió sin cuerpo a la consulta de unidades de la empresa.");
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE);
    }
    return response.count();
  }
}
