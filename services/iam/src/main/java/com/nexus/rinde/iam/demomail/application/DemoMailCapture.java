package com.nexus.rinde.iam.demomail.application;

import com.nexus.rinde.iam.demomail.infrastructure.*;
import com.nexus.rinde.iam.domain.model.events.*;
import com.nexus.rinde.iam.domain.model.valueobjects.*;
import com.nexus.rinde.iam.infrastructure.persistence.jpa.repositories.TenantRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Guarda el correo en la misma transacción que crea el enlace de acceso. */
@Service
@ConditionalOnProperty(name = "rinde.demo.mailbox.enabled", havingValue = "true")
@ConditionalOnProperty(name = "rinde.mail.provider", havingValue = "demo", matchIfMissing = true)
public class DemoMailCapture {
  private final DemoMailRepository messages;
  private final DemoMailCipher cipher;
  private final TenantRepository tenants;

  public DemoMailCapture(
      DemoMailRepository messages, DemoMailCipher cipher, TenantRepository tenants) {
    this.messages = messages;
    this.cipher = cipher;
    this.tenants = tenants;
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void registered(TenantRegistered event) {
    append(
        event.eventId(),
        event.tenantId(),
        event.administratorEmail(),
        "Administrador",
        event.tradeName(),
        TokenPurpose.EMAIL_VERIFICATION,
        event.verificationToken(),
        event.occurredAt(),
        event.tokenExpiresAt());
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void invited(UserInvited event) {
    append(
        event.eventId(),
        event.tenantId(),
        event.email(),
        event.fullName(),
        company(event.tenantId()),
        TokenPurpose.INVITATION,
        event.invitationToken(),
        event.occurredAt(),
        event.tokenExpiresAt());
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void passwordReset(PasswordResetRequested event) {
    append(
        event.eventId(),
        event.tenantId(),
        event.email(),
        "Usuario",
        company(event.tenantId()),
        TokenPurpose.PASSWORD_RESET,
        event.resetToken(),
        event.occurredAt(),
        event.tokenExpiresAt());
  }

  private String company(UUID id) {
    return tenants.findById(id).orElseThrow().getTradeName();
  }

  private void append(
      UUID eventId,
      UUID tenantId,
      String email,
      String name,
      String company,
      TokenPurpose purpose,
      String token,
      Instant created,
      Instant expires) {
    String context = eventId + "|" + tenantId + "|" + purpose.name();
    messages.append(
        eventId,
        tenantId,
        email,
        name,
        company,
        purpose,
        AccessToken.hash(token),
        cipher.encrypt(token, context),
        created,
        expires);
  }
}
