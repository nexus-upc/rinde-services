package com.nexus.rinde.incident.infrastructure.fleet;

import com.nexus.rinde.incident.domain.services.DriverLookupService;
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
 * Cliente HTTP que consulta a Fleet el conductor vinculado a un usuario. Un 404 significa que el
 * usuario no es conductor; cualquier otra falla se informa como 503.
 */
@Service
public class FleetDriverHttpClient implements DriverLookupService {

  private static final Logger log = LoggerFactory.getLogger(FleetDriverHttpClient.class);
  private static final String DRIVER_PATH =
      "/api/internal/v1/fleet/tenants/{tenantId}/users/{userId}/driver";

  private final RestClient restClient;

  public FleetDriverHttpClient(
      @Value("${rinde.fleet.service-url:http://localhost:8083}") String serviceUrl) {
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofSeconds(2)));
    this.restClient =
        RestClient.builder().baseUrl(serviceUrl).requestFactory(requestFactory).build();
  }

  @Override
  public Optional<UUID> findDriverIdByUserId(UUID tenantId, UUID userId) {
    try {
      DriverResponse response =
          restClient
              .get()
              .uri(DRIVER_PATH, tenantId, userId)
              .retrieve()
              .body(DriverResponse.class);
      return Optional.ofNullable(response).map(DriverResponse::driverId);
    } catch (HttpClientErrorException.NotFound ex) {
      return Optional.empty();
    } catch (RestClientException ex) {
      log.warn("Fleet no respondió la consulta del conductor: {}", ex.getMessage());
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el conductor en Fleet.");
    }
  }

  /** Cuerpo de GET .../users/{userId}/driver en Fleet. */
  public record DriverResponse(UUID driverId) {}
}
