package com.nexus.rinde.iam.interfaces.acl;

import com.nexus.rinde.shared.infrastructure.security.TenantStatusProvider;
import java.util.Optional;
import java.util.UUID;

/**
 * Consultas que core necesita de IAM & Tenancy: estado de la empresa para la seguridad y correo del
 * administrador para los avisos. Ambas se limitan a la empresa indicada.
 */
public interface IamContextFacade extends TenantStatusProvider {

  /** Estado de la empresa (PENDING_VERIFICATION, ACTIVE o RESTRICTED), si existe. */
  @Override
  Optional<String> findTenantStatus(UUID tenantId);

  /** Correo del administrador activo para comprobantes y avisos de vencimiento. */
  Optional<String> findAdministratorEmail(UUID tenantId);
}
