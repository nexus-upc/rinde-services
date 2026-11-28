package com.nexus.rinde.iam.application.internal.eventhandlers;

import com.nexus.rinde.iam.application.internal.outboundservices.AccessMailPort;
import com.nexus.rinde.iam.domain.model.aggregates.Tenant;
import com.nexus.rinde.iam.domain.model.events.PasswordResetRequested;
import com.nexus.rinde.iam.domain.model.events.TenantRegistered;
import com.nexus.rinde.iam.domain.model.events.UserInvited;
import com.nexus.rinde.iam.infrastructure.mail.MailDeliveryException;
import com.nexus.rinde.iam.infrastructure.persistence.jpa.repositories.TenantRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía los correos de acceso tras el commit, así no sale ningún correo de un alta revertida. Un
 * fallo se registra sin relanzarlo: el usuario ya fue creado.
 */
@Component
@ConditionalOnProperty(name = "rinde.mail.provider", havingValue = "azure")
public class AccessMailEventHandler {

  private static final Logger log = LoggerFactory.getLogger(AccessMailEventHandler.class);

  private final AccessMailPort mail;
  private final TenantRepository tenants;

  public AccessMailEventHandler(AccessMailPort mail, TenantRepository tenants) {
    this.mail = mail;
    this.tenants = tenants;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(TenantRegistered event) {
    deliver(
        "verificación de empresa",
        event.eventId(),
        event.administratorEmail(),
        () ->
            mail.sendTenantVerification(
                event.tenantId(),
                event.tradeName(),
                event.administratorEmail(),
                event.verificationToken()));
  }

  /** Busca el nombre de la empresa en una transacción nueva, porque el alta ya fue confirmada. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public void on(UserInvited event) {
    deliver(
        "invitación de usuario",
        event.eventId(),
        event.email(),
        () ->
            mail.sendUserInvitation(
                event.tenantId(),
                companyName(event.tenantId()),
                event.email(),
                event.invitationToken()));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(PasswordResetRequested event) {
    deliver(
        "recuperación de acceso",
        event.eventId(),
        event.email(),
        () -> mail.sendPasswordReset(event.tenantId(), event.email(), event.resetToken()));
  }

  private String companyName(UUID tenantId) {
    return tenants
        .findById(tenantId)
        .map(Tenant::getTradeName)
        .orElseThrow(() -> new IllegalStateException("No se encontró la empresa invitante."));
  }

  private static void deliver(String kind, UUID eventId, String recipient, Runnable send) {
    try {
      send.run();
    } catch (RuntimeException ex) {
      // Sin el token, el enlace ni el correo completo; el error no se pasa como causa al log.
      log.error(
          "No se pudo enviar el correo de {} (evento {}) a {}. Motivo: {}",
          kind,
          eventId,
          maskEmail(recipient),
          reason(ex));
    }
  }

  private static String reason(RuntimeException ex) {
    return ex instanceof MailDeliveryException ? ex.getMessage() : ex.getClass().getSimpleName();
  }

  /** Deja ver la primera letra y el dominio, por ejemplo a***@dominio.pe. */
  static String maskEmail(String email) {
    int at = email == null ? -1 : email.indexOf('@');
    if (at <= 0) {
      return "***";
    }
    return email.charAt(0) + "***" + email.substring(at);
  }
}
