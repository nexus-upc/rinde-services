package com.nexus.rinde.iam.application.internal.commandservices;

import com.nexus.rinde.iam.application.internal.TokenValidityProperties;
import com.nexus.rinde.iam.domain.model.aggregates.User;
import com.nexus.rinde.iam.domain.model.commands.ChangeCurrentUserPasswordCommand;
import com.nexus.rinde.iam.domain.model.commands.InviteUserCommand;
import com.nexus.rinde.iam.domain.model.commands.UpdateCurrentUserProfileCommand;
import com.nexus.rinde.iam.domain.model.commands.UpdateUserCommand;
import com.nexus.rinde.iam.domain.model.valueobjects.Email;
import com.nexus.rinde.iam.domain.services.UserCommandService;
import com.nexus.rinde.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Invita, cambia el rol y deshabilita usuarios de la empresa del administrador. TODO: el diseño no
 * dice si el último administrador puede deshabilitarse o cambiar de rol.
 */
@Service
public class UserCommandServiceImpl implements UserCommandService {

  private static final String EMAIL_TAKEN = "Ya existe un usuario registrado con ese correo.";

  private final UserRepository userRepository;
  private final TokenValidityProperties tokenValidity;
  private final Clock clock;

  public UserCommandServiceImpl(
      UserRepository userRepository, TokenValidityProperties tokenValidity, Clock clock) {
    this.userRepository = userRepository;
    this.tokenValidity = tokenValidity;
    this.clock = clock;
  }

  @Override
  @Transactional
  public User handle(InviteUserCommand command) {
    Email email = new Email(command.email());
    if (userRepository.existsByEmail(email)) {
      throw new ConflictException(EMAIL_TAKEN);
    }
    User user = new User(command.tenantId(), command.fullName(), email, command.role());
    user.invite(tokenValidity.invitationValidity(), clock.instant());
    try {
      return userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException ex) {
      // Dos invitaciones simultáneas pueden pasar la validación previa; la base garantiza la
      // unicidad.
      throw new ConflictException(EMAIL_TAKEN);
    }
  }

  @Override
  @Transactional
  public User handle(UpdateUserCommand command) {
    if (command.role() == null && !command.disable()) {
      throw new BusinessRuleException("Indique el nuevo rol o que el usuario debe deshabilitarse.");
    }
    User user =
        userRepository
            .findById(command.userId().value())
            .filter(found -> found.belongsTo(command.tenantId()))
            .orElseThrow(() -> new ResourceNotFoundException("El usuario no existe."));
    if (command.role() != null) {
      user.changeRole(command.role());
    }
    if (command.disable()) {
      user.disable();
    }
    return userRepository.save(user);
  }

  @Override
  public User handle(UpdateCurrentUserProfileCommand command) {
    throw new ResponseStatusException(
        HttpStatus.NOT_IMPLEMENTED, "Funcionalidad pendiente de implementar en S4-28.");
  }

  @Override
  public void handle(ChangeCurrentUserPasswordCommand command) {
    throw new ResponseStatusException(
        HttpStatus.NOT_IMPLEMENTED, "Funcionalidad pendiente de implementar en S4-28.");
  }
}
