package com.nexus.rinde.iam.infrastructure.mail;

import com.nexus.rinde.iam.application.internal.outboundservices.AccessMailPort;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adaptador de correos de acceso para el proveedor azure: compone el correo y lo envía. */
@Component
@ConditionalOnProperty(name = "rinde.mail.provider", havingValue = "azure")
public class AzureEmailAccessMailAdapter implements AccessMailPort {

  private final EmailGateway gateway;
  private final AccessMailComposer composer;

  public AzureEmailAccessMailAdapter(EmailGateway gateway, AccessMailComposer composer) {
    this.gateway = gateway;
    this.composer = composer;
  }

  @Override
  public void sendTenantVerification(
      UUID tenantId, String tradeName, String recipientEmail, String verificationToken) {
    send(recipientEmail, composer.verification(tenantId, tradeName, verificationToken));
  }

  @Override
  public void sendUserInvitation(
      UUID tenantId, String companyName, String recipientEmail, String invitationToken) {
    send(recipientEmail, composer.invitation(companyName, invitationToken));
  }

  @Override
  public void sendPasswordReset(UUID tenantId, String recipientEmail, String resetToken) {
    send(recipientEmail, composer.passwordReset(resetToken));
  }

  private void send(String recipientEmail, AccessMailComposer.Content content) {
    gateway.send(recipientEmail, content.subject(), content.plainText(), content.html());
  }
}
