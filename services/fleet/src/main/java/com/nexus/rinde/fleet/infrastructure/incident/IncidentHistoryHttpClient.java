package com.nexus.rinde.fleet.infrastructure.incident;

import com.nexus.rinde.fleet.application.internal.outboundservices.acl.ExternalIncidentSummary;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.IncidentHistoryClient;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.RemoteLookupResult;
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
 * Consulta las incidencias de un vehículo en Incident. Si Incident no responde, devuelve
 * unavailable para que el historial responda de forma parcial.
 */
@Component
public class IncidentHistoryHttpClient implements IncidentHistoryClient {

  private static final Logger log = LoggerFactory.getLogger(IncidentHistoryHttpClient.class);
  private static final String INCIDENTS_PATH =
      "/api/internal/v1/incidents/tenants/{tenantId}?vehicleId={vehicleId}";
  private static final ParameterizedTypeReference<List<ExternalIncidentSummary>> INCIDENT_LIST =
      new ParameterizedTypeReference<>() {};

  private final RestClient restClient;

  public IncidentHistoryHttpClient(@Value("${rinde.incident.service-url}") String serviceUrl) {
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofSeconds(2)));
    this.restClient =
        RestClient.builder().baseUrl(serviceUrl).requestFactory(requestFactory).build();
  }

  @Override
  public RemoteLookupResult<List<ExternalIncidentSummary>> findByTenantAndVehicle(
      UUID tenantId, UUID vehicleId) {
    try {
      List<ExternalIncidentSummary> incidents =
          restClient
              .get()
              .uri(INCIDENTS_PATH, tenantId, vehicleId)
              .retrieve()
              .body(INCIDENT_LIST);
      return RemoteLookupResult.available(incidents == null ? List.of() : incidents);
    } catch (RestClientException ex) {
      log.warn("Incident no respondió el historial para el vehículo {}.", vehicleId);
      return RemoteLookupResult.unavailable();
    }
  }
}
