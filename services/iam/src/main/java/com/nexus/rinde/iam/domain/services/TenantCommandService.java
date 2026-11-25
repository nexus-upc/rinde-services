package com.nexus.rinde.iam.domain.services;

import com.nexus.rinde.iam.domain.model.aggregates.Tenant;
import com.nexus.rinde.iam.domain.model.commands.ReactivateTenantCommand;
import com.nexus.rinde.iam.domain.model.commands.RegisterTenantCommand;
import com.nexus.rinde.iam.domain.model.commands.RestrictTenantCommand;
import com.nexus.rinde.iam.domain.model.commands.UpdateCurrentTenantProfileCommand;
import com.nexus.rinde.iam.domain.model.commands.VerifyTenantCommand;

/** Casos de uso que modifican empresas: registro, verificación y cambios por la suscripción. */
public interface TenantCommandService {

  Tenant handle(RegisterTenantCommand command);

  Tenant handle(VerifyTenantCommand command);

  void handle(RestrictTenantCommand command);

  void handle(ReactivateTenantCommand command);

  Tenant handle(UpdateCurrentTenantProfileCommand command);
}
