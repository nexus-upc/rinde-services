package com.nexus.rinde.notification.infrastructure.services;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.nexus.rinde.notification.domain.model.valueobjects.TripAssignmentNotice;
import com.nexus.rinde.notification.domain.services.PushAdapter;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.DriverDeviceRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Adaptador de envío push hacia Firebase Cloud Messaging con limpieza de tokens obsoletos. */
public class FcmPushAdapter implements PushAdapter {

  private static final Logger log = LoggerFactory.getLogger(FcmPushAdapter.class);

  private final FirebaseMessaging firebaseMessaging;
  private final DriverDeviceRepository driverDeviceRepository;

  public FcmPushAdapter(
      FirebaseMessaging firebaseMessaging, DriverDeviceRepository driverDeviceRepository) {
    this.firebaseMessaging = firebaseMessaging;
    this.driverDeviceRepository = driverDeviceRepository;
  }

  @Override
  public void send(TripAssignmentNotice notice) {
    List<String> tokens = notice.deviceTokens();
    if (tokens == null || tokens.isEmpty()) {
      return;
    }

    int deliveredCount = 0;
    Throwable transientFailure = null;

    for (String token : tokens) {
      try {
        firebaseMessaging.send(buildMessage(notice, token));
        deliveredCount++;
      } catch (FirebaseMessagingException ex) {
        if (MessagingErrorCode.UNREGISTERED == ex.getMessagingErrorCode()) {
          driverDeviceRepository.deleteByToken(token);
          log.warn(
              "Se depuró un token FCM no registrado para el conductor {} del evento {}.",
              notice.driverId(),
              notice.eventId());
        } else {
          transientFailure = ex;
          log.warn(
              "Fallo FCM {} al enviar el evento {} al conductor {}.",
              ex.getMessagingErrorCode(),
              notice.eventId(),
              notice.driverId());
        }
      } catch (RuntimeException ex) {
        transientFailure = ex;
        log.warn(
            "Error inesperado al enviar aviso FCM del evento {} al conductor {}.",
            notice.eventId(),
            notice.driverId(),
            ex);
      }
    }

    if (deliveredCount == 0 && transientFailure != null) {
      throw new PushAdapterUnavailableException(
          "No se pudo entregar el aviso push por Firebase Cloud Messaging.", transientFailure);
    }
  }

  Message buildMessage(TripAssignmentNotice notice, String token) {
    Notification notification =
        Notification.builder()
            .setTitle("Nuevo viaje asignado: " + notice.tripCode())
            .setBody(notice.message())
            .build();
    AndroidConfig androidConfig =
        AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build();
    return Message.builder()
        .setToken(token)
        .setNotification(notification)
        .setAndroidConfig(androidConfig)
        .putData("type", "TRIP_ASSIGNED")
        .putData("eventId", notice.eventId().toString())
        .putData("tripId", notice.tripId().toString())
        .putData("tripCode", notice.tripCode())
        .putData("destination", notice.destination())
        .putData("departureDate", notice.departureDate().toString())
        .build();
  }
}
