package com.nexus.rinde.notification.domain.services;

import com.nexus.rinde.notification.domain.model.queries.GetUnreadNotificationCountQuery;
import com.nexus.rinde.notification.domain.model.queries.ListCompanyNotificationsQuery;
import com.nexus.rinde.notification.domain.model.valueobjects.NotificationPage;

/** Consultas del centro de notificaciones para administradores y operadores. */
public interface CompanyNotificationQueryService {

  NotificationPage handle(ListCompanyNotificationsQuery query);

  long handle(GetUnreadNotificationCountQuery query);
}
