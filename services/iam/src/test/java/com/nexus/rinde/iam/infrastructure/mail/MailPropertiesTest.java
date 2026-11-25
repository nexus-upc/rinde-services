package com.nexus.rinde.iam.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class MailPropertiesTest {

  @Test
  void acceptsDemoProviderInNonProduction() {
    MailProperties properties = new MailProperties();
    properties.setEnvironment(new MockEnvironment());
    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  void rejectsDemoProviderInProduction() {
    MailProperties properties = new MailProperties();
    MockEnvironment prodEnv = new MockEnvironment();
    prodEnv.setActiveProfiles("production");
    properties.setEnvironment(prodEnv);

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("En producción el correo debe usar el proveedor azure.");
  }

  @Test
  void rejectsAzureProviderUntilImplementedInSprint4() {
    MailProperties properties = new MailProperties();
    properties.setProvider("azure");
    properties.setEnvironment(new MockEnvironment());

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("El proveedor de correo azure está pendiente de implementar en S4-27.");
  }
}
