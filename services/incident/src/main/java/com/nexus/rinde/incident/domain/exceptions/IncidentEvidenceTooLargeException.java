package com.nexus.rinde.incident.domain.exceptions;

/** La evidencia supera el tamaño máximo configurado y se rechaza con HTTP 413. */
public class IncidentEvidenceTooLargeException extends RuntimeException {

  private static final long MEGABYTE = 1024L * 1024L;

  public IncidentEvidenceTooLargeException(long maxSizeBytes) {
    super(
        "El comprobante supera el tamaño máximo permitido de "
            + (maxSizeBytes % MEGABYTE == 0
                ? (maxSizeBytes / MEGABYTE) + " MB."
                : maxSizeBytes + " bytes."));
  }
}
