package com.nexus.rinde.incident.domain.model.valueobjects;

import com.nexus.rinde.incident.domain.exceptions.IncidentEvidenceTooLargeException;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Archivo de evidencia de incidencia validado con su formato, tamaño y huella SHA-256. */
public record IncidentEvidenceFile(
    String fileName, IncidentEvidenceFormat format, long sizeBytes, String sha256) {

  private static final String DEFAULT_NAME = "evidencia";
  private static final int MAX_NAME_LENGTH = 120;

  /** Valida que el archivo no esté vacío, no exceda el límite y corresponda a un formato válido. */
  public static IncidentEvidenceFile inspect(
      String originalName, byte[] content, long maxSizeBytes) {
    if (content == null || content.length == 0) {
      throw new BusinessRuleException("El archivo del comprobante está vacío.");
    }
    if (content.length > maxSizeBytes) {
      throw new IncidentEvidenceTooLargeException(maxSizeBytes);
    }
    IncidentEvidenceFormat format =
        IncidentEvidenceFormat.detect(content)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "Formato no permitido. Adjunte una foto en JPEG, PNG o WEBP, o un PDF."));
    return new IncidentEvidenceFile(
        sanitizeName(originalName), format, content.length, sha256Hex(content));
  }

  /** Calcula la huella SHA-256 en hexadecimal minúscula. */
  public static String sha256Hex(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 no está disponible en la JVM.", ex);
    }
  }

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
