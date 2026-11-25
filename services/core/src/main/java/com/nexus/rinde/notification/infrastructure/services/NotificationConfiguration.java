package com.nexus.rinde.notification.infrastructure.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.nexus.rinde.notification.domain.model.valueobjects.RetryPolicy;
import com.nexus.rinde.notification.domain.services.PushAdapter;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.DriverDeviceRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra el adaptador push configurado (unavailable por defecto o FCM) y la política de reintentos. */
@Configuration
public class NotificationConfiguration {

  private static final String FCM_APP_NAME = "rinde-fcm";

  @Bean
  @ConditionalOnProperty(
      name = "rinde.push.provider",
      havingValue = "unavailable",
      matchIfMissing = true)
  @ConditionalOnMissingBean(PushAdapter.class)
  public PushAdapter unavailablePushAdapter() {
    return new UnavailablePushAdapter();
  }

  @Bean
  @ConditionalOnProperty(name = "rinde.push.provider", havingValue = "fcm")
  @ConditionalOnMissingBean(FirebaseMessaging.class)
  public FirebaseMessaging firebaseMessaging(PushProperties properties) throws IOException {
    String credentialsJson = properties.getFcm().getCredentialsJson();
    if (credentialsJson == null || credentialsJson.isBlank()) {
      throw new IllegalStateException(
          "Debe configurar rinde.push.fcm.credentials-json cuando rinde.push.provider es fcm.");
    }
    GoogleCredentials credentials =
        GoogleCredentials.fromStream(
            new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)));
    FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
    FirebaseApp app =
        FirebaseApp.getApps().stream()
            .filter(existing -> FCM_APP_NAME.equals(existing.getName()))
            .findFirst()
            .orElseGet(() -> FirebaseApp.initializeApp(options, FCM_APP_NAME));
    return FirebaseMessaging.getInstance(app);
  }

  @Bean
  @ConditionalOnProperty(name = "rinde.push.provider", havingValue = "fcm")
  @ConditionalOnMissingBean(PushAdapter.class)
  public PushAdapter fcmPushAdapter(
      FirebaseMessaging firebaseMessaging, DriverDeviceRepository driverDeviceRepository) {
    return new FcmPushAdapter(firebaseMessaging, driverDeviceRepository);
  }

  /** Calendario de reintentos leído de rinde.notification.retry.delays-minutes. */
  @Bean
  public RetryPolicy retryPolicy(NotificationRetryProperties properties) {
    return new RetryPolicy(properties.delaysMinutes().stream().map(Duration::ofMinutes).toList());
  }
}
