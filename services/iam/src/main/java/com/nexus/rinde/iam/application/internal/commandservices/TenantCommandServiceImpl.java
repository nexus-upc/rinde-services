package com.nexus.rinde.iam.application.internal.commandservices;

import com.nexus.rinde.iam.application.internal.TokenValidityProperties;
import com.nexus.rinde.iam.domain.model.aggregates.Tenant;
import com.nexus.rinde.iam.domain.model.aggregates.User;
import com.nexus.rinde.iam.domain.model.commands.ReactivateTenantCommand;
import com.nexus.rinde.iam.domain.model.commands.RegisterTenantCommand;
import com.nexus.rinde.iam.domain.model.commands.RestrictTenantCommand;
import com.nexus.rinde.iam.domain.model.commands.UpdateCurrentTenantProfileCommand;
import com.nexus.rinde.iam.domain.model.commands.VerifyTenantCommand;
import com.nexus.rinde.iam.domain.model.valueobjects.AccessToken;
import com.nexus.rinde.iam.domain.model.valueobjects.Email;
import com.nexus.rinde.iam.domain.model.valueobjects.IssuedAccessToken;
import com.nexus.rinde.iam.domain.model.valueobjects.PasswordHash;
import com.nexus.rinde.iam.domain.model.valueobjects.Ruc;
import com.nexus.rinde.iam.domain.model.valueobjects.TenantId;
import com.nexus.rinde.iam.domain.model.valueobjects.TenantStatus;
import com.nexus.rinde.iam.domain.model.valueobjects.TokenPurpose;
import com.nexus.rinde.iam.domain.services.TenantCommandService;
import com.nexus.rinde.iam.infrastructure.persistence.jpa.repositories.TenantRepository;
import com.nexus.rinde.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Registra empresas, confirma su correo y aplica las restricciones que llegan desde la suscripción.
 */
@Service
public class TenantCommandServiceImpl implements TenantCommandService {

  private static final Logger log = LoggerFactory.getLogger(TenantCommandServiceImpl.class);

  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenValidityProperties tokenValidity;
  private final Clock clock;

  public TenantCommandServiceImpl(
      TenantRepository tenantRepository,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      TokenValidityProperties tokenValidity,
      Clock clock) {
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenValidity = tokenValidity;
    this.clock = clock;
  }

  @Override
  public Tenant handle(UpdateCurrentTenantProfileCommand command) {
    throw new ResponseStatusException(
        HttpStatus.NOT_IMPLEMENTED, "Funcionalidad pendiente de implementar en S4-28.");
  }

  /**
   * Crea la empresa pendiente de verificación y a su administrador, ya activo con su contraseña.
   */
  @Override
  @Transactional
  public Tenant handle(RegisterTenantCommand command) {
    Ruc ruc = new Ruc(command.ruc());
    Email administratorEmail = new Email(command.administratorEmail());
    if (tenantRepository.existsByRuc(ruc)) {
      throw new ConflictException("Ya existe una empresa registrada con ese RUC.");
    }
    if (userRepository.existsByEmail(administratorEmail)) {
      throw new ConflictException("Ya existe un usuario registrado con ese correo.");
    }
    Instant now = clock.instant();
    Tenant tenant = new Tenant(command.tradeName(), ruc);
    User administrator =
        User.createAdministrator(
            tenant.getTenantId(),
            command.administratorFullName(),
            administratorEmail,
            new PasswordHash(passwordEncoder.encode(command.password())));
    IssuedAccessToken verification =
        administrator.issueAccessToken(
            TokenPurpose.EMAIL_VERIFICATION, tokenValidity.emailVerificationValidity(), now);
    tenant.register(administratorEmail, verification, now);
    try {
      tenantRepository.saveAndFlush(tenant);
      userRepository.saveAndFlush(administrator);
    } catch (DataIntegrityViolationException ex) {
      // Dos registros simultáneos pueden pasar la validación previa; la base garantiza la unicidad.
      throw new ConflictException("El RUC o el correo ya están registrados.");
    }
    return tenant;
  }

  /** Activa la empresa con el enlace enviado al correo del administrador. */
  @Override
  @Transactional
  public Tenant handle(VerifyTenantCommand command) {
    Tenant tenant = findTenant(command.tenantId());
    User administrator =
        userRepository
            .findByAccessTokenHash(AccessToken.hash(command.token()))
            .filter(user -> user.belongsTo(command.tenantId()))
            .orElseThrow(() -> new BusinessRuleException(User.INVALID_LINK_MESSAGE));
    administrator.consumeAccessToken(
        command.token(), clock.instant(), TokenPurpose.EMAIL_VERIFICATION);
    tenant.verify();
    userRepository.save(administrator);
    return tenantRepository.save(tenant);
  }

  /** Es idempotente: si la empresa no está activa se ignora, porque el evento puede repetirse. */
  @Override
  @Transactional
  public void handle(RestrictTenantCommand command) {
    Tenant tenant = findTenant(command.tenantId());
    if (tenant.getStatus() != TenantStatus.ACTIVE) {
      log.info(
          "Se ignora la restricción de la empresa {}: estado {}",
          tenant.getId(),
          tenant.getStatus());
      return;
    }
    tenant.restrict();
    tenantRepository.save(tenant);
  }

  /**
   * Es idempotente: si la empresa no está restringida se ignora, porque el evento puede repetirse.
   */
  @Override
  @Transactional
  public void handle(ReactivateTenantCommand command) {
    Tenant tenant = findTenant(command.tenantId());
    if (tenant.getStatus() != TenantStatus.RESTRICTED) {
      log.info(
          "Se ignora la reactivación de la empresa {}: estado {}",
          tenant.getId(),
          tenant.getStatus());
      return;
    }
    tenant.reactivate();
    tenantRepository.save(tenant);
  }

  private Tenant findTenant(TenantId tenantId) {
    return tenantRepository
        .findById(tenantId.value())
        .orElseThrow(() -> new ResourceNotFoundException("La empresa no existe."));
  }
}
