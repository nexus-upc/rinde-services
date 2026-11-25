package com.nexus.rinde.notification.application.internal.commandservices;

import com.nexus.rinde.notification.domain.model.commands.RegisterDriverDeviceCommand;
import com.nexus.rinde.notification.domain.model.commands.RemoveDriverDeviceCommand;
import com.nexus.rinde.notification.domain.services.DriverDeviceCommandService;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.DriverDeviceEntity;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.DriverDeviceRepository;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestiona el registro idempotente y la baja segura de dispositivos del conductor. */
@Service
public class DriverDeviceCommandServiceImpl implements DriverDeviceCommandService {

  private static final int MAX_TOKEN_LENGTH = 512;
  private static final String SUPPORTED_PLATFORM = "ANDROID";

  private final DriverDeviceRepository driverDeviceRepository;
  private final Clock clock;

  public DriverDeviceCommandServiceImpl(
      DriverDeviceRepository driverDeviceRepository, Clock clock) {
    this.driverDeviceRepository = driverDeviceRepository;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void handle(RegisterDriverDeviceCommand command) {
    if (command == null || command.tenantId() == null || command.userId() == null) {
      throw new BusinessRuleException("La empresa y el usuario autenticado son obligatorios.");
    }
    String token = normalizeToken(command.token());
    String platform = normalizePlatform(command.platform());
    Instant now = clock.instant();

    DriverDeviceEntity device =
        driverDeviceRepository
            .findByToken(token)
            .map(
                existing -> {
                  existing.reassign(command.tenantId(), command.userId(), platform, now);
                  return existing;
                })
            .orElseGet(
                () ->
                    DriverDeviceEntity.register(
                        command.tenantId(), command.userId(), token, platform, now));
    driverDeviceRepository.save(device);
  }

  @Override
  @Transactional
  public void handle(RemoveDriverDeviceCommand command) {
    if (command == null || command.tenantId() == null || command.userId() == null) {
      throw new BusinessRuleException("La empresa y el usuario autenticado son obligatorios.");
    }
    if (command.token() == null || command.token().isBlank()) {
      return;
    }
    String token = command.token().trim();
    driverDeviceRepository
        .findByToken(token)
        .filter(
            device ->
                device.getTenantId().equals(command.tenantId())
                    && device.getUserId().equals(command.userId()))
        .ifPresent(driverDeviceRepository::delete);
  }

  private static String normalizeToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new BusinessRuleException("El token del dispositivo es obligatorio.");
    }
    String trimmed = rawToken.trim();
    if (trimmed.length() > MAX_TOKEN_LENGTH) {
      throw new BusinessRuleException(
          "El token del dispositivo no puede superar los 512 caracteres.");
    }
    return trimmed;
  }

  private static String normalizePlatform(String rawPlatform) {
    if (rawPlatform == null || rawPlatform.isBlank()) {
      throw new BusinessRuleException("La plataforma es obligatoria.");
    }
    String trimmed = rawPlatform.trim();
    if (!SUPPORTED_PLATFORM.equals(trimmed)) {
      throw new BusinessRuleException("La plataforma debe ser ANDROID.");
    }
    return trimmed;
  }
}
