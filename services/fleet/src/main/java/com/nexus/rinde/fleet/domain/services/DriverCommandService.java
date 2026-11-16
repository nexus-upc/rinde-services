package com.nexus.rinde.fleet.domain.services;

import com.nexus.rinde.fleet.domain.model.aggregates.Driver;
import com.nexus.rinde.fleet.domain.model.commands.LinkDriverAccountCommand;
import com.nexus.rinde.fleet.domain.model.commands.RegisterDriverCommand;

/** Servicio de aplicación para comandos sobre el agregado Driver. */
public interface DriverCommandService {

  Driver handle(RegisterDriverCommand command);

  Driver handle(LinkDriverAccountCommand command);
}
