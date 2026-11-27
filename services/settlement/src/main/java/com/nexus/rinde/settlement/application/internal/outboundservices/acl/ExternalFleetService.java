package com.nexus.rinde.settlement.application.internal.outboundservices.acl;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Adaptador ACL que consulta por HTTP el conductor registrado a un usuario en el microservicio
 * Fleet. Si Fleet no responde, la operación informa indisponibilidad.
 */
@Service
public class ExternalFleetService {

  private static final Logger log = LoggerFactory.getLogger(ExternalFleetService.class);
  private static final String DRIVER_PATH =
      "/api/internal/v1/fleet/tenants/{tenantId}/users/{userId}/driver";
  private static final String UNAVAILABLE = "No se pudo consultar el conductor en Fleet.";

  private final RestClient restClient;

  public ExternalFleetService(
      @Value("${rinde.fleet.base-url:http://localhost:8083}") String serviceUrl) {
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofSeconds(2)));
    this.restClient =
        RestClient.builder().baseUrl(serviceUrl).requestFactory(requestFactory).build();
  }

  /** Id del conductor ligado al usuario, o vacío si no tiene conductor registrado. */
  public Optional<UUID> findDriverIdByUserId(UUID tenantId, UUID userId) {
    if (userId == null) {
      return Optional.empty();
    }
    try {
      DriverIdResponse response =
          restClient
              .get()
              .uri(DRIVER_PATH, tenantId, userId)
              .retrieve()
              .body(DriverIdResponse.class);
      return Optional.ofNullable(response).map(DriverIdResponse::driverId);
    } catch (HttpClientErrorException.NotFound ex) {
      return Optional.empty();
    } catch (RestClientException ex) {
      log.warn("Fleet no respondió el conductor del usuario: {}", ex.getMessage());
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE);
    }
  }

  public record DriverIdResponse(UUID driverId) {}
}
