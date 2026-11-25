package com.nexus.rinde.expense.infrastructure.trip;

import com.nexus.rinde.expense.domain.model.valueobjects.TripSnapshot;
import com.nexus.rinde.expense.domain.services.TripStatusService;
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
 * Cliente HTTP que consulta el estado del viaje al microservicio Trip.
 * Reemplaza TripContextFacade del monolito (GET /api/internal/v1/trips/tenants/{tenantId}/{tripId}).
 */
@Service
public class TripStatusHttpClient implements TripStatusService {

  private static final Logger log = LoggerFactory.getLogger(TripStatusHttpClient.class);
  private static final String TRIP_PATH = "/api/internal/v1/trips/tenants/{tenantId}/{tripId}";

  private final RestClient restClient;

  public TripStatusHttpClient(
      @Value("${rinde.trip.service-url:http://localhost:8082}") String serviceUrl) {
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofSeconds(2)));
    this.restClient =
        RestClient.builder().baseUrl(serviceUrl).requestFactory(requestFactory).build();
  }

  @Override
  public Optional<TripSnapshot> findTrip(UUID tenantId, UUID tripId) {
    try {
      TripInternalResponse response =
          restClient
              .get()
              .uri(TRIP_PATH, tenantId, tripId)
              .retrieve()
              .body(TripInternalResponse.class);
      return Optional.ofNullable(response)
          .map(body -> new TripSnapshot(body.tripId(), body.status(), body.driverId()));
    } catch (HttpClientErrorException.NotFound ex) {
      return Optional.empty();
    } catch (RestClientException ex) {
      log.warn("Trip service no respondió a la consulta del viaje: {}", ex.getMessage());
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el estado del viaje en Trip.");
    }
  }
}
