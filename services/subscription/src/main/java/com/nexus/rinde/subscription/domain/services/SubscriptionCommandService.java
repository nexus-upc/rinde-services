package com.nexus.rinde.subscription.domain.services;

import com.nexus.rinde.subscription.domain.model.aggregates.Subscription;
import com.nexus.rinde.subscription.domain.model.commands.ChoosePlanCommand;

/** Casos de uso que modifican suscripciones. */
public interface SubscriptionCommandService {

  Subscription handle(ChoosePlanCommand command);
}
