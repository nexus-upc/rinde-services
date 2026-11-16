package com.nexus.rinde.iam.application.internal.queryservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.nexus.rinde.iam.domain.model.aggregates.User;
import com.nexus.rinde.iam.domain.model.queries.GetUserByIdQuery;
import com.nexus.rinde.iam.domain.model.queries.GetUsersByTenantQuery;
import com.nexus.rinde.iam.domain.model.valueobjects.Email;
import com.nexus.rinde.iam.domain.model.valueobjects.PasswordHash;
import com.nexus.rinde.iam.domain.model.valueobjects.Role;
import com.nexus.rinde.iam.domain.model.valueobjects.TenantId;
import com.nexus.rinde.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

  @Mock private UserRepository userRepository;
  @InjectMocks private UserQueryServiceImpl service;

  private final TenantId tenantId = new TenantId(UUID.randomUUID());

  private User user(TenantId owner, String fullName, String email) {
    return User.createAdministrator(owner, fullName, new Email(email), new PasswordHash("h"));
  }

  @Test
  void listsTheUsersOfTheTenantSortedByName() {
    User zoe = user(tenantId, "Zoe Rivas", "zoe@andes.pe");
    User ana = user(tenantId, "ana Rojas", "ana@andes.pe");
    when(userRepository.findByTenantId(tenantId)).thenReturn(List.of(zoe, ana));

    List<User> users = service.handle(new GetUsersByTenantQuery(tenantId));

    assertThat(users).containsExactly(ana, zoe);
  }

  @Test
  void listsOnlyTheUsersOfTheRequestedRole() {
    User admin = user(tenantId, "Ana Rojas", "ana@andes.pe");
    User driver = new User(tenantId, "Luis Quispe", new Email("luis@andes.pe"), Role.DRIVER);
    when(userRepository.findByTenantId(tenantId)).thenReturn(List.of(admin, driver));

    List<User> drivers = service.handle(new GetUsersByTenantQuery(tenantId, Role.DRIVER));

    assertThat(drivers).containsExactly(driver);
  }

  @Test
  void findsAUserOfTheTenant() {
    User ana = user(tenantId, "Ana Rojas", "ana@andes.pe");
    when(userRepository.findById(ana.getId())).thenReturn(Optional.of(ana));

    assertThat(service.handle(new GetUserByIdQuery(tenantId, ana.getUserId()))).contains(ana);
  }

  @Test
  void doesNotFindAUserOfAnotherTenant() {
    User outsider = user(new TenantId(UUID.randomUUID()), "Otro", "otro@otra.pe");
    when(userRepository.findById(outsider.getId())).thenReturn(Optional.of(outsider));

    assertThat(service.handle(new GetUserByIdQuery(tenantId, outsider.getUserId()))).isEmpty();
  }
}
