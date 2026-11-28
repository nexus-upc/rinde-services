package com.nexus.rinde.notification.infrastructure.messaging;

import com.nexus.rinde.notification.application.events.ExpenseRegisteredNotification;
import com.nexus.rinde.notification.application.internal.commandservices.CompanyNotificationRecorder;
import com.nexus.rinde.notification.domain.model.valueobjects.CompanyNotice;
import com.nexus.rinde.notification.domain.model.valueobjects.NotificationType;
import java.math.RoundingMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Crea el aviso de gasto por revisar; una entrega repetida del mismo eventId no crea otro. */
@Component
public class ExpenseRegisteredListener {

  private final CompanyNotificationRecorder recorder;

  public ExpenseRegisteredListener(CompanyNotificationRecorder recorder) {
    this.recorder = recorder;
  }

  @RabbitListener(
      queues = "${rinde.messaging.notification.expense-queue:notification.expense-registered}")
  public void on(ExpenseRegisteredNotification event) {
    String amount = event.amount().setScale(2, RoundingMode.HALF_UP).toPlainString();
    recorder.recordOnce(
        new CompanyNotice(
            event.eventId(),
            event.tenantId(),
            NotificationType.EXPENSE_PENDING_REVIEW,
            "Gasto pendiente de revisión",
            "Se registró un gasto por " + event.currency() + " " + amount + " para revisar.",
            "/expenses?tripId=" + event.tripId(),
            event.occurredAt()));
  }
}
