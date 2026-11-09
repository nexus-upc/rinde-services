package com.nexus.rinde.fleet.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.rinde.fleet.application.internal.outboundservices.acl.IamAccountClient;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.IamUserAccount;
import com.nexus.rinde.fleet.domain.model.aggregates.Driver;
import com.nexus.rinde.fleet.domain.model.commands.RegisterDriverCommand;
import com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories.DriverRepository;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverCommandServiceImplTest {

  @Mock private DriverRepository driverRepository;
  @Mock private IamAccountClient iamAccountClient;
  @InjectMocks private DriverCommandServiceImpl service;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void noDuplicatedDocumentOrLicense() {
    when(driverRepository.existsByTenantIdAndDocumentNumber(any(), any())).thenReturn(false);
    when(driverRepository.existsByTenantIdAndLicenseNumber(any(), any())).thenReturn(false);
  }

  private RegisterDriverCommand command(UUID linkedUserId) {
    return new RegisterDriverCommand(
        tenantId,
        linkedUserId,
        "Luis Quispe",
        "DNI",
        "10203040",
        "Q10203040",
        "A-IIIc",
        LocalDate.now().plusYears(2));
  }

  @Test
  void registersADriverWithoutAccountAndDoesNotAskIam() {
    when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Driver driver = service.handle(command(null));

    assertThat(driver.getUserId()).isNull();
    verify(iamAccountClient, never()).findUserAccount(any(), any());
  }

  @Test
  void rejectsAnAccountThatIamDoesNotFindInTheTenant() {
    when(iamAccountClient.findUserAccount(tenantId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.handle(command(userId)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("La cuenta no existe en la empresa.");
    verify(driverRepository, never()).save(any());
  }

  @Test
  void rejectsAnAccountWhoseRoleIsNotDriver() {
    when(iamAccountClient.findUserAccount(tenantId, userId))
        .thenReturn(Optional.of(new IamUserAccount(userId, "OPERATIONS_MANAGER")));

    assertThatThrownBy(() -> service.handle(command(userId)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La cuenta debe tener el rol de conductor.");
    verify(driverRepository, never()).save(any());
  }

  @Test
  void rejectsAnAccountAlreadyLinkedToAnotherDriver() {
    when(iamAccountClient.findUserAccount(tenantId, userId))
        .thenReturn(Optional.of(new IamUserAccount(userId, "DRIVER")));
    when(driverRepository.existsByTenantIdAndUserId(tenantId, userId)).thenReturn(true);

    assertThatThrownBy(() -> service.handle(command(userId)))
        .isInstanceOf(ConflictException.class)
        .hasMessage("La cuenta ya está vinculada a otro conductor.");
    verify(driverRepository, never()).save(any());
  }

  @Test
  void linksAFreeDriverAccountOfTheTenant() {
    when(iamAccountClient.findUserAccount(tenantId, userId))
        .thenReturn(Optional.of(new IamUserAccount(userId, "DRIVER")));
    when(driverRepository.existsByTenantIdAndUserId(tenantId, userId)).thenReturn(false);
    when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Driver driver = service.handle(command(userId));

    assertThat(driver.getUserId()).isEqualTo(userId);
    assertThat(driver.getTenantId()).isEqualTo(tenantId);
  }
}
