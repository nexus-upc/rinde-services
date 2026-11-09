package com.nexus.rinde.fleet.application.internal.commandservices;

import com.nexus.rinde.fleet.application.internal.outboundservices.acl.IamAccountClient;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.IamUserAccount;
import com.nexus.rinde.fleet.domain.model.aggregates.Driver;
import com.nexus.rinde.fleet.domain.model.commands.RegisterDriverCommand;
import com.nexus.rinde.fleet.domain.services.DriverCommandService;
import com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories.DriverRepository;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación del servicio de aplicación de comandos para Driver. */
@Service
@Transactional
public class DriverCommandServiceImpl implements DriverCommandService {

  private static final String DRIVER_ROLE = "DRIVER";

  private final DriverRepository driverRepository;
  private final IamAccountClient iamAccountClient;

  public DriverCommandServiceImpl(
      DriverRepository driverRepository, IamAccountClient iamAccountClient) {
    this.driverRepository = driverRepository;
    this.iamAccountClient = iamAccountClient;
  }

  @Override
  public Driver handle(RegisterDriverCommand command) {
    if (driverRepository.existsByTenantIdAndDocumentNumber(
        command.tenantId(), command.documentNumber().trim())) {
      throw new ConflictException("El documento ya se encuentra registrado en la empresa.");
    }

    if (driverRepository.existsByTenantIdAndLicenseNumber(
        command.tenantId(), command.licenseNumber().trim().toUpperCase())) {
      throw new ConflictException("La licencia ya se encuentra registrada en la empresa.");
    }

    validateAccountLink(command.tenantId(), command.userId());

    Driver driver =
        new Driver(
            command.tenantId(),
            command.userId(),
            command.fullName(),
            command.documentType(),
            command.documentNumber(),
            command.licenseNumber(),
            command.licenseCategory(),
            command.licenseExpirationDate(),
            LocalDate.now());

    return driverRepository.save(driver);
  }

  /**
   * Comprueba en IAM que la cuenta sea de la empresa, tenga rol DRIVER y no esté vinculada a otro
   * conductor. Sin cuenta indicada no hay nada que comprobar.
   */
  private void validateAccountLink(UUID tenantId, UUID userId) {
    if (userId == null) {
      return;
    }
    IamUserAccount account =
        iamAccountClient
            .findUserAccount(tenantId, userId)
            .orElseThrow(
                () -> new ResourceNotFoundException("La cuenta no existe en la empresa."));
    if (!DRIVER_ROLE.equals(account.role())) {
      throw new BusinessRuleException("La cuenta debe tener el rol de conductor.");
    }
    if (driverRepository.existsByTenantIdAndUserId(tenantId, userId)) {
      throw new ConflictException("La cuenta ya está vinculada a otro conductor.");
    }
  }
}
