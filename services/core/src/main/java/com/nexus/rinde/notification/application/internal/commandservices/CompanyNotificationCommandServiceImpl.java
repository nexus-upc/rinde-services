package com.nexus.rinde.notification.application.internal.commandservices;

import com.nexus.rinde.notification.domain.model.commands.MarkAllNotificationsReadCommand;
import com.nexus.rinde.notification.domain.model.commands.MarkNotificationReadCommand;
import com.nexus.rinde.notification.domain.services.CompanyNotificationCommandService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Implementación base pendiente del centro de notificaciones (S4-25). */
@Service
public class CompanyNotificationCommandServiceImpl implements CompanyNotificationCommandService {

  @Override
  public void handle(MarkNotificationReadCommand command) {
    throw new ResponseStatusException(
        HttpStatus.NOT_IMPLEMENTED, "Funcionalidad pendiente de implementar en S4-25.");
  }

  @Override
  public void handle(MarkAllNotificationsReadCommand command) {
    throw new ResponseStatusException(
        HttpStatus.NOT_IMPLEMENTED, "Funcionalidad pendiente de implementar en S4-25.");
  }
}
