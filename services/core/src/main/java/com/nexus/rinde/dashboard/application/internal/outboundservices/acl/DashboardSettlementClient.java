package com.nexus.rinde.dashboard.application.internal.outboundservices.acl;

import com.nexus.rinde.configuration.remote.RemoteServiceUnavailableException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente HTTP que consulta al microservicio Settlement para el tablero operativo. Reemplaza la
 * fachada SettlementContextFacade del monolito ahora que las liquidaciones viven en su propio
 * servicio.
 */
@Component
public class DashboardSettlementClient {

  private static final Logger log = LoggerFactory.getLogger(DashboardSettlementClient.class);
  private static final String TRIP_PATH =
      "/api/internal/v1/settlements/tenants/{tenantId}/trips/{tripId}";
  private static final String OPEN_COUNT_PATH =
      "/api/internal/v1/settlements/tenants/{tenantId}/open/count";

  private final RestClient restClient;

  public DashboardSettlementClient(
      @Value("${rinde.settlement.base-url:http://localhost:8086}") String serviceUrl) {
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofSeconds(2)));
    this.restClient =
        RestClient.builder().baseUrl(serviceUrl).requestFactory(requestFactory).build();
  }

  public Optional<SettlementSummary> findByTripIdAndTenantId(UUID tripId, UUID tenantId) {
    try {
      return Optional.ofNullable(
          restClient
              .get()
              .uri(TRIP_PATH, tenantId, tripId)
              .retrieve()
              .body(SettlementSummary.class));
    } catch (HttpClientErrorException.NotFound ex) {
      return Optional.empty();
    } catch (RestClientException ex) {
      log.warn("Settlement no respondió la liquidación del viaje: {}", ex.getMessage());
      throw new RemoteServiceUnavailableException("liquidaciones", ex);
    }
  }

  public long countOpenByTenantId(UUID tenantId) {
    try {
      OpenSettlementCount response =
          restClient.get().uri(OPEN_COUNT_PATH, tenantId).retrieve().body(OpenSettlementCount.class);
      return response == null ? 0L : response.count();
    } catch (RestClientException ex) {
      log.warn("Settlement no respondió la cantidad de liquidaciones abiertas: {}", ex.getMessage());
      throw new RemoteServiceUnavailableException("liquidaciones", ex);
    }
  }

  /** Vista mínima de la liquidación consumida por el tablero operativo. */
  public record SettlementSummary(String status, BigDecimal advanceAmount, BigDecimal advanceBalance) {}

  public record OpenSettlementCount(long count) {}
}
