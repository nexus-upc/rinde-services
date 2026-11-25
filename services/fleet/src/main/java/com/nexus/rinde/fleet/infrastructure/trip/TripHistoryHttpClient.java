package com.nexus.rinde.fleet.infrastructure.trip;

import com.nexus.rinde.fleet.application.internal.outboundservices.acl.ExternalTripSummary;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.RemoteLookupResult;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.TripHistoryClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Consulta los viajes de la empresa en Trip y filtra por vehículo. Si Trip no responde, devuelve
 * unavailable para que el historial responda de forma parcial.
 */
@Component
public class TripHistoryHttpClient implements TripHistoryClient {

  private static final Logger log = LoggerFactory.getLogger(TripHistoryHttpClient.class);
  private static final String TRIPS_PATH = "/api/internal/v1/trips/tenants/{tenantId}";
  private static final ParameterizedTypeReference<List<ExternalTripSummary>> TRIP_LIST =
      new ParameterizedTypeReference<>() {};

  private final RestClient restClient;

  public TripHistoryHttpClient(@Value("${rinde.trip.service-url}") String serviceUrl) {
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofSeconds(2)));
    this.restClient =
        RestClient.builder().baseUrl(serviceUrl).requestFactory(requestFactory).build();
  }

  @Override
  public RemoteLookupResult<List<ExternalTripSummary>> findByTenantAndVehicle(
      UUID tenantId, UUID vehicleId) {
    try {
      List<ExternalTripSummary> trips =
          restClient.get().uri(TRIPS_PATH, tenantId).retrieve().body(TRIP_LIST);
      if (trips == null) {
        return RemoteLookupResult.available(List.of());
      }
      List<ExternalTripSummary> filtered =
          trips.stream().filter(trip -> vehicleId.equals(trip.vehicleId())).toList();
      return RemoteLookupResult.available(filtered);
    } catch (RestClientException ex) {
      log.warn("Trip no respondió el historial de viajes para el vehículo {}.", vehicleId);
      return RemoteLookupResult.unavailable();
    }
  }
}
