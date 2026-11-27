package com.nexus.rinde.incident.interfaces.rest;

import com.nexus.rinde.incident.domain.exceptions.IncidentEvidenceTooLargeException;
import com.nexus.rinde.shared.interfaces.rest.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Traduce excesos de tamaño de evidencias a HTTP 413 en formato RFC 7807. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IncidentEvidenceExceptionHandler {

  private static final String CONTAINER_LIMIT_MESSAGE =
      "El comprobante supera el tamaño máximo permitido.";

  @ExceptionHandler(IncidentEvidenceTooLargeException.class)
  public ResponseEntity<ProblemDetail> handleEvidenceTooLarge(
      IncidentEvidenceTooLargeException ex) {
    return tooLarge(ex.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ProblemDetail> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
    return tooLarge(CONTAINER_LIMIT_MESSAGE);
  }

  private static ResponseEntity<ProblemDetail> tooLarge(String detail) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(ProblemDetails.of(HttpStatus.PAYLOAD_TOO_LARGE, detail));
  }
}
