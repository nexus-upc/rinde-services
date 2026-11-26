package com.nexus.rinde.notification.application.internal.commandservices;

import com.nexus.rinde.notification.domain.model.valueobjects.PaymentReceiptNotice;
import com.nexus.rinde.notification.domain.model.valueobjects.SubscriptionExpiringNotice;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Envía y registra comprobantes y avisos mediante el correo simulado del proyecto. */
@Service
public class SubscriptionEmailNotificationService {

  private final AdministratorEmailOutbox emailOutbox;

  public SubscriptionEmailNotificationService(AdministratorEmailOutbox emailOutbox) {
    this.emailOutbox = emailOutbox;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(SubscriptionExpiringNotice event) {
    String expiry = DateTimeFormatter.ISO_INSTANT.format(event.expiresAt());
    emailOutbox.queueOnce(
        event.eventId(),
        event.tenantId(),
        "Tu suscripción vence pronto",
        "Tu suscripción vence el " + expiry + ". Renueva el plan para mantener habilitada la empresa.");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(PaymentReceiptNotice event) {
    String body =
        "Pago simulado confirmado. Referencia: "
            + event.providerReference()
            + ". Importe: "
            + event.amount().toPlainString()
            + ". Periodo: "
            + DateTimeFormatter.ISO_INSTANT.format(event.startsAt())
            + " a "
            + DateTimeFormatter.ISO_INSTANT.format(event.expiresAt())
            + ". RINDE no recibió ni almacenó datos de tarjeta.";
    emailOutbox.queueOnce(
        event.eventId(), event.tenantId(), "Comprobante de suscripción RINDE", body);
  }
}
