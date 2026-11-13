package com.nexus.rinde.settlement.application.internal.outboundservices.acl;

/** El servicio Expense no respondió o devolvió un error inesperado. */
public class ExpenseServiceUnavailableException extends RuntimeException {

  public ExpenseServiceUnavailableException(Throwable cause) {
    super("El servicio de gastos no está disponible.", cause);
  }
}
