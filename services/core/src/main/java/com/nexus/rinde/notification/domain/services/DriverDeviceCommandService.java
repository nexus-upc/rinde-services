package com.nexus.rinde.notification.domain.services;

import com.nexus.rinde.notification.domain.model.commands.RegisterDriverDeviceCommand;
import com.nexus.rinde.notification.domain.model.commands.RemoveDriverDeviceCommand;

/** Comandos de registro y baja de dispositivos de conductores para avisos push. */
public interface DriverDeviceCommandService {

  void handle(RegisterDriverDeviceCommand command);

  void handle(RemoveDriverDeviceCommand command);
}
