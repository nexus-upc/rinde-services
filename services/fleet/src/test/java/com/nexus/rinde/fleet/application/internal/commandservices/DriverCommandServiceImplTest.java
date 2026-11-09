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
import com.nexus.rinde.fleet.domain.model.commands.LinkDriverAccountCommand;
import com.nexus.rinde.fleet.domain.model.commands.RegisterDriverCommand;
import com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories.DriverRepository;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

  private RegisterDriverCommand registerCommand(UUID linkedUserId) {
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

  private Driver driver(UUID linkedUserId) {
    return new Driver(
        tenantId,
        linkedUserId,
        "Luis Quispe",
        "DNI",
        "10203040",
        "Q10203040",
        "A-IIIc",
        LocalDate.now().plusYears(2),
        LocalDate.now());
  }

  private void noDuplicatedDocumentOrLicense() {
    when(driverRepository.existsByTenantIdAndDocumentNumber(any(), any())).thenReturn(false);
    when(driverRepository.existsByTenantIdAndLicenseNumber(any(), any())).thenReturn(false);
  }

  private void iamReports(String role, String status) {
    when(iamAccountClient.findUserAccount(tenantId, userId))
        .thenReturn(Optional.of(new IamUserAccount(userId, role, status)));
  }

  @Test
  void registersADriverWithoutAccountAndDoesNotAskIam() {
    noDuplicatedDocumentOrLicense();
    when(driverRepository.save(any(Driver.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Driver driver = service.handle(registerCommand(null));

    assertThat(driver.getUserId()).isNull();
    verify(iamAccountClient, never()).findUserAccount(any(), any());
  }

  @Test
  void rejectsAnAccountThatIamDoesNotFindInTheTenant() {
    noDuplicatedDocumentOrLicense();
    when(iamAccountClient.findUserAccount(tenantId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.handle(registerCommand(userId)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("La cuenta no existe en la empresa.");
    verify(driverRepository, never()).save(any());
  }

  @Test
  void rejectsAnAccountWhoseRoleIsNotDriver() {
    noDuplicatedDocumentOrLicense();
    iamReports("OPERATIONS_MANAGER", "ACTIVE");

    assertThatThrownBy(() -> service.handle(registerCommand(userId)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La cuenta debe tener el rol de conductor.");
    verify(driverRepository, never()).save(any());
  }

  @Test
  void rejectsAnAccountThatIsNotActive() {
    noDuplicatedDocumentOrLicense();
    iamReports("DRIVER", "INVITED");

    assertThatThrownBy(() -> service.handle(registerCommand(userId)))
        .isInstanceOf(ConflictException.class)
        .hasMessage("La cuenta del conductor no está activa.");
    verify(driverRepository, never()).save(any());
  }

  @Test
  void rejectsAnAccountAlreadyLinkedToAnotherDriver() {
    noDuplicatedDocumentOrLicense();
    iamReports("DRIVER", "ACTIVE");
    when(driverRepository.findAllByTenantIdAndUserId(tenantId, userId))
        .thenReturn(List.of(driver(userId)));

    assertThatThrownBy(() -> service.handle(registerCommand(userId)))
        .isInstanceOf(ConflictException.class)
        .hasMessage("La cuenta ya está vinculada a otro conductor.");
    verify(driverRepository, never()).save(any());
  }

  @Test
  void linksAFreeActiveDriverAccountOfTheTenant() {
    noDuplicatedDocumentOrLicense();
    iamReports("DRIVER", "ACTIVE");
    when(driverRepository.findAllByTenantIdAndUserId(tenantId, userId)).thenReturn(List.of());
    when(driverRepository.save(any(Driver.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Driver driver = service.handle(registerCommand(userId));

    assertThat(driver.getUserId()).isEqualTo(userId);
    assertThat(driver.getTenantId()).isEqualTo(tenantId);
  }

  @Test
  void unlinksTheAccountWithoutAskingIam() {
    Driver linked = driver(userId);
    when(driverRepository.findByIdAndTenantId(linked.getId(), tenantId))
        .thenReturn(Optional.of(linked));
    when(driverRepository.save(any(Driver.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Driver driver = service.handle(new LinkDriverAccountCommand(tenantId, linked.getId(), null));

    assertThat(driver.getUserId()).isNull();
    verify(iamAccountClient, never()).findUserAccount(any(), any());
  }

  @Test
  void relinkingTheSameAccountKeepsTheDriverUnchanged() {
    Driver linked = driver(userId);
    when(driverRepository.findByIdAndTenantId(linked.getId(), tenantId))
        .thenReturn(Optional.of(linked));
    iamReports("DRIVER", "ACTIVE");
    when(driverRepository.findAllByTenantIdAndUserId(tenantId, userId))
        .thenReturn(List.of(linked));
    when(driverRepository.save(any(Driver.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Driver driver = service.handle(new LinkDriverAccountCommand(tenantId, linked.getId(), userId));

    assertThat(driver.getUserId()).isEqualTo(userId);
  }

  @Test
  void linksAnotherActiveAccountToTheDriver() {
    Driver unlinked = driver(null);
    when(driverRepository.findByIdAndTenantId(unlinked.getId(), tenantId))
        .thenReturn(Optional.of(unlinked));
    iamReports("DRIVER", "ACTIVE");
    when(driverRepository.findAllByTenantIdAndUserId(tenantId, userId)).thenReturn(List.of());
    when(driverRepository.save(any(Driver.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Driver driver = service.handle(new LinkDriverAccountCommand(tenantId, unlinked.getId(), userId));

    assertThat(driver.getUserId()).isEqualTo(userId);
  }

  @Test
  void rejectsChangingToAnAccountLinkedToAnotherDriver() {
    Driver unlinked = driver(null);
    when(driverRepository.findByIdAndTenantId(unlinked.getId(), tenantId))
        .thenReturn(Optional.of(unlinked));
    iamReports("DRIVER", "ACTIVE");
    when(driverRepository.findAllByTenantIdAndUserId(tenantId, userId))
        .thenReturn(List.of(driver(userId)));

    assertThatThrownBy(
            () -> service.handle(new LinkDriverAccountCommand(tenantId, unlinked.getId(), userId)))
        .isInstanceOf(ConflictException.class)
        .hasMessage("La cuenta ya está vinculada a otro conductor.");
    assertThat(unlinked.getUserId()).isNull();
  }

  @Test
  void rejectsChangingToAnInactiveAccount() {
    Driver unlinked = driver(null);
    when(driverRepository.findByIdAndTenantId(unlinked.getId(), tenantId))
        .thenReturn(Optional.of(unlinked));
    iamReports("DRIVER", "DISABLED");

    assertThatThrownBy(
            () -> service.handle(new LinkDriverAccountCommand(tenantId, unlinked.getId(), userId)))
        .isInstanceOf(ConflictException.class)
        .hasMessage("La cuenta del conductor no está activa.");
    verify(driverRepository, never()).save(any());
  }

  @Test
  void rejectsLinkingAnUnknownDriver() {
    UUID driverId = UUID.randomUUID();
    when(driverRepository.findByIdAndTenantId(driverId, tenantId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.handle(new LinkDriverAccountCommand(tenantId, driverId, userId)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("El conductor no existe.");
  }
}
