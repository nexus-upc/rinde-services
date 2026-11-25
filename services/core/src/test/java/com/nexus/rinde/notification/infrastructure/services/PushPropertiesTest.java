package com.nexus.rinde.notification.infrastructure.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.google.firebase.messaging.FirebaseMessaging;
import com.nexus.rinde.notification.domain.services.PushAdapter;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.DriverDeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class PushPropertiesTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(TestPushConfig.class));

  @Test
  void acceptsUnavailableProviderByDefault() {
    PushProperties properties = new PushProperties();
    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  void rejectsFcmWhenCredentialsJsonIsEmpty() {
    PushProperties properties = new PushProperties();
    properties.setProvider("fcm");

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Debe configurar rinde.push.fcm.credentials-json cuando rinde.push.provider es fcm.");
  }

  @Test
  void acceptsFcmWhenCredentialsJsonIsConfigured() {
    PushProperties properties = new PushProperties();
    properties.setProvider("fcm");
    properties.getFcm().setCredentialsJson("{\"type\":\"service_account\"}");

    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  void rejectsUnsupportedProvider() {
    PushProperties properties = new PushProperties();
    properties.setProvider("apns");

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Proveedor push no válido: apns");
  }

  @Test
  void registersUnavailablePushAdapterByDefaultWithoutFirebaseCredentials() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(PushAdapter.class);
          assertThat(context.getBean(PushAdapter.class)).isInstanceOf(UnavailablePushAdapter.class);
        });
  }

  @Test
  void registersFcmPushAdapterWhenProviderIsFcm() {
    contextRunner
        .withPropertyValues(
            "rinde.push.provider=fcm",
            "rinde.push.fcm.credentials-json={\"type\":\"service_account\"}")
        .withBean(FirebaseMessaging.class, () -> mock(FirebaseMessaging.class))
        .run(
            context -> {
              assertThat(context).hasSingleBean(PushAdapter.class);
              assertThat(context.getBean(PushAdapter.class)).isInstanceOf(FcmPushAdapter.class);
            });
  }

  @Configuration
  @EnableConfigurationProperties({PushProperties.class, NotificationRetryProperties.class})
  static class TestPushConfig extends NotificationConfiguration {

    @Bean
    DriverDeviceRepository driverDeviceRepository() {
      return mock(DriverDeviceRepository.class);
    }
  }
}
