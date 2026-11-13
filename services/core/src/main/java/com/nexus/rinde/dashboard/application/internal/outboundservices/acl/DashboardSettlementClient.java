package com.nexus.rinde.dashboard.application.internal.outboundservices.acl;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Obtiene datos de liquidación del servicio Settlement por HTTP. Reemplaza SettlementContextFacade en Dashboard. */
@Service
public class DashboardSettlementClient {

  private final RestClient client;

  public DashboardSettlementClient(@Qualifier("settlementRestClient") RestClient client) {
    this.client = client;
  }

  public Optional<SettlementInfo> findByTripIdAndTenantId(UUID tripId, UUID tenantId) {
    try {
      SettlementResponse body =
          client
              .get()
              .uri(
                  "/api/internal/v1/settlements/tenants/{tenantId}/trips/{tripId}",
                  tenantId,
                  tripId)
              .retrieve()
              .body(SettlementResponse.class);
      return Optional.ofNullable(body).map(r -> new SettlementInfo(r.status(), r.advanceAmount(), r.advanceBalance()));
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 404) return Optional.empty();
      throw new DashboardSettlementUnavailableException(ex);
    } catch (RestClientException ex) {
      throw new DashboardSettlementUnavailableException(ex);
    }
  }

  public long countPendingByTenantId(UUID tenantId) {
    try {
      PendingCountResponse body =
          client
              .get()
              .uri("/api/internal/v1/settlements/tenants/{tenantId}/pending", tenantId)
              .retrieve()
              .body(PendingCountResponse.class);
      return body != null ? body.count() : 0L;
    } catch (RestClientException ex) {
      throw new DashboardSettlementUnavailableException(ex);
    }
  }

  public record SettlementInfo(String status, BigDecimal advanceAmount, BigDecimal advanceBalance) {}

  private record SettlementResponse(String status, BigDecimal advanceAmount, BigDecimal advanceBalance) {}

  private record PendingCountResponse(long count) {}
}
