package com.nexus.rinde.subscription.application.internal.commandservices;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Ejecuta la política de vencimiento a diario; las pruebas llaman directamente al servicio. */
@Component
public class SubscriptionLifecycleJob {

  private final SubscriptionLifecycleService lifecycleService;

  public SubscriptionLifecycleJob(SubscriptionLifecycleService lifecycleService) {
    this.lifecycleService = lifecycleService;
  }

  @Scheduled(cron = "${rinde.subscription.lifecycle.cron:0 0 3 * * *}")
  public void runDaily() {
    lifecycleService.advanceSubscriptions();
  }
}
