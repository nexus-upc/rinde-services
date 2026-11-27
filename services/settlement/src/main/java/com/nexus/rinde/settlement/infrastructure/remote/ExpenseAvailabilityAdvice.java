package com.nexus.rinde.settlement.infrastructure.remote;

import com.nexus.rinde.settlement.application.internal.outboundservices.acl.ExpenseServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Mapea la indisponibilidad del servicio Expense a 503 Service Unavailable. */
@RestControllerAdvice
public class ExpenseAvailabilityAdvice {

  @ExceptionHandler(ExpenseServiceUnavailableException.class)
  public ProblemDetail handle(ExpenseServiceUnavailableException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
  }
}
