package com.nexus.rinde.iam.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class MailPropertiesTest {

  private static final String CONNECTION_STRING =
      "endpoint=https://rinde-prueba.communication.azure.com/;accesskey=Y2xhdmUtZmljdGljaWE=";
  private static final String SENDER = "no-responder@rinde-prueba.invalid";

  private static MailProperties azure(MockEnvironment environment) {
    MailProperties properties = new MailProperties();
    properties.setProvider("azure");
    properties.setAcsConnectionString(CONNECTION_STRING);
    properties.setSender(SENDER);
    properties.setEnvironment(environment);
    return properties;
  }

  private static MockEnvironment production() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("production");
    return environment;
  }

  @Test
  void defaultsToTheDemoProvider() {
    MailProperties properties = new MailProperties();
    properties.setEnvironment(new MockEnvironment());

    assertThat(properties.getProvider()).isEqualTo("demo");
    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  void acceptsDemoProviderInNonProduction() {
    MailProperties properties = new MailProperties();
    properties.setEnvironment(new MockEnvironment());
    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  void rejectsDemoProviderInProduction() {
    MailProperties properties = new MailProperties();
    properties.setEnvironment(production());

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("En producción el correo debe usar el proveedor azure.");
  }

  @Test
  void rejectsAnUnknownProvider() {
    MailProperties properties = new MailProperties();
    properties.setProvider("smtp");
    properties.setEnvironment(new MockEnvironment());

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Proveedor de correo no válido: smtp");
  }

  @Test
  void acceptsAzureWithConnectionStringAndSender() {
    assertThatCode(azure(new MockEnvironment())::validate).doesNotThrowAnyException();
  }

  @Test
  void acceptsAzureInProductionWhenConfigured() {
    assertThatCode(azure(production())::validate).doesNotThrowAnyException();
  }

  @Test
  void rejectsAzureWithoutConnectionString() {
    MailProperties properties = azure(new MockEnvironment());
    properties.setAcsConnectionString("");

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Con el proveedor azure se requieren RINDE_ACS_CONNECTION_STRING y RINDE_MAIL_SENDER.");
  }

  @Test
  void rejectsAzureWithoutSender() {
    MailProperties properties = azure(new MockEnvironment());
    properties.setSender(" ");

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Con el proveedor azure se requieren RINDE_ACS_CONNECTION_STRING y RINDE_MAIL_SENDER.");
  }

  @Test
  void neverPrintsTheConnectionString() {
    String text = azure(new MockEnvironment()).toString();

    assertThat(text).contains("azure").doesNotContain("accesskey").doesNotContain("endpoint=");
  }
}
