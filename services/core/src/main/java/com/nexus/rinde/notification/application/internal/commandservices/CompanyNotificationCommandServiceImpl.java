package com.nexus.rinde.notification.application.internal.commandservices;

import com.nexus.rinde.notification.domain.model.commands.MarkAllNotificationsReadCommand;
import com.nexus.rinde.notification.domain.model.commands.MarkNotificationReadCommand;
import com.nexus.rinde.notification.domain.services.CompanyNotificationCommandService;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRepository;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.NotificationReadRepository;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Marca notificaciones como leídas para un usuario sin afectar a los demás de la empresa. */
@Service
public class CompanyNotificationCommandServiceImpl implements CompanyNotificationCommandService {

  private static final String NOT_FOUND = "La notificación no existe.";

  private final CompanyNotificationRepository notifications;
  private final NotificationReadRepository reads;
  private final Clock clock;

  public CompanyNotificationCommandServiceImpl(
      CompanyNotificationRepository notifications, NotificationReadRepository reads, Clock clock) {
    this.notifications = notifications;
    this.reads = reads;
    this.clock = clock;
  }

  /** Una segunda lectura no cambia el readAt guardado la primera vez. */
  @Override
  @Transactional
  public void handle(MarkNotificationReadCommand command) {
    if (!notifications.existsByIdAndTenantId(command.notificationId(), command.tenantId())) {
      throw new ResourceNotFoundException(NOT_FOUND);
    }
    reads.insertReadIfAbsent(
        command.notificationId(), command.userId(), command.tenantId(), clock.instant());
  }

  @Override
  @Transactional
  public void handle(MarkAllNotificationsReadCommand command) {
    reads.insertReadsForAllOfTenant(command.userId(), command.tenantId(), clock.instant());
  }
}
