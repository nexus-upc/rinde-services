package com.nexus.rinde.dashboard.infrastructure.remote;

import com.nexus.rinde.dashboard.application.internal.outboundservices.acl.DashboardSettlementUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Devuelve 503 cuando el servicio Settlement no responde durante una consulta del dashboard. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DashboardSettlementAvailabilityAdvice {

  @ExceptionHandler(DashboardSettlementUnavailableException.class)
  public ProblemDetail handle(DashboardSettlementUnavailableException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
  }
}
