package com.nexus.rinde.notification.application.internal.commandservices;

import com.nexus.rinde.notification.application.events.TripAssignedNotification;
import com.nexus.rinde.notification.domain.model.valueobjects.TripAssignmentNotice;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.RetryStoreEntry;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.RetryStoreRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Prepara el aviso de asignación y lo conserva si la entrega push no está disponible. */
@Service
public class TripAssignmentNotificationService {

  private final RetryStoreRepository retryStoreRepository;
  private final TripAssignmentPushDispatcher pushDispatcher;
  private final Clock clock;

  public TripAssignmentNotificationService(
      RetryStoreRepository retryStoreRepository,
      TripAssignmentPushDispatcher pushDispatcher,
      Clock clock) {
    this.retryStoreRepository = retryStoreRepository;
    this.pushDispatcher = pushDispatcher;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(TripAssignedNotification event) {
    if (retryStoreRepository.existsById(event.eventId())) {
      return;
    }

    Instant now = clock.instant();
    TripAssignmentNotice notice =
        TripAssignmentNotice.from(
            event.eventId(),
            event.tenantId(),
            event.tripId(),
            event.driverId(),
            event.code(),
            event.destination(),
            event.departureDate(),
            event.occurredAt());
    RetryStoreEntry entry =
        retryStoreRepository.saveAndFlush(RetryStoreEntry.prepared(notice, now));

    pushDispatcher.dispatch(entry, notice, false, now);
    retryStoreRepository.saveAndFlush(entry);
  }
}
