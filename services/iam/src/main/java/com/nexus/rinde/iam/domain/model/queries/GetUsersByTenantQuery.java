package com.nexus.rinde.iam.domain.model.queries;

import com.nexus.rinde.iam.domain.model.valueobjects.Role;
import com.nexus.rinde.iam.domain.model.valueobjects.TenantId;

/** Lista los usuarios de una empresa; si se indica un rol, solo los de ese rol. */
public record GetUsersByTenantQuery(TenantId tenantId, Role role) {

  /** Lista todos los usuarios de la empresa. */
  public GetUsersByTenantQuery(TenantId tenantId) {
    this(tenantId, null);
  }
}
