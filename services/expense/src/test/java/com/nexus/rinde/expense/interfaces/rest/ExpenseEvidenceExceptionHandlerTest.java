package com.nexus.rinde.expense.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.expense.domain.exceptions.EvidenceTooLargeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class ExpenseEvidenceExceptionHandlerTest {

  private final ExpenseEvidenceExceptionHandler handler = new ExpenseEvidenceExceptionHandler();

  @Test
  @DisplayName("Un comprobante que supera el límite de dominio responde 413")
  void shouldAnswerPayloadTooLargeForDomainLimit() {
    ResponseEntity<ProblemDetail> response =
        handler.handleEvidenceTooLarge(new EvidenceTooLargeException(5L * 1024 * 1024));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    assertThat(response.getBody().getDetail())
        .isEqualTo("El comprobante supera el tamaño máximo permitido de 5 MB.");
  }

  @Test
  @DisplayName("El límite del contenedor de multipart también responde 413")
  void shouldAnswerPayloadTooLargeForContainerLimit() {
    ResponseEntity<ProblemDetail> response =
        handler.handleUploadTooLarge(new MaxUploadSizeExceededException(10L * 1024 * 1024));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    assertThat(response.getBody().getDetail())
        .isEqualTo("El comprobante supera el tamaño máximo permitido.");
  }
}
