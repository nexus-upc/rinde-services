package com.nexus.rinde.notification.application.internal.commandservices;

import com.nexus.rinde.notification.domain.model.valueobjects.EmailMessage;
import com.nexus.rinde.notification.domain.model.valueobjects.RetryPolicy;
import com.nexus.rinde.notification.domain.services.EmailAdapter;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.EmailOutboxEntry;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.RetryStoreEntry;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.EmailOutboxRepository;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.RetryStoreRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Reintenta los avisos push y los correos vencidos; cada uno se procesa en su propia transacción. */
@Service
public class NotificationRetryService {

  private static final Logger log = LoggerFactory.getLogger(NotificationRetryService.class);

  private final RetryStoreRepository retryStoreRepository;
  private final EmailOutboxRepository emailOutboxRepository;
  private final TripAssignmentPushDispatcher pushDispatcher;
  private final EmailAdapter emailAdapter;
  private final RetryPolicy retryPolicy;
  private final Clock clock;
  private final TransactionTemplate transactionTemplate;

  public NotificationRetryService(
      RetryStoreRepository retryStoreRepository,
      EmailOutboxRepository emailOutboxRepository,
      TripAssignmentPushDispatcher pushDispatcher,
      EmailAdapter emailAdapter,
      RetryPolicy retryPolicy,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    this.retryStoreRepository = retryStoreRepository;
    this.emailOutboxRepository = emailOutboxRepository;
    this.pushDispatcher = pushDispatcher;
    this.emailAdapter = emailAdapter;
    this.retryPolicy = retryPolicy;
    this.clock = clock;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  /** Procesa los avisos y correos cuyo próximo intento ya venció. */
  public void retryDueNotices() {
    Instant now = clock.instant();
    for (RetryStoreEntry entry :
        retryStoreRepository.findAllByDeliveryStatusAndNextAttemptAtLessThanEqual(
            RetryStoreEntry.PENDING, now)) {
      runIsolated(entry.getEventId(), () -> retryPush(entry.getEventId()));
    }
    for (EmailOutboxEntry entry :
        emailOutboxRepository.findAllByDeliveryStatusAndNextAttemptAtLessThanEqual(
            EmailOutboxEntry.PENDING, now)) {
      runIsolated(entry.getEventId(), () -> retryEmail(entry.getEventId()));
    }
  }

  /** Un fallo en un aviso no revierte ni detiene los demás. */
  private void runIsolated(UUID eventId, Runnable retry) {
    try {
      transactionTemplate.executeWithoutResult(status -> retry.run());
    } catch (RuntimeException ex) {
      log.error("No se pudo reintentar la notificación del evento {}.", eventId, ex);
    }
  }

  private void retryPush(UUID eventId) {
    Instant now = clock.instant();
    retryStoreRepository
        .findById(eventId)
        .filter(entry -> entry.isDueAt(now))
        .ifPresent(entry -> pushDispatcher.dispatch(entry, entry.toNotice(), true, now));
  }

  private void retryEmail(UUID eventId) {
    Instant now = clock.instant();
    emailOutboxRepository
        .findById(eventId)
        .filter(entry -> entry.isDueAt(now))
        .ifPresent(
            entry -> {
              EmailMessage message =
                  new EmailMessage(
                      entry.getEventId(),
                      entry.getTenantId(),
                      entry.getRecipientEmail(),
                      entry.getSubject(),
                      entry.getBody());
              try {
                emailAdapter.send(message);
                entry.markSimulatedSent(now);
              } catch (RuntimeException ex) {
                String detail =
                    ex.getMessage() == null ? "Falló la entrega del correo." : ex.getMessage();
                entry.markFailed(detail, retryPolicy, now);
                log.warn("Reintento de correo fallido para el evento {}.", eventId, ex);
              }
            });
  }
}
