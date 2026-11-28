package com.nexus.rinde.notification.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.rinde.fleet.interfaces.acl.MaintenanceDue;
import com.nexus.rinde.iam.interfaces.acl.IamContextFacade;
import com.nexus.rinde.notification.domain.model.valueobjects.EmailMessage;
import com.nexus.rinde.notification.domain.model.valueobjects.RetryPolicy;
import com.nexus.rinde.notification.domain.services.EmailAdapter;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.CompanyNotificationEntity;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.EmailOutboxEntry;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRepository;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.EmailOutboxRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

/** Prueba que un mantenimiento genera un solo correo y un solo aviso de empresa por evento. */
class MaintenanceDueNotificationServiceTest {

  private static final Instant T0 = Instant.parse("2026-10-08T06:00:00Z");
  private static final RetryPolicy POLICY =
      new RetryPolicy(List.of(Duration.ofMinutes(1), Duration.ofMinutes(2)));

  private final UUID tenantId = UUID.randomUUID();
  private final IamContextFacade iamContextFacade = mock(IamContextFacade.class);
  private final EmailOutboxRepository emailOutboxRepository = mock(EmailOutboxRepository.class);
  private final EmailAdapter emailAdapter = mock(EmailAdapter.class);
  private final CompanyNotificationRepository companyNotifications =
      mock(CompanyNotificationRepository.class);
  private MaintenanceDueNotificationService service;

  @BeforeEach
  void setUp() {
    when(iamContextFacade.findAdministratorEmail(tenantId))
        .thenReturn(Optional.of("ana@andes.pe"));
    when(emailOutboxRepository.saveAndFlush(any(EmailOutboxEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    AdministratorEmailOutbox outbox =
        new AdministratorEmailOutbox(
            iamContextFacade,
            emailOutboxRepository,
            emailAdapter,
            POLICY,
            Clock.fixed(T0, ZoneOffset.UTC));
    CompanyNotificationRecorder recorder =
        new CompanyNotificationRecorder(
            companyNotifications, mock(PlatformTransactionManager.class));
    service = new MaintenanceDueNotificationService(outbox, recorder);
  }

  @Test
  void queuesOneEmailForTheSameEventId() {
    UUID eventId = UUID.randomUUID();
    when(emailOutboxRepository.existsById(eventId)).thenReturn(false, true);
    MaintenanceDue event = dueEvent(eventId, "DUE_SOON");

    service.handle(event);
    service.handle(event);

    ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
    verify(emailAdapter, times(1)).send(sent.capture());
    assertThat(sent.getValue().recipient()).isEqualTo("ana@andes.pe");
    assertThat(sent.getValue().subject()).contains("ABC-123").contains("próximo");
    assertThat(sent.getValue().body()).contains("2026-10-13");
  }

  @Test
  void overdueMaintenanceIsDescribedAsVencido() {
    UUID eventId = UUID.randomUUID();
    when(emailOutboxRepository.existsById(eventId)).thenReturn(false);

    service.handle(dueEvent(eventId, "OVERDUE"));

    ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
    verify(emailAdapter, times(1)).send(sent.capture());
    assertThat(sent.getValue().subject()).contains("ABC-123").contains("vencido");
    assertThat(sent.getValue().body()).contains("vencido");
  }

  @Test
  void createsOneCompanyNoticeForTheSameEventId() {
    UUID eventId = UUID.randomUUID();
    when(emailOutboxRepository.existsById(eventId)).thenReturn(false, true);
    when(companyNotifications.existsByEventId(eventId)).thenReturn(false, true);

    MaintenanceDue event = dueEvent(eventId, "OVERDUE");
    service.handle(event);
    service.handle(event);

    ArgumentCaptor<CompanyNotificationEntity> saved =
        ArgumentCaptor.forClass(CompanyNotificationEntity.class);
    verify(companyNotifications, times(1)).saveAndFlush(saved.capture());
    CompanyNotificationEntity notice = saved.getValue();
    assertThat(notice.getEventId()).isEqualTo(eventId);
    assertThat(notice.getTenantId()).isEqualTo(tenantId);
    assertThat(notice.getType()).isEqualTo("MAINTENANCE_DUE");
    assertThat(notice.getTitle()).isEqualTo("Mantenimiento vencido");
    assertThat(notice.getLink()).startsWith("/fleet?vehicleId=");
    assertThat(notice.getCreatedAt()).isEqualTo(T0);
  }

  private MaintenanceDue dueEvent(UUID eventId, String state) {
    return new MaintenanceDue(
        eventId,
        T0,
        tenantId,
        UUID.randomUUID(),
        "ABC-123",
        state,
        LocalDate.of(2026, 10, 13));
  }
}
