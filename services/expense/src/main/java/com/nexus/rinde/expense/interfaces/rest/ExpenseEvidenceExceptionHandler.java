package com.nexus.rinde.expense.interfaces.rest;

import com.nexus.rinde.expense.domain.exceptions.EvidenceTooLargeException;
import com.nexus.rinde.shared.interfaces.rest.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Responde 413 cuando el comprobante supera el tamaño permitido, tanto en la validación del
 * dominio como en el límite del contenedor de multipart. Va antes que el manejador genérico.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExpenseEvidenceExceptionHandler {

  private static final String TOO_LARGE_MESSAGE =
      "El comprobante supera el tamaño máximo permitido.";

  @ExceptionHandler(EvidenceTooLargeException.class)
  public ResponseEntity<ProblemDetail> handleEvidenceTooLarge(EvidenceTooLargeException ex) {
    return tooLarge(ex.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ProblemDetail> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
    return tooLarge(TOO_LARGE_MESSAGE);
  }

  private static ResponseEntity<ProblemDetail> tooLarge(String detail) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(ProblemDetails.of(HttpStatus.PAYLOAD_TOO_LARGE, detail));
  }
}
