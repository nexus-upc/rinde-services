package com.nexus.rinde.notification.application.internal.queryservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.rinde.notification.domain.model.queries.GetUnreadNotificationCountQuery;
import com.nexus.rinde.notification.domain.model.queries.ListCompanyNotificationsQuery;
import com.nexus.rinde.notification.domain.model.valueobjects.NotificationPage;
import com.nexus.rinde.notification.domain.model.valueobjects.NotificationType;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRepository;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRow;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class CompanyNotificationQueryServiceImplTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final CompanyNotificationRepository notifications =
      mock(CompanyNotificationRepository.class);
  private final CompanyNotificationQueryServiceImpl service =
      new CompanyNotificationQueryServiceImpl(notifications);

  @Test
  void rejectsNegativePage() {
    assertThatThrownBy(() -> service.handle(query(false, Set.of(), -1, 20)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El número de página no puede ser negativo.");
    verify(notifications, never()).findPageForUser(any(), any(), anyBoolean(), anyCollection(), any());
  }

  @Test
  void rejectsSizeOutsideOneToOneHundred() {
    for (int size : List.of(0, 101)) {
      assertThatThrownBy(() -> service.handle(query(false, Set.of(), 0, size)))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessage("El tamaño de página debe estar entre 1 y 100.");
    }
    verify(notifications, never()).findPageForUser(any(), any(), anyBoolean(), anyCollection(), any());
  }

  @Test
  void listsAllTypesWhenNoTypeFilterIsGiven() {
    when(notifications.findPageForUser(
            eq(tenantId), eq(userId), eq(false), anyCollection(), any()))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    service.handle(query(false, Set.of(), 0, 20));

    verify(notifications)
        .findPageForUser(
            eq(tenantId),
            eq(userId),
            eq(false),
            eq(List.of("EXPENSE_PENDING_REVIEW", "MAINTENANCE_DUE", "INCIDENT_REPORTED", "SETTLEMENT_READY")),
            eq(PageRequest.of(0, 20)));
  }

  @Test
  void passesTypeAndUnreadFiltersAndMapsReadState() {
    Instant createdAt = Instant.parse("2026-11-25T14:00:00Z");
    Instant readAt = Instant.parse("2026-11-25T15:00:00Z");
    UUID notificationId = UUID.randomUUID();
    when(notifications.findPageForUser(
            eq(tenantId),
            eq(userId),
            eq(true),
            eq(List.of("INCIDENT_REPORTED")),
            eq(PageRequest.of(1, 2))))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new CompanyNotificationRow(
                        notificationId,
                        "INCIDENT_REPORTED",
                        "Incidencia reportada",
                        "Se reportó una incidencia.",
                        "/incidents/" + notificationId,
                        createdAt,
                        readAt)),
                PageRequest.of(1, 2),
                3));

    NotificationPage page =
        service.handle(query(true, Set.of(NotificationType.INCIDENT_REPORTED), 1, 2));

    assertThat(page.page()).isEqualTo(1);
    assertThat(page.size()).isEqualTo(2);
    assertThat(page.totalItems()).isEqualTo(3);
    assertThat(page.totalPages()).isEqualTo(2);
    assertThat(page.items()).hasSize(1);
    assertThat(page.items().getFirst().id()).isEqualTo(notificationId);
    assertThat(page.items().getFirst().readAt()).isEqualTo(readAt);
  }

  @Test
  void countsUnreadNotificationsForTheUserAcrossAllTypes() {
    when(notifications.countUnreadForUser(tenantId, userId)).thenReturn(3L);

    long count = service.handle(new GetUnreadNotificationCountQuery(tenantId, userId));

    assertThat(count).isEqualTo(3L);
  }

  private ListCompanyNotificationsQuery query(
      boolean unreadOnly, Set<NotificationType> types, int page, int size) {
    return new ListCompanyNotificationsQuery(tenantId, userId, unreadOnly, types, page, size);
  }
}
