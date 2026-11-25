package com.nexus.rinde.notification.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexus.rinde.fleet.interfaces.acl.FleetContextFacade;
import com.nexus.rinde.fleet.interfaces.acl.FleetDriverSummary;
import com.nexus.rinde.notification.application.internal.outboundservices.acl.NotificationFleetService;
import com.nexus.rinde.notification.domain.model.valueobjects.EmailMessage;
import com.nexus.rinde.notification.domain.model.valueobjects.RetryPolicy;
import com.nexus.rinde.notification.domain.model.valueobjects.TripAssignmentNotice;
import com.nexus.rinde.notification.domain.services.EmailAdapter;
import com.nexus.rinde.notification.domain.services.PushAdapter;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.DriverDeviceEntity;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.EmailOutboxEntry;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.RetryStoreEntry;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.DriverDeviceRepository;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.EmailOutboxRepository;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.RetryStoreRepository;
import com.nexus.rinde.notification.infrastructure.services.PushAdapterUnavailableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Prueba la política de reintentos con repositorios simulados que aplican la misma condición de
 * vencimiento que la consulta, un reloj controlable y adaptadores de doble.
 */
class NotificationRetryServiceTest {

  private static final Instant T0 = Instant.parse("2026-10-20T10:00:00Z");
  private static final RetryPolicy POLICY =
      new RetryPolicy(
          List.of(
              Duration.ofMinutes(1),
              Duration.ofMinutes(2),
              Duration.ofMinutes(4),
              Duration.ofMinutes(8),
              Duration.ofMinutes(16)));

  private final UUID tenantId = UUID.randomUUID();
  private final List<RetryStoreEntry> pushStore = new ArrayList<>();
  private final List<EmailOutboxEntry> emailStore = new ArrayList<>();
  private final RetryStoreRepository retryStoreRepository = mock(RetryStoreRepository.class);
  private final EmailOutboxRepository emailOutboxRepository = mock(EmailOutboxRepository.class);
  private final FleetContextFacade fleetContextFacade = mock(FleetContextFacade.class);
  private final DriverDeviceRepository driverDeviceRepository = mock(DriverDeviceRepository.class);
  private final FakePushAdapter pushAdapter = new FakePushAdapter();
  private final FakeEmailAdapter emailAdapter = new FakeEmailAdapter();
  private final MutableClock clock = new MutableClock(T0);
  private NotificationRetryService service;

  @BeforeEach
  void setUp() {
    when(fleetContextFacade.findDriversByTenantId(tenantId)).thenReturn(List.of());
    when(retryStoreRepository.findAllByDeliveryStatusAndNextAttemptAtLessThanEqual(
            anyString(), any(Instant.class)))
        .thenAnswer(
            invocation -> {
              String status = invocation.getArgument(0);
              Instant now = invocation.getArgument(1);
              return pushStore.stream()
                  .filter(entry -> isDue(entry.getDeliveryStatus(), entry.getNextAttemptAt(), status, now))
                  .toList();
            });
    when(retryStoreRepository.findById(any(UUID.class)))
        .thenAnswer(
            invocation ->
                pushStore.stream()
                    .filter(entry -> entry.getEventId().equals(invocation.getArgument(0)))
                    .findFirst());
    when(emailOutboxRepository.findAllByDeliveryStatusAndNextAttemptAtLessThanEqual(
            anyString(), any(Instant.class)))
        .thenAnswer(
            invocation -> {
              String status = invocation.getArgument(0);
              Instant now = invocation.getArgument(1);
              return emailStore.stream()
                  .filter(entry -> isDue(entry.getDeliveryStatus(), entry.getNextAttemptAt(), status, now))
                  .toList();
            });
    when(emailOutboxRepository.findById(any(UUID.class)))
        .thenAnswer(
            invocation ->
                emailStore.stream()
                    .filter(entry -> entry.getEventId().equals(invocation.getArgument(0)))
                    .findFirst());

    PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    TripAssignmentPushDispatcher pushDispatcher =
        new TripAssignmentPushDispatcher(
            new NotificationFleetService(fleetContextFacade),
            driverDeviceRepository,
            pushAdapter,
            POLICY);
    service =
        new NotificationRetryService(
            retryStoreRepository,
            emailOutboxRepository,
            pushDispatcher,
            emailAdapter,
            POLICY,
            clock,
            transactionManager);
  }

  @Test
  void doesNotRetryBeforeTheWaitHasPassed() {
    RetryStoreEntry entry = pushNoticeFailedAtFirstAttempt();
    pushAdapter.failing.add(entry.getEventId());

    clock.setInstant(T0.plusSeconds(59));
    service.retryDueNotices();

    assertThat(pushAdapter.attemptsFor(entry.getEventId())).isZero();
    assertThat(entry.getRetryCount()).isZero();
    assertThat(entry.getDeliveryStatus()).isEqualTo(RetryStoreEntry.PENDING);
  }

  @Test
  void deliversOnceTheWaitHasPassedAndNeverSendsAgain() {
    RetryStoreEntry entry = pushNoticeFailedAtFirstAttempt();

    clock.setInstant(T0.plus(Duration.ofMinutes(1)));
    service.retryDueNotices();

    assertThat(entry.getDeliveryStatus()).isEqualTo(RetryStoreEntry.DELIVERED);
    assertThat(entry.getNextAttemptAt()).isNull();
    assertThat(entry.getDeliveredAt()).isEqualTo(clock.instant());
    assertThat(pushAdapter.attemptsFor(entry.getEventId())).isEqualTo(1);

    clock.advance(Duration.ofHours(1));
    service.retryDueNotices();

    assertThat(pushAdapter.attemptsFor(entry.getEventId())).isEqualTo(1);
  }

  @Test
  void endsFailedAfterFiveFailedRetriesWithWaitsOneTwoFourEightSixteen() {
    RetryStoreEntry entry = pushNoticeFailedAtFirstAttempt();
    pushAdapter.failing.add(entry.getEventId());

    // Reintentos en T0+1, T0+3, T0+7, T0+15 y T0+31 minutos desde la primera falla.
    List<Instant> retryInstants = new ArrayList<>();
    for (long minutes : new long[] {1, 3, 7, 15, 31}) {
      retryInstants.add(T0.plus(Duration.ofMinutes(minutes)));
    }
    for (int i = 0; i < retryInstants.size(); i++) {
      clock.setInstant(retryInstants.get(i));
      service.retryDueNotices();
      assertThat(pushAdapter.attemptsFor(entry.getEventId())).isEqualTo(i + 1);
      assertThat(entry.getRetryCount()).isEqualTo(i + 1);
      assertThat(entry.getLastError()).isEqualTo("Sin proveedor push.");
    }

    assertThat(entry.getDeliveryStatus()).isEqualTo(RetryStoreEntry.FAILED);
    assertThat(entry.getNextAttemptAt()).isNull();

    clock.advance(Duration.ofDays(1));
    service.retryDueNotices();

    assertThat(pushAdapter.attemptsFor(entry.getEventId())).isEqualTo(5);
    assertThat(entry.getDeliveryStatus()).isEqualTo(RetryStoreEntry.FAILED);
  }

  @Test
  void oneFailingNoticeDoesNotPreventAnotherDueNoticeFromBeingDelivered() {
    RetryStoreEntry failing = pushNoticeFailedAtFirstAttempt();
    RetryStoreEntry healthy = pushNoticeFailedAtFirstAttempt();
    pushAdapter.failing.add(failing.getEventId());

    clock.setInstant(T0.plus(Duration.ofMinutes(1)));
    service.retryDueNotices();

    assertThat(failing.getDeliveryStatus()).isEqualTo(RetryStoreEntry.PENDING);
    assertThat(failing.getRetryCount()).isEqualTo(1);
    assertThat(healthy.getDeliveryStatus()).isEqualTo(RetryStoreEntry.DELIVERED);
  }

  @Test
  void skipsPendingRetryWhenDriverRemovedAllRegisteredDevices() {
    RetryStoreEntry entry = pushNoticeFailedAtFirstAttempt();
    UUID driverId = entry.getRecipientDriverId();
    UUID userId =
        fleetContextFacade.findDriversByTenantId(tenantId).stream()
            .filter(driver -> driverId.equals(driver.id()))
            .findFirst()
            .orElseThrow()
            .userId();
    when(driverDeviceRepository.findAllByTenantIdAndUserIdOrderByUpdatedAtDesc(tenantId, userId))
        .thenReturn(List.of());

    clock.setInstant(T0.plus(Duration.ofMinutes(1)));
    service.retryDueNotices();

    assertThat(entry.getDeliveryStatus()).isEqualTo(RetryStoreEntry.SKIPPED);
    assertThat(entry.getNextAttemptAt()).isNull();
    assertThat(pushAdapter.attemptsFor(entry.getEventId())).isZero();
  }

  @Test
  void retriesEmailUntilDelivered() {
    EmailOutboxEntry entry = emailNoticeFailedAtFirstAttempt();
    emailAdapter.failing.add(entry.getEventId());

    clock.setInstant(T0.plus(Duration.ofMinutes(1)));
    service.retryDueNotices();
    assertThat(entry.getDeliveryStatus()).isEqualTo(EmailOutboxEntry.PENDING);
    assertThat(entry.getNextAttemptAt()).isEqualTo(T0.plus(Duration.ofMinutes(3)));

    emailAdapter.failing.remove(entry.getEventId());
    clock.setInstant(T0.plus(Duration.ofMinutes(3)));
    service.retryDueNotices();

    assertThat(entry.getDeliveryStatus()).isEqualTo(EmailOutboxEntry.SIMULATED_SENT);
    assertThat(entry.getLastError()).isNull();
    assertThat(emailAdapter.attemptsFor(entry.getEventId())).isEqualTo(2);

    clock.advance(Duration.ofHours(1));
    service.retryDueNotices();
    assertThat(emailAdapter.attemptsFor(entry.getEventId())).isEqualTo(2);
  }

  @Test
  void emailEndsFailedAfterSixAttemptsInTotal() {
    EmailOutboxEntry entry = emailNoticeFailedAtFirstAttempt();
    emailAdapter.failing.add(entry.getEventId());

    for (long minutes : new long[] {1, 3, 7, 15, 31}) {
      clock.setInstant(T0.plus(Duration.ofMinutes(minutes)));
      service.retryDueNotices();
    }
    clock.advance(Duration.ofDays(1));
    service.retryDueNotices();

    assertThat(entry.getDeliveryStatus()).isEqualTo(EmailOutboxEntry.FAILED);
    assertThat(entry.getDeliveryAttempts()).isEqualTo(6);
    assertThat(entry.getLastError()).isEqualTo("Sin proveedor de correo.");
    assertThat(emailAdapter.attemptsFor(entry.getEventId())).isEqualTo(5);
  }

  /** Aviso cuya primera entrega falló en T0; su primer reintento vence a T0+1 minuto. */
  private RetryStoreEntry pushNoticeFailedAtFirstAttempt() {
    UUID driverId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    List<FleetDriverSummary> currentDrivers =
        new ArrayList<>(fleetContextFacade.findDriversByTenantId(tenantId));
    currentDrivers.add(
        new FleetDriverSummary(driverId, userId, "Conductor", "LIC-001", "A-IIb", "ENABLED"));
    when(fleetContextFacade.findDriversByTenantId(tenantId)).thenReturn(currentDrivers);
    when(driverDeviceRepository.findAllByTenantIdAndUserIdOrderByUpdatedAtDesc(tenantId, userId))
        .thenReturn(
            List.of(DriverDeviceEntity.register(tenantId, userId, "token-" + driverId, "ANDROID", T0)));

    TripAssignmentNotice notice =
        TripAssignmentNotice.from(
            UUID.randomUUID(),
            tenantId,
            UUID.randomUUID(),
            driverId,
            "VIA-" + pushStore.size(),
            "Arequipa",
            LocalDate.of(2026, 10, 20),
            T0);
    RetryStoreEntry entry = RetryStoreEntry.prepared(notice, T0);
    entry.markFailed("Sin proveedor push.", false, POLICY, T0);
    pushStore.add(entry);
    return entry;
  }

  /** Correo cuya primera entrega falló en T0; su primer reintento vence a T0+1 minuto. */
  private EmailOutboxEntry emailNoticeFailedAtFirstAttempt() {
    EmailMessage message =
        new EmailMessage(
            UUID.randomUUID(), tenantId, "ana@andes.pe", "Asunto", "Cuerpo del correo.");
    EmailOutboxEntry entry = EmailOutboxEntry.pending(message, T0);
    entry.markFailed("Sin proveedor de correo.", POLICY, T0);
    emailStore.add(entry);
    return entry;
  }

  private static boolean isDue(String current, Instant nextAttemptAt, String status, Instant now) {
    return status.equals(current) && nextAttemptAt != null && !nextAttemptAt.isAfter(now);
  }

  /** Reloj cuyo instante se fija o avanza en cada prueba. */
  private static final class MutableClock extends Clock {

    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    void setInstant(Instant instant) {
      this.now = instant;
    }

    void advance(Duration duration) {
      this.now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  /** Adaptador push de doble: falla para los avisos marcados y registra cada intento. */
  private static final class FakePushAdapter implements PushAdapter {

    private final Set<UUID> failing = new HashSet<>();
    private final List<UUID> attempts = new ArrayList<>();

    @Override
    public void send(TripAssignmentNotice notice) {
      attempts.add(notice.eventId());
      if (failing.contains(notice.eventId())) {
        throw new PushAdapterUnavailableException("Sin proveedor push.");
      }
    }

    long attemptsFor(UUID eventId) {
      return attempts.stream().filter(eventId::equals).count();
    }
  }

  /** Adaptador de correo de doble: falla para los mensajes marcados y registra cada intento. */
  private static final class FakeEmailAdapter implements EmailAdapter {

    private final Set<UUID> failing = new HashSet<>();
    private final List<UUID> attempts = new ArrayList<>();

    @Override
    public void send(EmailMessage message) {
      attempts.add(message.eventId());
      if (failing.contains(message.eventId())) {
        throw new IllegalStateException("Sin proveedor de correo.");
      }
    }

    long attemptsFor(UUID eventId) {
      return attempts.stream().filter(eventId::equals).count();
    }
  }
}
