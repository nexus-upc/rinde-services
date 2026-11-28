package com.nexus.rinde.iam.infrastructure.mail;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** Arma asunto, texto y HTML de los correos de acceso; los enlaces salen de rinde.web.base-url. */
@Component
@ConditionalOnProperty(name = "rinde.mail.provider", havingValue = "azure")
public class AccessMailComposer {

  static final String VERIFICATION_SUBJECT = "Verifica tu empresa en Rinde";
  static final String INVITATION_SUBJECT_PREFIX = "Tu invitación a ";
  static final String RESET_SUBJECT = "Recupera tu acceso a Rinde";

  private final String webBaseUrl;

  public AccessMailComposer(@Value("${rinde.web.base-url}") String webBaseUrl) {
    this.webBaseUrl = webBaseUrl.replaceAll("/+$", "");
  }

  public Content verification(UUID tenantId, String tradeName, String verificationToken) {
    String link =
        webBaseUrl + "/verify-email?tenant=" + tenantId + "&token=" + encode(verificationToken);
    String intro =
        "Recibimos el registro de "
            + tradeName
            + " en Rinde. Para activar la cuenta, verifica tu empresa en este enlace:";
    return build(
        VERIFICATION_SUBJECT,
        intro,
        link,
        "48 horas",
        "Si no registraste esta empresa, ignora este correo.");
  }

  public Content invitation(String companyName, String invitationToken) {
    String link = webBaseUrl + "/accept-invitation?token=" + encode(invitationToken);
    String intro =
        "Te invitaron a unirte a "
            + companyName
            + " en Rinde. Para aceptar la invitación y crear tu acceso, abre este enlace:";
    return build(
        INVITATION_SUBJECT_PREFIX + companyName,
        intro,
        link,
        "48 horas",
        "Si no esperabas esta invitación, ignora este correo.");
  }

  public Content passwordReset(String resetToken) {
    String link = webBaseUrl + "/reset-password?token=" + encode(resetToken);
    String intro =
        "Recibimos una solicitud para recuperar tu acceso a Rinde. "
            + "Para elegir una nueva contraseña, abre este enlace:";
    return build(
        RESET_SUBJECT,
        intro,
        link,
        "1 hora",
        "Si no pediste este cambio, ignora este correo: tu contraseña no cambiará.");
  }

  private static Content build(
      String subject, String intro, String link, String validity, String closing) {
    String expiry = "El enlace vence en " + validity + ". " + closing;
    String plainText = "Hola,\n\n" + intro + "\n\n" + link + "\n\n" + expiry + "\n";
    String html =
        "<!DOCTYPE html><html lang=\"es\"><body>"
            + paragraph("Hola,")
            + paragraph(intro)
            + "<p><a href=\""
            + HtmlUtils.htmlEscape(link)
            + "\">"
            + HtmlUtils.htmlEscape(link)
            + "</a></p>"
            + paragraph(expiry)
            + "</body></html>";
    return new Content(subject, plainText, html);
  }

  private static String paragraph(String text) {
    return "<p>" + HtmlUtils.htmlEscape(text) + "</p>";
  }

  private static String encode(String token) {
    return URLEncoder.encode(token, StandardCharsets.UTF_8);
  }

  /** Contenido de un correo listo para enviar. */
  public record Content(String subject, String plainText, String html) {}
}
