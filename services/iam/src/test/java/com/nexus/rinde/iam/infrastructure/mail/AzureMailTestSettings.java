package com.nexus.rinde.iam.infrastructure.mail;

import org.springframework.test.context.DynamicPropertyRegistry;

/** Configuración de Azure ficticia para las pruebas: el gateway siempre es un doble. */
final class AzureMailTestSettings {

  static final String CONNECTION_STRING =
      "endpoint=https://rinde-prueba.communication.azure.com/;accesskey=Y2xhdmUtZmljdGljaWE=";
  static final String SENDER = "no-responder@rinde-prueba.invalid";

  private AzureMailTestSettings() {}

  static void register(DynamicPropertyRegistry registry, boolean mailboxEnabled) {
    registry.add("rinde.mail.provider", () -> "azure");
    registry.add("rinde.mail.acs-connection-string", () -> CONNECTION_STRING);
    registry.add("rinde.mail.sender", () -> SENDER);
    registry.add("rinde.web.base-url", () -> "http://localhost:4200/");
    registry.add("rinde.demo.mailbox.enabled", () -> mailboxEnabled);
    registry.add("rinde.demo.mailbox.encryption-key", () -> "a".repeat(64));
  }
}
