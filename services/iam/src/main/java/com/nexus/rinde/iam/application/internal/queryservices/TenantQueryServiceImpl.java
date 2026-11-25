package com.nexus.rinde.iam.application.internal.queryservices;

import com.nexus.rinde.iam.domain.model.aggregates.Tenant;
import com.nexus.rinde.iam.domain.model.queries.GetCurrentTenantProfileQuery;
import com.nexus.rinde.iam.domain.model.queries.GetTenantByIdQuery;
import com.nexus.rinde.iam.domain.services.TenantQueryService;
import com.nexus.rinde.iam.infrastructure.persistence.jpa.repositories.TenantRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Consulta empresas por id. */
@Service
public class TenantQueryServiceImpl implements TenantQueryService {

  private final TenantRepository tenantRepository;

  public TenantQueryServiceImpl(TenantRepository tenantRepository) {
    this.tenantRepository = tenantRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Tenant> handle(GetTenantByIdQuery query) {
    return tenantRepository.findById(query.tenantId().value());
  }

  @Override
  public Tenant handle(GetCurrentTenantProfileQuery query) {
    throw new ResponseStatusException(
        HttpStatus.NOT_IMPLEMENTED, "Funcionalidad pendiente de implementar en S4-28.");
  }
}
