package com.nexus.rinde.notification.infrastructure.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.nexus.rinde.notification.domain.model.valueobjects.TripAssignmentNotice;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.DriverDeviceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class FcmPushAdapterTest {

  private final FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
  private final DriverDeviceRepository driverDeviceRepository = mock(DriverDeviceRepository.class);
  private final FcmPushAdapter adapter =
      new FcmPushAdapter(firebaseMessaging, driverDeviceRepository);

  @Test
  @SuppressWarnings("unchecked")
  void buildsAndSendsNotificationWithTripTitleBodyAndData() throws Exception {
    TripAssignmentNotice notice = sampleNotice(List.of("fcm-token-01"));
    when(firebaseMessaging.send(any(Message.class))).thenReturn("projects/rinde/messages/1");

    adapter.send(notice);

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(firebaseMessaging).send(captor.capture());
    Message sent = captor.getValue();

    assertThat(ReflectionTestUtils.getField(sent, "token")).isEqualTo("fcm-token-01");
    Notification notification =
        (Notification) ReflectionTestUtils.getField(sent, "notification");
    assertThat(ReflectionTestUtils.getField(notification, "title"))
        .isEqualTo("Nuevo viaje asignado: TRP-000014");
    assertThat(ReflectionTestUtils.getField(notification, "body")).isEqualTo(notice.message());

    Map<String, String> data = (Map<String, String>) ReflectionTestUtils.getField(sent, "data");
    assertThat(data)
        .containsEntry("type", "TRIP_ASSIGNED")
        .containsEntry("eventId", notice.eventId().toString())
        .containsEntry("tripId", notice.tripId().toString())
        .containsEntry("tripCode", "TRP-000014")
        .containsEntry("destination", "Trujillo")
        .containsEntry("departureDate", "2026-11-26");
    verify(driverDeviceRepository, never()).deleteByToken(any());
  }

  @Test
  void deletesObsoleteTokenWhenFcmRespondsUnregistered() throws Exception {
    TripAssignmentNotice notice = sampleNotice(List.of("stale-token", "active-token"));
    FirebaseMessagingException unregistered = mock(FirebaseMessagingException.class);
    when(unregistered.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);

    when(firebaseMessaging.send(any(Message.class)))
        .thenThrow(unregistered)
        .thenReturn("projects/rinde/messages/2");

    assertThatCode(() -> adapter.send(notice)).doesNotThrowAnyException();

    verify(firebaseMessaging, times(2)).send(any(Message.class));
    verify(driverDeviceRepository).deleteByToken("stale-token");
    verify(driverDeviceRepository, never()).deleteByToken("active-token");
  }

  @Test
  void deletesSingleUnregisteredTokenWithoutThrowingTransientFailure() throws Exception {
    TripAssignmentNotice notice = sampleNotice(List.of("stale-only-token"));
    FirebaseMessagingException unregistered = mock(FirebaseMessagingException.class);
    when(unregistered.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
    when(firebaseMessaging.send(any(Message.class))).thenThrow(unregistered);

    assertThatCode(() -> adapter.send(notice)).doesNotThrowAnyException();

    verify(driverDeviceRepository).deleteByToken("stale-only-token");
  }

  @Test
  void throwsPushAdapterUnavailableExceptionOnTransientFcmFailure() throws Exception {
    TripAssignmentNotice notice = sampleNotice(List.of("fcm-token-01"));
    FirebaseMessagingException unavailable = mock(FirebaseMessagingException.class);
    when(unavailable.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
    when(firebaseMessaging.send(any(Message.class))).thenThrow(unavailable);

    assertThatThrownBy(() -> adapter.send(notice))
        .isInstanceOf(PushAdapterUnavailableException.class)
        .hasMessage("No se pudo entregar el aviso push por Firebase Cloud Messaging.");

    verify(driverDeviceRepository, never()).deleteByToken(any());
  }

  @Test
  void doesNothingWhenNoticeHasNoDeviceTokens() throws Exception {
    TripAssignmentNotice notice = sampleNotice(List.of());

    adapter.send(notice);

    verify(firebaseMessaging, never()).send(any(Message.class));
  }

  private static TripAssignmentNotice sampleNotice(List<String> tokens) {
    return TripAssignmentNotice.from(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "TRP-000014",
            "Trujillo",
            LocalDate.of(2026, 11, 26),
            Instant.parse("2026-11-25T10:00:00Z"))
        .withDeviceTokens(tokens);
  }
}
