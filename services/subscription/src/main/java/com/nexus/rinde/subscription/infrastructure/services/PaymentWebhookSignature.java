package com.nexus.rinde.subscription.infrastructure.services;

import com.nexus.rinde.shared.domain.exceptions.AuthenticationFailedException;
import com.nexus.rinde.subscription.interfaces.rest.resources.PaymentWebhookResource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Firma y verifica el contenido canónico del webhook simulado con HMAC-SHA256. */
@Component
public class PaymentWebhookSignature {

  private static final String ALGORITHM = "HmacSHA256";

  private final byte[] secret;

  public PaymentWebhookSignature(
      @Value("${rinde.billing.simulation.webhook-secret}") String secret) {
    if (secret == null || secret.isBlank() || secret.length() < 32) {
      throw new IllegalArgumentException(
          "La clave HMAC del webhook debe tener al menos 32 caracteres.");
    }
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
  }

  public void verify(PaymentWebhookResource resource, String signature) {
    byte[] received;
    try {
      received = HexFormat.of().parseHex(signature == null ? "" : signature.trim());
    } catch (IllegalArgumentException ex) {
      throw new AuthenticationFailedException("La firma del webhook de pago no es válida.");
    }
    if (!MessageDigest.isEqual(sign(resource), received)) {
      throw new AuthenticationFailedException("La firma del webhook de pago no es válida.");
    }
  }

  /** Utilizado también por los fixtures de aceptación para emitir el evento de prueba firmado. */
  public String signHex(PaymentWebhookResource resource) {
    return HexFormat.of().formatHex(sign(resource));
  }

  private byte[] sign(PaymentWebhookResource resource) {
    String canonical =
        String.join(
            "\n",
            resource.providerEventId(),
            resource.providerReference(),
            resource.status().name(),
            resource.amount(),
            resource.failureReason() == null ? "" : resource.failureReason());
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret, ALGORITHM));
      return mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
    } catch (Exception ex) {
      throw new IllegalStateException("No se pudo firmar el webhook de pago.", ex);
    }
  }
}
