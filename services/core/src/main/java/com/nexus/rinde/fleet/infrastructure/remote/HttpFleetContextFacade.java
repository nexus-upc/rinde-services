package com.nexus.rinde.fleet.infrastructure.remote;

import com.nexus.rinde.configuration.remote.RemoteServiceUnavailableException;
import com.nexus.rinde.fleet.interfaces.acl.FleetContextFacade;
import com.nexus.rinde.fleet.interfaces.acl.FleetDriverSummary;
import com.nexus.rinde.fleet.interfaces.acl.FleetVehicleSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Consulta unidades y conductores en el servicio Fleet separado; reemplaza la copia de core. */
@Service
@Primary
@ConditionalOnProperty(name = "rinde.fleet.remote-enabled", havingValue = "true")
public class HttpFleetContextFacade implements FleetContextFacade {

  private static final String BASE = "/api/internal/v1/fleet/tenants/{tenant}";

  private final RestClient client;

  public HttpFleetContextFacade(@Qualifier("fleetRestClient") RestClient client) {
    this.client = client;
  }

  @Override
  public long countVehiclesByTenantId(UUID tenantId) {
    return get(BASE + "/vehicles/count", VehicleCount.class, tenantId)
        .map(VehicleCount::count)
        .orElse(0L);
  }

  @Override
  public List<FleetVehicleSummary> findVehiclesByTenantId(UUID tenantId) {
    return list(BASE + "/vehicles", new ParameterizedTypeReference<>() {}, tenantId);
  }

  @Override
  public List<FleetDriverSummary> findDriversByTenantId(UUID tenantId) {
    return list(BASE + "/drivers", new ParameterizedTypeReference<>() {}, tenantId);
  }

  private <T> Optional<T> get(String path, Class<T> type, Object... variables) {
    try {
      return Optional.ofNullable(client.get().uri(path, variables).retrieve().body(type));
    } catch (HttpClientErrorException.NotFound ex) {
      return Optional.empty();
    } catch (RestClientException ex) {
      throw new RemoteServiceUnavailableException("flota", ex);
    }
  }

  private <T> List<T> list(
      String path, ParameterizedTypeReference<List<T>> type, Object... variables) {
    try {
      List<T> body = client.get().uri(path, variables).retrieve().body(type);
      return body == null ? List.of() : body;
    } catch (RestClientException ex) {
      throw new RemoteServiceUnavailableException("flota", ex);
    }
  }

  record VehicleCount(long count) {}
}
