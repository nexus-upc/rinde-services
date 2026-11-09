package com.nexus.rinde.expense.domain.model.valueobjects;

import com.nexus.rinde.expense.domain.exceptions.EvidenceTooLargeException;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Comprobante validado con su formato, tamaño y huella SHA-256. */
public record EvidenceFile(String fileName, EvidenceFormat format, long sizeBytes, String sha256) {

  private static final String DEFAULT_NAME = "comprobante";
  private static final int MAX_NAME_LENGTH = 120;

  /** Valida que el contenido no esté vacío, no supere el tamaño y tenga un formato permitido. */
  public static EvidenceFile inspect(String originalName, byte[] content, long maxSizeBytes) {
    if (content == null || content.length == 0) {
      throw new BusinessRuleException("El archivo del comprobante está vacío.");
    }
    if (content.length > maxSizeBytes) {
      throw new EvidenceTooLargeException(maxSizeBytes);
    }
    EvidenceFormat format =
        EvidenceFormat.detect(content)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "Formato no permitido. Adjunte una foto en JPEG, PNG o WEBP, o un PDF."));
    return new EvidenceFile(
        sanitizeName(originalName), format, content.length, sha256Hex(content));
  }

  /** Huella SHA-256 en hexadecimal minúscula. */
  public static String sha256Hex(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 no está disponible en la JVM.", e);
    }
  }

  /** Conserva solo el nombre base, sin rutas ni caracteres de control. */
  static String sanitizeName(String originalName) {
    if (originalName == null) {
      return DEFAULT_NAME;
    }
    String name = originalName.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
    if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
      return DEFAULT_NAME;
    }
    return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
  }
}
