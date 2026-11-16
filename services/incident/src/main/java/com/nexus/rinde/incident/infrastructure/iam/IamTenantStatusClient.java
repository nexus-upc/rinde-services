package com.nexus.rinde.incident.infrastructure.iam;

import com.nexus.rinde.shared.infrastructure.security.TenantStatusProvider;
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Consulta a IAM el estado actual de la empresa, para que una suscripción vencida se aplique sin
 * esperar a que expire el token. Si IAM no responde, se informa indisponibilidad.
 */
@Component
public class IamTenantStatusClient implements TenantStatusProvider {

  private static final Logger log = LoggerFactory.getLogger(IamTenantStatusClient.class);
  private static final String STATUS_PATH = "/api/internal/v1/tenants/{tenantId}/status";

  private final RestClient restClient;

  public IamTenantStatusClient(@Value("${rinde.iam.service-url}") String serviceUrl) {
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofSeconds(2)));
    this.restClient =
        RestClient.builder().baseUrl(serviceUrl).requestFactory(requestFactory).build();
  }

  @Override
  public Optional<String> findTenantStatus(UUID tenantId) {
    try {
      TenantStatusResponse response =
          restClient.get().uri(STATUS_PATH, tenantId).retrieve().body(TenantStatusResponse.class);
      return Optional.ofNullable(response).map(TenantStatusResponse::status);
    } catch (HttpClientErrorException.NotFound ex) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudo comprobar el estado actual de la empresa.");
    } catch (RestClientException ex) {
      log.warn("IAM no respondió el estado actual de la empresa: {}", ex.getMessage());
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudo comprobar el estado actual de la empresa.");
    }
  }

  /** Cuerpo de GET /api/internal/v1/tenants/{tenantId}/status en IAM. */
  public record TenantStatusResponse(UUID tenantId, String status) {}
}
