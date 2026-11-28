package com.nexus.rinde.notification.application.internal.queryservices;

import com.nexus.rinde.notification.domain.model.queries.GetUnreadNotificationCountQuery;
import com.nexus.rinde.notification.domain.model.queries.ListCompanyNotificationsQuery;
import com.nexus.rinde.notification.domain.model.valueobjects.NotificationPage;
import com.nexus.rinde.notification.domain.model.valueobjects.NotificationPage.NotificationItem;
import com.nexus.rinde.notification.domain.model.valueobjects.NotificationType;
import com.nexus.rinde.notification.domain.services.CompanyNotificationQueryService;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRepository;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRow;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas del centro de notificaciones: listado paginado y conteo de no leídas por usuario. */
@Service
public class CompanyNotificationQueryServiceImpl implements CompanyNotificationQueryService {

  private static final int MAX_PAGE_SIZE = 100;
  private static final List<String> ALL_TYPES =
      Arrays.stream(NotificationType.values()).map(Enum::name).toList();

  private final CompanyNotificationRepository notifications;

  public CompanyNotificationQueryServiceImpl(CompanyNotificationRepository notifications) {
    this.notifications = notifications;
  }

  @Override
  @Transactional(readOnly = true)
  public NotificationPage handle(ListCompanyNotificationsQuery query) {
    validatePaging(query.page(), query.size());
    List<String> types =
        query.types() == null || query.types().isEmpty()
            ? ALL_TYPES
            : query.types().stream().map(Enum::name).toList();

    Page<CompanyNotificationRow> rows =
        notifications.findPageForUser(
            query.tenantId(),
            query.userId(),
            query.unreadOnly(),
            types,
            PageRequest.of(query.page(), query.size()));

    List<NotificationItem> items =
        rows.getContent().stream()
            .map(
                row ->
                    new NotificationItem(
                        row.id(),
                        row.type(),
                        row.title(),
                        row.message(),
                        row.link(),
                        row.createdAt(),
                        row.readAt()))
            .toList();
    return new NotificationPage(
        items, query.page(), query.size(), rows.getTotalElements(), rows.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public long handle(GetUnreadNotificationCountQuery query) {
    return notifications.countUnreadForUser(query.tenantId(), query.userId());
  }

  private static void validatePaging(int page, int size) {
    if (page < 0) {
      throw new BusinessRuleException("El número de página no puede ser negativo.");
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new BusinessRuleException("El tamaño de página debe estar entre 1 y 100.");
    }
  }
}
