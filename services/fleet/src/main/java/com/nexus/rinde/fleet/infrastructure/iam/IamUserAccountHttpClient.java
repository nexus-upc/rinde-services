package com.nexus.rinde.fleet.infrastructure.iam;

import com.nexus.rinde.fleet.application.internal.outboundservices.acl.IamAccountClient;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.IamUserAccount;
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
 * Consulta a IAM la cuenta del usuario en una empresa. Un 404 significa que la cuenta no está en
 * esa empresa; cualquier otra falla se informa como 503.
 */
@Component
public class IamUserAccountHttpClient implements IamAccountClient {

  private static final Logger log = LoggerFactory.getLogger(IamUserAccountHttpClient.class);
  private static final String USER_PATH = "/api/internal/v1/tenants/{tenantId}/users/{userId}";

  private final RestClient restClient;

  public IamUserAccountHttpClient(@Value("${rinde.iam.service-url}") String serviceUrl) {
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofSeconds(2)));
    this.restClient =
        RestClient.builder().baseUrl(serviceUrl).requestFactory(requestFactory).build();
  }

  @Override
  public Optional<IamUserAccount> findUserAccount(UUID tenantId, UUID userId) {
    try {
      UserAccountResponse response =
          restClient.get().uri(USER_PATH, tenantId, userId).retrieve().body(UserAccountResponse.class);
      return Optional.ofNullable(response)
          .map(body -> new IamUserAccount(body.userId(), body.role()));
    } catch (HttpClientErrorException.NotFound ex) {
      return Optional.empty();
    } catch (RestClientException ex) {
      log.warn("IAM no respondió la cuenta del usuario indicado.");
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "No se pudo comprobar la cuenta en IAM. Intente nuevamente.");
    }
  }

  /** Campos de GET /api/internal/v1/tenants/{tenantId}/users/{userId} que Fleet usa. */
  public record UserAccountResponse(UUID userId, String role) {}
}
