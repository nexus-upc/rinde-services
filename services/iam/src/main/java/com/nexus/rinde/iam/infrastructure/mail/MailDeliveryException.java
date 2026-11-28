package com.nexus.rinde.iam.infrastructure.mail;

/** Error al entregar un correo de acceso; el mensaje nunca incluye el token ni el enlace. */
public class MailDeliveryException extends RuntimeException {

  public MailDeliveryException(String message) {
    super(message);
  }
}
