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
 * Adaptador ACL que consulta por HTTP el estado de un viaje al microservicio Trip. Si Trip no
 * responde, la operación informa indisponibilidad.
 */
@Service
public class ExternalTripService {

  private static final Logger log = LoggerFactory.getLogger(ExternalTripService.class);
  private static final String STATUS_PATH =
      "/api/internal/v1/trips/tenants/{tenantId}/{tripId}/status";
  private static final String UNAVAILABLE = "No se pudo consultar el viaje en Trip.";

  private final RestClient restClient;

  public ExternalTripService(
      @Value("${rinde.trip.base-url:http://localhost:8082}") String serviceUrl) {
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofSeconds(2)));
    this.restClient =
        RestClient.builder().baseUrl(serviceUrl).requestFactory(requestFactory).build();
  }

  /** Estado del viaje, o vacío si no existe para la empresa. */
  public Optional<String> findStatusByTripId(UUID tenantId, UUID tripId) {
    try {
      TripStatusResponse response =
          restClient
              .get()
              .uri(STATUS_PATH, tenantId, tripId)
              .retrieve()
              .body(TripStatusResponse.class);
      return Optional.ofNullable(response).map(TripStatusResponse::status);
    } catch (HttpClientErrorException.NotFound ex) {
      return Optional.empty();
    } catch (RestClientException ex) {
      log.warn("Trip no respondió el estado del viaje: {}", ex.getMessage());
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE);
    }
  }

  public record TripStatusResponse(UUID tripId, String status) {}
}
