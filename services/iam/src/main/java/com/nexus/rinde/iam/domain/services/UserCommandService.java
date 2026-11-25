package com.nexus.rinde.iam.domain.services;

import com.nexus.rinde.iam.domain.model.aggregates.User;
import com.nexus.rinde.iam.domain.model.commands.ChangeCurrentUserPasswordCommand;
import com.nexus.rinde.iam.domain.model.commands.InviteUserCommand;
import com.nexus.rinde.iam.domain.model.commands.UpdateCurrentUserProfileCommand;
import com.nexus.rinde.iam.domain.model.commands.UpdateUserCommand;

/** Casos de uso que modifican usuarios de una empresa: invitar, cambiar el rol y deshabilitar. */
public interface UserCommandService {

  User handle(InviteUserCommand command);

  User handle(UpdateUserCommand command);

  User handle(UpdateCurrentUserProfileCommand command);

  void handle(ChangeCurrentUserPasswordCommand command);
}
