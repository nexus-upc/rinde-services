package com.nexus.rinde.iam.domain.services;

import com.nexus.rinde.iam.domain.model.aggregates.Tenant;
import com.nexus.rinde.iam.domain.model.queries.GetCurrentTenantProfileQuery;
import com.nexus.rinde.iam.domain.model.queries.GetTenantByIdQuery;
import java.util.Optional;

/** Consultas de empresas. */
public interface TenantQueryService {

  Optional<Tenant> handle(GetTenantByIdQuery query);

  Tenant handle(GetCurrentTenantProfileQuery query);
}
