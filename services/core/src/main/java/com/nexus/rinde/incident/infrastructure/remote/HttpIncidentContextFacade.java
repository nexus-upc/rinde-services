package com.nexus.rinde.incident.infrastructure.remote;

import com.nexus.rinde.configuration.remote.RemoteServiceUnavailableException;
import com.nexus.rinde.incident.interfaces.acl.IncidentContextFacade;
import com.nexus.rinde.incident.interfaces.acl.OpenIncidentSummary;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Consulta las incidencias abiertas en el servicio Incident separado (puerto 8085). */
@Service
@Primary
@ConditionalOnProperty(name = "rinde.incident.remote-enabled", havingValue = "true")
public class HttpIncidentContextFacade implements IncidentContextFacade {

  private static final String SERVICE = "incidencias";
  private static final ParameterizedTypeReference<List<OpenIncidentSummary>> OPEN_LIST =
      new ParameterizedTypeReference<>() {};

  private final RestClient client;

  public HttpIncidentContextFacade(@Qualifier("incidentRestClient") RestClient client) {
    this.client = client;
  }

  @Override
  public long countOpenByTenantId(UUID tenantId) {
    try {
      OpenCountResponse body =
          client
              .get()
              .uri("/api/internal/v1/incidents/tenants/{tenant}/open-count", tenantId)
              .retrieve()
              .body(OpenCountResponse.class);
      if (body == null || body.count() == null) {
        throw new RemoteServiceUnavailableException(SERVICE, null);
      }
      return body.count();
    } catch (RestClientException ex) {
      throw new RemoteServiceUnavailableException(SERVICE, ex);
    }
  }

  @Override
  public List<OpenIncidentSummary> findOpenByTenantId(UUID tenantId) {
    try {
      List<OpenIncidentSummary> body =
          client
              .get()
              .uri("/api/internal/v1/incidents/tenants/{tenant}/open", tenantId)
              .retrieve()
              .body(OPEN_LIST);
      return body == null ? List.of() : body;
    } catch (RestClientException ex) {
      throw new RemoteServiceUnavailableException(SERVICE, ex);
    }
  }

  /** Cuerpo de GET /api/internal/v1/incidents/tenants/{tenantId}/open-count: {"count": N}. */
  record OpenCountResponse(Long count) {}
}
