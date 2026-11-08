package com.nexus.rinde.iam.infrastructure.remote;

import com.nexus.rinde.iam.interfaces.acl.IamContextFacade;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Consulta IAM separado sin copiar empresas ni usuarios a la base del monolito. */
@Service
@Primary
@ConditionalOnProperty(name = "rinde.iam.remote-enabled", havingValue = "true")
public class HttpIamContextFacade implements IamContextFacade {
  private final RestClient client;

  public HttpIamContextFacade(@Qualifier("iamRestClient") RestClient client) {
    this.client = client;
  }

  @Override
  public Optional<String> findTenantStatus(UUID tenantId) {
    return get("/api/internal/v1/tenants/{tenant}/status", TenantStatusResponse.class, tenantId)
        .map(response -> {
          if (!tenantId.equals(response.tenantId()) || response.status() == null
              || !Set.of("ACTIVE", "RESTRICTED", "PENDING_VERIFICATION").contains(response.status())) {
            throw new IamUnavailableException();
          }
          return response.status();
        });
  }

  @Override
  public Optional<String> findAdministratorEmail(UUID tenantId) {
    return get("/api/internal/v1/tenants/{tenant}/administrator-email", AdministratorEmailResponse.class,
        tenantId).map(AdministratorEmailResponse::email);
  }

  private <T> Optional<T> get(String path, Class<T> type, Object... variables) {
    try {
      return Optional.ofNullable(client.get().uri(path, variables).retrieve().body(type));
    } catch (HttpClientErrorException.NotFound ex) {
      return Optional.empty();
    } catch (RestClientException ex) {
      throw new IamUnavailableException();
    }
  }

  record TenantStatusResponse(UUID tenantId, String status) {}
  record AdministratorEmailResponse(String email) {}
}
