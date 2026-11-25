package com.nexus.rinde.notification.domain.services;

import com.nexus.rinde.notification.domain.model.commands.MarkAllNotificationsReadCommand;
import com.nexus.rinde.notification.domain.model.commands.MarkNotificationReadCommand;

/** Comandos de lectura sobre el centro de notificaciones de la empresa. */
public interface CompanyNotificationCommandService {

  void handle(MarkNotificationReadCommand command);

  void handle(MarkAllNotificationsReadCommand command);
}
