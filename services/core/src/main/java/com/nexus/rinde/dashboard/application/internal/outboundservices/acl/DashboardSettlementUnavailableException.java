package com.nexus.rinde.dashboard.application.internal.outboundservices.acl;

/** El servicio Settlement no respondió o devolvió un error inesperado. */
public class DashboardSettlementUnavailableException extends RuntimeException {

  public DashboardSettlementUnavailableException(Throwable cause) {
    super("El servicio de liquidaciones no está disponible.", cause);
  }
}
