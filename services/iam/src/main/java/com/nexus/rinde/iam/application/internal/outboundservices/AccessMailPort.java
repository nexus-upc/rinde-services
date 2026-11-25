package com.nexus.rinde.iam.application.internal.outboundservices;

import java.util.UUID;

/** Puerto de salida para correos transaccionales de acceso en IAM. */
public interface AccessMailPort {

  void sendTenantVerification(
      UUID tenantId, String tradeName, String recipientEmail, String verificationToken);

  void sendUserInvitation(
      UUID tenantId, String companyName, String recipientEmail, String invitationToken);

  void sendPasswordReset(UUID tenantId, String recipientEmail, String resetToken);
}
