package com.nexus.rinde.notification.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.rinde.notification.domain.model.valueobjects.CompanyNotice;
import com.nexus.rinde.notification.domain.model.valueobjects.NotificationType;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.CompanyNotificationEntity;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

class CompanyNotificationRecorderTest {

  private final CompanyNotificationRepository notifications =
      mock(CompanyNotificationRepository.class);
  private final CompanyNotificationRecorder recorder =
      new CompanyNotificationRecorder(notifications, mock(PlatformTransactionManager.class));

  @Test
  void savesTheNoticeWhenTheEventIsNew() {
    UUID eventId = UUID.randomUUID();
    CompanyNotice notice = notice(eventId);

    recorder.recordOnce(notice);

    ArgumentCaptor<CompanyNotificationEntity> saved =
        ArgumentCaptor.forClass(CompanyNotificationEntity.class);
    verify(notifications, times(1)).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getEventId()).isEqualTo(eventId);
    assertThat(saved.getValue().getType()).isEqualTo("EXPENSE_PENDING_REVIEW");
    assertThat(saved.getValue().getLink()).isEqualTo("/expenses?tripId=abc");
  }

  @Test
  void ignoresARepeatedEventWithoutSaving() {
    UUID eventId = UUID.randomUUID();
    when(notifications.existsByEventId(eventId)).thenReturn(true);

    recorder.recordOnce(notice(eventId));

    verify(notifications, never()).saveAndFlush(any());
  }

  @Test
  void treatsAConcurrentDuplicateAsAlreadyRecorded() {
    UUID eventId = UUID.randomUUID();
    when(notifications.existsByEventId(eventId)).thenReturn(false, true);
    when(notifications.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("uk_company_notifications_event_id"));

    recorder.recordOnce(notice(eventId));

    verify(notifications, times(1)).saveAndFlush(any());
  }

  @Test
  void rethrowsAnIntegrityErrorThatIsNotADuplicate() {
    UUID eventId = UUID.randomUUID();
    DataIntegrityViolationException failure =
        new DataIntegrityViolationException("otra restricción");
    when(notifications.existsByEventId(eventId)).thenReturn(false, false);
    when(notifications.saveAndFlush(any())).thenThrow(failure);

    assertThatThrownBy(() -> recorder.recordOnce(notice(eventId))).isSameAs(failure);
  }

  private CompanyNotice notice(UUID eventId) {
    return new CompanyNotice(
        eventId,
        UUID.randomUUID(),
        NotificationType.EXPENSE_PENDING_REVIEW,
        "Gasto pendiente de revisión",
        "Se registró un gasto por PEN 180.50 para revisar.",
        "/expenses?tripId=abc",
        Instant.parse("2026-11-25T14:00:00Z"));
  }
}
