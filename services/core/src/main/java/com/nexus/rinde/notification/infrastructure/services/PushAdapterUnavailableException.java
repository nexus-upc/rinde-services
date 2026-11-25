package com.nexus.rinde.notification.infrastructure.services;

/** Indica que el proveedor push no está disponible o falló de forma transitoria. */
public class PushAdapterUnavailableException extends RuntimeException {

  public PushAdapterUnavailableException(String message) {
    super(message);
  }

  public PushAdapterUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
