package com.nexus.rinde.notification.application.internal.queryservices;

import com.nexus.rinde.notification.domain.model.queries.GetUnreadNotificationCountQuery;
import com.nexus.rinde.notification.domain.model.queries.ListCompanyNotificationsQuery;
import com.nexus.rinde.notification.domain.model.valueobjects.NotificationPage;
import com.nexus.rinde.notification.domain.services.CompanyNotificationQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Implementación base pendiente de consultas del centro de notificaciones (S4-25). */
@Service
public class CompanyNotificationQueryServiceImpl implements CompanyNotificationQueryService {

  @Override
  public NotificationPage handle(ListCompanyNotificationsQuery query) {
    throw new ResponseStatusException(
        HttpStatus.NOT_IMPLEMENTED, "Funcionalidad pendiente de implementar en S4-25.");
  }

  @Override
  public long handle(GetUnreadNotificationCountQuery query) {
    throw new ResponseStatusException(
        HttpStatus.NOT_IMPLEMENTED, "Funcionalidad pendiente de implementar en S4-25.");
  }
}
