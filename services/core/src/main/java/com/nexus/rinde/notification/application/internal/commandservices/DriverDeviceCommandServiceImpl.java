package com.nexus.rinde.notification.application.internal.commandservices;

import com.nexus.rinde.notification.domain.model.commands.RegisterDriverDeviceCommand;
import com.nexus.rinde.notification.domain.model.commands.RemoveDriverDeviceCommand;
import com.nexus.rinde.notification.domain.services.DriverDeviceCommandService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Implementación base pendiente del registro de dispositivos push (S4-24). */
@Service
public class DriverDeviceCommandServiceImpl implements DriverDeviceCommandService {

  @Override
  public void handle(RegisterDriverDeviceCommand command) {
    throw new ResponseStatusException(
        HttpStatus.NOT_IMPLEMENTED, "Funcionalidad pendiente de implementar en S4-24.");
  }

  @Override
  public void handle(RemoveDriverDeviceCommand command) {
    throw new ResponseStatusException(
        HttpStatus.NOT_IMPLEMENTED, "Funcionalidad pendiente de implementar en S4-24.");
  }
}
