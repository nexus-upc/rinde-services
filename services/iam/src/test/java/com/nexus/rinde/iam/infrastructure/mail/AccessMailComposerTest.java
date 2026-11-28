package com.nexus.rinde.iam.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessMailComposerTest {

  private static final String BASE = "http://localhost:4200";
  private static final UUID TENANT = UUID.fromString("7d1c0f3e-5a2b-4c8d-9e6f-1a2b3c4d5e6f");

  private final AccessMailComposer composer = new AccessMailComposer(BASE);

  @Test
  void verificationUsesTheExactSubjectLinkAndValidity() {
    AccessMailComposer.Content content =
        composer.verification(TENANT, "Andes SAC", "token-verificacion");

    assertThat(content.subject()).isEqualTo("Verifica tu empresa en Rinde");
    assertThat(content.plainText())
        .contains(BASE + "/verify-email?tenant=" + TENANT + "&token=token-verificacion")
        .contains("48 horas")
        .contains("Andes SAC");
    assertThat(content.html())
        .contains("href=\"" + BASE + "/verify-email?tenant=" + TENANT + "&amp;token=token-verificacion\"");
  }

  @Test
  void invitationUsesTheCompanySubjectLinkAndValidity() {
    AccessMailComposer.Content content = composer.invitation("Andes SAC", "token-invitacion");

    assertThat(content.subject()).isEqualTo("Tu invitación a Andes SAC");
    assertThat(content.plainText())
        .contains(BASE + "/accept-invitation?token=token-invitacion")
        .contains("48 horas");
    assertThat(content.html()).contains("href=\"" + BASE + "/accept-invitation?token=token-invitacion\"");
  }

  @Test
  void passwordResetUsesTheExactSubjectLinkAndOneHourValidity() {
    AccessMailComposer.Content content = composer.passwordReset("token-recuperacion");

    assertThat(content.subject()).isEqualTo("Recupera tu acceso a Rinde");
    assertThat(content.plainText())
        .contains(BASE + "/reset-password?token=token-recuperacion")
        .contains("1 hora")
        .doesNotContain("48 horas");
    assertThat(content.html()).contains("href=\"" + BASE + "/reset-password?token=token-recuperacion\"");
  }

  @Test
  void aTrailingSlashInTheBaseUrlIsNotDuplicated() {
    AccessMailComposer withSlashes = new AccessMailComposer(BASE + "//");

    AccessMailComposer.Content content = withSlashes.passwordReset("token-recuperacion");

    assertThat(content.plainText())
        .contains(BASE + "/reset-password?token=token-recuperacion")
        .doesNotContain("//reset-password");
  }

  @Test
  void tokenIsEncodedForTheUrl() {
    AccessMailComposer.Content content = composer.passwordReset("a b/c+d=");

    assertThat(content.plainText()).contains("?token=a+b%2Fc%2Bd%3D");
  }

  @Test
  void companyNameIsEscapedOnlyInHtml() {
    String company = "<b>Andes</b> & SAC";

    AccessMailComposer.Content content = composer.invitation(company, "token-invitacion");

    assertThat(content.plainText()).contains("unirte a " + company + " en Rinde");
    assertThat(content.html())
        .contains("&lt;b&gt;Andes&lt;/b&gt; &amp; SAC")
        .doesNotContain("<b>Andes</b>");
  }
}
