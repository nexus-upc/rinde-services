package com.nexus.rinde.notification.infrastructure.messaging;

import com.nexus.rinde.notification.application.events.TripFinishedNotification;
import com.nexus.rinde.notification.application.internal.commandservices.CompanyNotificationRecorder;
import com.nexus.rinde.notification.domain.model.valueobjects.CompanyNotice;
import com.nexus.rinde.notification.domain.model.valueobjects.NotificationType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Crea el aviso de viaje listo para liquidar; una entrega repetida del mismo eventId no crea otro. */
@Component
public class TripFinishedListener {

  private final CompanyNotificationRecorder recorder;

  public TripFinishedListener(CompanyNotificationRecorder recorder) {
    this.recorder = recorder;
  }

  @RabbitListener(
      queues = "${rinde.messaging.notification.trip-finished-queue:notification.trip-finished}")
  public void on(TripFinishedNotification event) {
    recorder.recordOnce(
        new CompanyNotice(
            event.eventId(),
            event.tenantId(),
            NotificationType.SETTLEMENT_READY,
            "Viaje listo para liquidar",
            "El viaje finalizó y está listo para liquidar.",
            "/trips/" + event.tripId(),
            event.occurredAt()));
  }
}
