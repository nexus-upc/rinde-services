package com.nexus.rinde.iam.infrastructure.mail;

/** Envío técnico de un correo: el adaptador de Azure lo implementa con el SDK; las pruebas usan un doble. */
public interface EmailGateway {

  void send(String to, String subject, String plainText, String html);
}
