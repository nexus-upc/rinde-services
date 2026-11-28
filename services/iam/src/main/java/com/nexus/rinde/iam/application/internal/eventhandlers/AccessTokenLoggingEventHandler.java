package com.nexus.rinde.iam.application.internal.eventhandlers;

import com.nexus.rinde.iam.domain.model.events.PasswordResetRequested;
import com.nexus.rinde.iam.domain.model.events.TenantRegistered;
import com.nexus.rinde.iam.domain.model.events.UserInvited;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Registra los enlaces de acceso: INFO con el token enmascarado, DEBUG con el enlace completo de la
 * web. TODO: eliminar cuando Notifications envíe los correos con estos eventos.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "rinde.demo.mailbox.enabled",
    havingValue = "false",
    matchIfMissing = true)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "rinde.mail.provider",
    havingValue = "demo",
    matchIfMissing = true)
public class AccessTokenLoggingEventHandler {

  private static final Logger log = LoggerFactory.getLogger(AccessTokenLoggingEventHandler.class);
  private static final String MASK = "****";
  private static final int VISIBLE_CHARACTERS = 4;

  private final String webBaseUrl;

  /** La URL de la web viene de rinde.web.base-url; sin la barra final para armar los enlaces. */
  public AccessTokenLoggingEventHandler(@Value("${rinde.web.base-url}") String webBaseUrl) {
    this.webBaseUrl = webBaseUrl.replaceAll("/+$", "");
  }

  @EventListener
  public void on(TenantRegistered event) {
    log.info(
        "Token de verificación de la empresa {} para {}: {}",
        event.tenantId(),
        event.administratorEmail(),
        mask(event.verificationToken()));
    log.debug(
        "Enlace de verificación de la empresa {} para {}: {}",
        event.tenantId(),
        event.administratorEmail(),
        webLink(
            "/verify-email?tenant=" + event.tenantId() + "&token=" + event.verificationToken()));
  }

  @EventListener
  public void on(UserInvited event) {
    log.info("Token de invitación para {}: {}", event.email(), mask(event.invitationToken()));
    log.debug(
        "Enlace de invitación para {}: {}",
        event.email(),
        webLink("/accept-invitation?token=" + event.invitationToken()));
  }

  @EventListener
  public void on(PasswordResetRequested event) {
    log.info("Token de recuperación para {}: {}", event.email(), mask(event.resetToken()));
    log.debug(
        "Enlace de recuperación para {}: {}",
        event.email(),
        webLink("/reset-password?token=" + event.resetToken()));
  }

  /** Arma el enlace que usará la web, por ejemplo http://localhost:4200/reset-password?token=... */
  private String webLink(String pathAndQuery) {
    return webBaseUrl + pathAndQuery;
  }

  /** Deja ver solo los primeros cuatro caracteres; un token corto se oculta por completo. */
  private static String mask(String token) {
    if (token == null) {
      return "null";
    }
    if (token.length() <= VISIBLE_CHARACTERS) {
      return MASK;
    }
    return token.substring(0, VISIBLE_CHARACTERS) + MASK;
  }
}
