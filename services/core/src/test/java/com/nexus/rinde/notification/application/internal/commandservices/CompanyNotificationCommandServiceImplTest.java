package com.nexus.rinde.notification.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.rinde.notification.domain.model.commands.MarkAllNotificationsReadCommand;
import com.nexus.rinde.notification.domain.model.commands.MarkNotificationReadCommand;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRepository;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.NotificationReadRepository;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompanyNotificationCommandServiceImplTest {

  private static final Instant NOW = Instant.parse("2026-11-28T10:00:00Z");

  private final UUID tenantId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID notificationId = UUID.randomUUID();
  private final CompanyNotificationRepository notifications =
      mock(CompanyNotificationRepository.class);
  private final NotificationReadRepository reads = mock(NotificationReadRepository.class);
  private final CompanyNotificationCommandServiceImpl service =
      new CompanyNotificationCommandServiceImpl(
          notifications, reads, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void marksExistingNotificationAsReadForTheUserWithoutReplacingPreviousRead() {
    when(notifications.existsByIdAndTenantId(notificationId, tenantId)).thenReturn(true);

    service.handle(new MarkNotificationReadCommand(tenantId, userId, notificationId));

    verify(reads).insertReadIfAbsent(notificationId, userId, tenantId, NOW);
  }

  @Test
  void rejectsNotificationThatDoesNotExistInTheCompany() {
    when(notifications.existsByIdAndTenantId(notificationId, tenantId)).thenReturn(false);

    assertThatThrownBy(
            () -> service.handle(new MarkNotificationReadCommand(tenantId, userId, notificationId)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("La notificación no existe.");
    verify(reads, never()).insertReadIfAbsent(any(), any(), any(), any());
  }

  @Test
  void marksAllNotificationsOfTheTenantAsReadForTheUser() {
    service.handle(new MarkAllNotificationsReadCommand(tenantId, userId));

    verify(reads).insertReadsForAllOfTenant(eq(userId), eq(tenantId), eq(NOW));
  }
}
