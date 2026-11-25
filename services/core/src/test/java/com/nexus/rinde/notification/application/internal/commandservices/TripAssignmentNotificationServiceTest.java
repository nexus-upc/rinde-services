package com.nexus.rinde.notification.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.rinde.fleet.interfaces.acl.FleetContextFacade;
import com.nexus.rinde.fleet.interfaces.acl.FleetDriverSummary;
import com.nexus.rinde.notification.application.events.TripAssignedNotification;
import com.nexus.rinde.notification.application.internal.outboundservices.acl.NotificationFleetService;
import com.nexus.rinde.notification.domain.model.valueobjects.RetryPolicy;
import com.nexus.rinde.notification.domain.model.valueobjects.TripAssignmentNotice;
import com.nexus.rinde.notification.domain.services.PushAdapter;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.DriverDeviceEntity;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.RetryStoreEntry;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.DriverDeviceRepository;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.RetryStoreRepository;
import com.nexus.rinde.notification.infrastructure.services.PushAdapterUnavailableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TripAssignmentNotificationServiceTest {

  private static final Instant T0 = Instant.parse("2026-11-25T10:00:00Z");
  private static final RetryPolicy POLICY =
      new RetryPolicy(
          List.of(
              Duration.ofMinutes(1),
              Duration.ofMinutes(2),
              Duration.ofMinutes(4),
              Duration.ofMinutes(8),
              Duration.ofMinutes(16)));

  private final RetryStoreRepository retryStoreRepository = mock(RetryStoreRepository.class);
  private final FleetContextFacade fleetContextFacade = mock(FleetContextFacade.class);
  private final DriverDeviceRepository driverDeviceRepository = mock(DriverDeviceRepository.class);
  private final PushAdapter pushAdapter = mock(PushAdapter.class);
  private final Clock clock = Clock.fixed(T0, ZoneOffset.UTC);

  private TripAssignmentNotificationService service;

  @BeforeEach
  void setUp() {
    when(retryStoreRepository.saveAndFlush(any(RetryStoreEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    TripAssignmentPushDispatcher dispatcher =
        new TripAssignmentPushDispatcher(
            new NotificationFleetService(fleetContextFacade),
            driverDeviceRepository,
            pushAdapter,
            POLICY);
    service = new TripAssignmentNotificationService(retryStoreRepository, dispatcher, clock);
  }

  @Test
  void resolvesDriverUserIdAndDeliversPushToRegisteredDevices() {
    UUID tenantId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    TripAssignedNotification event = event(tenantId, driverId);

    when(fleetContextFacade.findDriversByTenantId(tenantId))
        .thenReturn(
            List.of(
                new FleetDriverSummary(
                    driverId, userId, "Luis Quispe", "Q-123456", "A-IIb", "ENABLED")));
    when(driverDeviceRepository.findAllByTenantIdAndUserIdOrderByUpdatedAtDesc(tenantId, userId))
        .thenReturn(
            List.of(
                DriverDeviceEntity.register(
                    tenantId, userId, "fcm-token-luis-01", "ANDROID", T0)));

    service.handle(event);

    ArgumentCaptor<TripAssignmentNotice> noticeCaptor =
        ArgumentCaptor.forClass(TripAssignmentNotice.class);
    verify(pushAdapter).send(noticeCaptor.capture());
    assertThat(noticeCaptor.getValue().deviceTokens()).containsExactly("fcm-token-luis-01");

    ArgumentCaptor<RetryStoreEntry> entryCaptor = ArgumentCaptor.forClass(RetryStoreEntry.class);
    verify(retryStoreRepository, org.mockito.Mockito.times(2)).saveAndFlush(entryCaptor.capture());
    RetryStoreEntry saved = entryCaptor.getValue();
    assertThat(saved.getDeliveryStatus()).isEqualTo(RetryStoreEntry.DELIVERED);
    assertThat(saved.getDeliveredAt()).isEqualTo(T0);
    assertThat(saved.getNextAttemptAt()).isNull();
  }

  @Test
  void skipsDuplicateEventIdWithoutQueryingFleetOrSendingPush() {
    TripAssignedNotification event = event(UUID.randomUUID(), UUID.randomUUID());
    when(retryStoreRepository.existsById(event.eventId())).thenReturn(true);

    service.handle(event);

    verify(fleetContextFacade, never()).findDriversByTenantId(any());
    verify(driverDeviceRepository, never())
        .findAllByTenantIdAndUserIdOrderByUpdatedAtDesc(any(), any());
    verify(pushAdapter, never()).send(any());
    verify(retryStoreRepository, never()).saveAndFlush(any());
  }

  @Test
  void schedulesRetryWithoutThrowingWhenPushFails() {
    UUID tenantId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    TripAssignedNotification event = event(tenantId, driverId);

    when(fleetContextFacade.findDriversByTenantId(tenantId))
        .thenReturn(
            List.of(
                new FleetDriverSummary(
                    driverId, userId, "Luis Quispe", "Q-123456", "A-IIb", "ENABLED")));
    when(driverDeviceRepository.findAllByTenantIdAndUserIdOrderByUpdatedAtDesc(tenantId, userId))
        .thenReturn(
            List.of(
                DriverDeviceEntity.register(
                    tenantId, userId, "fcm-token-luis-01", "ANDROID", T0)));
    doThrow(new PushAdapterUnavailableException("Proveedor push temporalmente no disponible."))
        .when(pushAdapter)
        .send(any());

    service.handle(event);

    ArgumentCaptor<RetryStoreEntry> entryCaptor = ArgumentCaptor.forClass(RetryStoreEntry.class);
    verify(retryStoreRepository, org.mockito.Mockito.times(2)).saveAndFlush(entryCaptor.capture());
    RetryStoreEntry saved = entryCaptor.getValue();
    assertThat(saved.getDeliveryStatus()).isEqualTo(RetryStoreEntry.PENDING);
    assertThat(saved.getRetryCount()).isZero();
    assertThat(saved.getNextAttemptAt()).isEqualTo(T0.plus(Duration.ofMinutes(1)));
    assertThat(saved.getLastError()).isEqualTo("Proveedor push temporalmente no disponible.");
  }

  @Test
  void marksSkippedWithoutRetriesWhenDriverHasNoLinkedUser() {
    UUID tenantId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    TripAssignedNotification event = event(tenantId, driverId);

    when(fleetContextFacade.findDriversByTenantId(tenantId))
        .thenReturn(
            List.of(
                new FleetDriverSummary(
                    driverId, null, "Luis Quispe", "Q-123456", "A-IIb", "ENABLED")));

    service.handle(event);

    verify(pushAdapter, never()).send(any());
    ArgumentCaptor<RetryStoreEntry> entryCaptor = ArgumentCaptor.forClass(RetryStoreEntry.class);
    verify(retryStoreRepository, org.mockito.Mockito.times(2)).saveAndFlush(entryCaptor.capture());
    RetryStoreEntry saved = entryCaptor.getValue();
    assertThat(saved.getDeliveryStatus()).isEqualTo(RetryStoreEntry.SKIPPED);
    assertThat(saved.getNextAttemptAt()).isNull();
    assertThat(saved.getLastError()).contains("usuario vinculado");
  }

  @Test
  void marksSkippedWithoutRetriesWhenDriverHasNoRegisteredDevices() {
    UUID tenantId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    TripAssignedNotification event = event(tenantId, driverId);

    when(fleetContextFacade.findDriversByTenantId(tenantId))
        .thenReturn(
            List.of(
                new FleetDriverSummary(
                    driverId, userId, "Luis Quispe", "Q-123456", "A-IIb", "ENABLED")));
    when(driverDeviceRepository.findAllByTenantIdAndUserIdOrderByUpdatedAtDesc(tenantId, userId))
        .thenReturn(List.of());

    service.handle(event);

    verify(pushAdapter, never()).send(any());
    ArgumentCaptor<RetryStoreEntry> entryCaptor = ArgumentCaptor.forClass(RetryStoreEntry.class);
    verify(retryStoreRepository, org.mockito.Mockito.times(2)).saveAndFlush(entryCaptor.capture());
    RetryStoreEntry saved = entryCaptor.getValue();
    assertThat(saved.getDeliveryStatus()).isEqualTo(RetryStoreEntry.SKIPPED);
    assertThat(saved.getNextAttemptAt()).isNull();
    assertThat(saved.getLastError()).contains("dispositivos registrados");
  }

  private static TripAssignedNotification event(UUID tenantId, UUID driverId) {
    return new TripAssignedNotification(
        UUID.randomUUID(),
        T0,
        tenantId,
        UUID.randomUUID(),
        "TRP-000014",
        "Trujillo",
        LocalDate.of(2026, 11, 26),
        UUID.randomUUID(),
        driverId);
  }
}
