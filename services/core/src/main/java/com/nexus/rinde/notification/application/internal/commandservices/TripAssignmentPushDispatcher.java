package com.nexus.rinde.notification.application.internal.commandservices;

import com.nexus.rinde.notification.application.internal.outboundservices.acl.NotificationFleetService;
import com.nexus.rinde.notification.domain.model.valueobjects.RetryPolicy;
import com.nexus.rinde.notification.domain.model.valueobjects.TripAssignmentNotice;
import com.nexus.rinde.notification.domain.services.PushAdapter;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.DriverDeviceEntity;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.RetryStoreEntry;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.DriverDeviceRepository;
import com.nexus.rinde.notification.infrastructure.services.PushAdapterUnavailableException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Resuelve el usuario destinatario y sus tokens para despachar o reintentar avisos push. */
@Component
public class TripAssignmentPushDispatcher {

  private static final Logger log = LoggerFactory.getLogger(TripAssignmentPushDispatcher.class);

  private final NotificationFleetService notificationFleetService;
  private final DriverDeviceRepository driverDeviceRepository;
  private final PushAdapter pushAdapter;
  private final RetryPolicy retryPolicy;

  public TripAssignmentPushDispatcher(
      NotificationFleetService notificationFleetService,
      DriverDeviceRepository driverDeviceRepository,
      PushAdapter pushAdapter,
      RetryPolicy retryPolicy) {
    this.notificationFleetService = notificationFleetService;
    this.driverDeviceRepository = driverDeviceRepository;
    this.pushAdapter = pushAdapter;
    this.retryPolicy = retryPolicy;
  }

  /** Envía el aviso a los dispositivos activos del conductor o actualiza el estado según el resultado. */
  public void dispatch(
      RetryStoreEntry entry, TripAssignmentNotice notice, boolean retry, Instant now) {
    try {
      Optional<UUID> userId =
          notificationFleetService.findUserIdByDriverId(notice.tenantId(), notice.driverId());
      if (userId.isEmpty()) {
        entry.markSkipped(
            "El conductor no tiene un usuario vinculado para recibir avisos.", now);
        log.info(
            "Aviso push omitido para el evento {}: conductor {} sin usuario vinculado.",
            notice.eventId(),
            notice.driverId());
        return;
      }

      List<String> tokens =
          driverDeviceRepository
              .findAllByTenantIdAndUserIdOrderByUpdatedAtDesc(notice.tenantId(), userId.get())
              .stream()
              .map(DriverDeviceEntity::getToken)
              .toList();
      if (tokens.isEmpty()) {
        entry.markSkipped(
            "El conductor no tiene dispositivos registrados para recibir avisos.", now);
        log.info(
            "Aviso push omitido para el evento {}: conductor {} sin dispositivos registrados.",
            notice.eventId(),
            notice.driverId());
        return;
      }

      pushAdapter.send(notice.withDeviceTokens(tokens));
      entry.markDelivered(now);
    } catch (PushAdapterUnavailableException ex) {
      entry.markFailed(ex.getMessage(), retry, retryPolicy, now);
      log.info("Aviso push pendiente para el conductor {}: {}", notice.driverId(), ex.getMessage());
    } catch (RuntimeException ex) {
      String detail = ex.getMessage() == null ? "Falló la entrega push." : ex.getMessage();
      entry.markFailed(detail, retry, retryPolicy, now);
      log.warn("Falló la entrega push del evento {} y quedó pendiente.", notice.eventId(), ex);
    }
  }
}
