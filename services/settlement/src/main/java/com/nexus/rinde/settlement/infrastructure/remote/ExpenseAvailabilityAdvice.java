package com.nexus.rinde.settlement.infrastructure.remote;

import com.nexus.rinde.settlement.application.internal.outboundservices.acl.ExpenseServiceUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Devuelve 503 cuando el servicio Expense no responde durante una operación de liquidación. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExpenseAvailabilityAdvice {

  @ExceptionHandler(ExpenseServiceUnavailableException.class)
  public ProblemDetail handle(ExpenseServiceUnavailableException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
  }
}
