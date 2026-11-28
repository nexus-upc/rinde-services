package com.nexus.rinde.notification.infrastructure.messaging;

import com.nexus.rinde.notification.application.events.IncidentReportedNotification;
import com.nexus.rinde.notification.application.internal.commandservices.CompanyNotificationRecorder;
import com.nexus.rinde.notification.domain.model.valueobjects.CompanyNotice;
import com.nexus.rinde.notification.domain.model.valueobjects.NotificationType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Crea el aviso de incidencia reportada; una entrega repetida del mismo eventId no crea otro. */
@Component
public class IncidentReportedListener {

  private final CompanyNotificationRecorder recorder;

  public IncidentReportedListener(CompanyNotificationRecorder recorder) {
    this.recorder = recorder;
  }

  @RabbitListener(
      queues = "${rinde.messaging.notification.incident-queue:notification.incident-reported}")
  public void on(IncidentReportedNotification event) {
    recorder.recordOnce(
        new CompanyNotice(
            event.eventId(),
            event.tenantId(),
            NotificationType.INCIDENT_REPORTED,
            "Incidencia reportada",
            incidentMessage(event.incidentType()),
            "/incidents/" + event.incidentId(),
            event.occurredAt()));
  }

  /** Mensaje en español sin mostrar el tipo interno de la incidencia. */
  static String incidentMessage(String incidentType) {
    String detail =
        switch (incidentType == null ? "" : incidentType) {
          case "BREAKDOWN" -> " por falla mecánica";
          case "DELAY" -> " por demora o retraso";
          default -> "";
        };
    return "Se reportó una incidencia" + detail + " para revisar.";
  }
}
