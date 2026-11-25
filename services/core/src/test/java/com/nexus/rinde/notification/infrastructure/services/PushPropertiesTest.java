package com.nexus.rinde.notification.infrastructure.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PushPropertiesTest {

  @Test
  void acceptsUnavailableProviderByDefault() {
    PushProperties properties = new PushProperties();
    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  void rejectsFcmUntilImplementedInSprint4() {
    PushProperties properties = new PushProperties();
    properties.setProvider("fcm");

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("El proveedor push fcm está pendiente de implementar en S4-24.");
  }
}
