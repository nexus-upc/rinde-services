package com.nexus.rinde.iam.demomail.infrastructure;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Protege los tokens de demostración con una clave distinta de las firmas JWT. */
@Component
@ConditionalOnProperty(name = "rinde.demo.mailbox.enabled", havingValue = "true")
@ConditionalOnProperty(name = "rinde.mail.provider", havingValue = "demo", matchIfMissing = true)
public class DemoMailCipher {
  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public DemoMailCipher(
      @Value("${rinde.demo.mailbox.encryption-key:}") String secret, Environment environment) {
    if (environment.acceptsProfiles(Profiles.of("prod", "production"))) {
      throw new IllegalStateException(
          "El buzón de demostración no puede habilitarse con perfiles de producción.");
    }
    if (!secret.matches("[0-9a-fA-F]{64}"))
      throw new IllegalStateException(
          "Configure una clave hexadecimal de 32 bytes para el buzón de demostración.");
    if (secret.equals(environment.getProperty("rinde.security.jwt.secret"))
        || secret.equals(environment.getProperty("rinde.platform.jwt.secret"))) {
      throw new IllegalStateException("La clave del buzón debe ser distinta de las claves JWT.");
    }
    key = new SecretKeySpec(HexFormat.of().parseHex(secret), "AES");
  }

  public String encrypt(String token, String context) {
    try {
      byte[] iv = new byte[12];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
      cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
      byte[] content = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder()
          .encodeToString(
              ByteBuffer.allocate(iv.length + content.length).put(iv).put(content).array());
    } catch (Exception ex) {
      throw new IllegalStateException("No se pudo proteger el mensaje de demostración.");
    }
  }

  public String decrypt(String encrypted, String context) {
    try {
      byte[] bytes = Base64.getDecoder().decode(encrypted);
      if (bytes.length < 29) throw new IllegalArgumentException();
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      byte[] iv = new byte[12];
      buffer.get(iv);
      byte[] content = new byte[buffer.remaining()];
      buffer.get(content);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
      cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
      return new String(cipher.doFinal(content), StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new IllegalStateException("No se pudo abrir el mensaje de demostración.");
    }
  }
}
