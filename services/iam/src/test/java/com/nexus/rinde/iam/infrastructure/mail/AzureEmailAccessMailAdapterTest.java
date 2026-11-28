package com.nexus.rinde.iam.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AzureEmailAccessMailAdapterTest {

  private static final UUID TENANT = UUID.fromString("7d1c0f3e-5a2b-4c8d-9e6f-1a2b3c4d5e6f");
  private static final String BASE = "http://localhost:4200";

  /** Doble que guarda los envíos en memoria; no llama a ningún servicio real. */
  private static final class RecordingGateway implements EmailGateway {
    final List<String[]> sent = new ArrayList<>();

    @Override
    public void send(String to, String subject, String plainText, String html) {
      sent.add(new String[] {to, subject, plainText, html});
    }
  }

  private final RecordingGateway gateway = new RecordingGateway();
  private final AzureEmailAccessMailAdapter adapter =
      new AzureEmailAccessMailAdapter(gateway, new AccessMailComposer(BASE));

  @Test
  void verificationGoesToTheAdministratorWithTheLink() {
    adapter.sendTenantVerification(TENANT, "Andes SAC", "ana@prueba.invalid", "token-verificacion");

    String[] mail = onlyMail();
    assertThat(mail[0]).isEqualTo("ana@prueba.invalid");
    assertThat(mail[1]).isEqualTo("Verifica tu empresa en Rinde");
    assertThat(mail[2]).contains(BASE + "/verify-email?tenant=" + TENANT + "&token=token-verificacion");
    assertThat(mail[3]).contains("verify-email");
  }

  @Test
  void invitationGoesToTheInvitedUserWithTheCompanyName() {
    adapter.sendUserInvitation(TENANT, "Andes SAC", "luis@prueba.invalid", "token-invitacion");

    String[] mail = onlyMail();
    assertThat(mail[0]).isEqualTo("luis@prueba.invalid");
    assertThat(mail[1]).isEqualTo("Tu invitación a Andes SAC");
    assertThat(mail[2]).contains(BASE + "/accept-invitation?token=token-invitacion");
  }

  @Test
  void passwordResetGoesToTheUserWithTheResetLink() {
    adapter.sendPasswordReset(TENANT, "ana@prueba.invalid", "token-recuperacion");

    String[] mail = onlyMail();
    assertThat(mail[0]).isEqualTo("ana@prueba.invalid");
    assertThat(mail[1]).isEqualTo("Recupera tu acceso a Rinde");
    assertThat(mail[2]).contains(BASE + "/reset-password?token=token-recuperacion");
  }

  private String[] onlyMail() {
    assertThat(gateway.sent).hasSize(1);
    return gateway.sent.get(0);
  }
}
