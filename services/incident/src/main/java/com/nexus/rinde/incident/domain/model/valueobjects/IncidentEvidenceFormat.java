package com.nexus.rinde.incident.domain.model.valueobjects;

import java.util.Optional;

/** Formatos de evidencia permitidos en incidencias, detectados por su firma binaria inicial. */
public enum IncidentEvidenceFormat {
  JPEG("image/jpeg"),
  PNG("image/png"),
  WEBP("image/webp"),
  PDF("application/pdf");

  private final String contentType;

  IncidentEvidenceFormat(String contentType) {
    this.contentType = contentType;
  }

  public String contentType() {
    return contentType;
  }

  /** Detecta el formato real a partir de los primeros bytes del archivo. */
  public static Optional<IncidentEvidenceFormat> detect(byte[] content) {
    if (content == null) {
      return Optional.empty();
    }
    if (startsWith(content, 0, 0xFF, 0xD8, 0xFF)) {
      return Optional.of(JPEG);
    }
    if (startsWith(content, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
      return Optional.of(PNG);
    }
    if (startsWith(content, 0, 'R', 'I', 'F', 'F') && startsWith(content, 8, 'W', 'E', 'B', 'P')) {
      return Optional.of(WEBP);
    }
    if (startsWith(content, 0, '%', 'P', 'D', 'F', '-')) {
      return Optional.of(PDF);
    }
    return Optional.empty();
  }

  private static boolean startsWith(byte[] content, int offset, int... expected) {
    if (content.length < offset + expected.length) {
      return false;
    }
    for (int i = 0; i < expected.length; i++) {
      if ((content[offset + i] & 0xFF) != expected[i]) {
        return false;
      }
    }
    return true;
  }
}
