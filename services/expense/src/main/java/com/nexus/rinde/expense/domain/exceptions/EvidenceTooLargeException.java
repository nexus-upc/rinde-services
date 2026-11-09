package com.nexus.rinde.expense.domain.exceptions;

/** El comprobante supera el tamaño máximo configurado. Se responde con 413. */
public class EvidenceTooLargeException extends RuntimeException {

  private static final long MEGABYTE = 1024L * 1024L;

  public EvidenceTooLargeException(long maxSizeBytes) {
    super(
        "El comprobante supera el tamaño máximo permitido de "
            + (maxSizeBytes % MEGABYTE == 0
                ? (maxSizeBytes / MEGABYTE) + " MB."
                : maxSizeBytes + " bytes."));
  }
}
