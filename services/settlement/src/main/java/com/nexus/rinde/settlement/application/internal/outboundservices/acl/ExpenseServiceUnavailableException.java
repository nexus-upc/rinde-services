package com.nexus.rinde.settlement.application.internal.outboundservices.acl;

/** Fallo de comunicación con Expense al leer los gastos aprobados. */
public class ExpenseServiceUnavailableException extends RuntimeException {

  public ExpenseServiceUnavailableException(String message) {
    super(message);
  }
}
