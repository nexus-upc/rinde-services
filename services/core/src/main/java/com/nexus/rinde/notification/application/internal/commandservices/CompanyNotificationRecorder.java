package com.nexus.rinde.notification.application.internal.commandservices;

import com.nexus.rinde.notification.domain.model.valueobjects.CompanyNotice;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.CompanyNotificationEntity;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Guarda cada aviso de empresa una sola vez por eventId, aunque el broker entregue el evento dos veces. */
@Service
public class CompanyNotificationRecorder {

  private static final Logger log = LoggerFactory.getLogger(CompanyNotificationRecorder.class);

  private final CompanyNotificationRepository notifications;
  private final TransactionTemplate newTransaction;

  public CompanyNotificationRecorder(
      CompanyNotificationRepository notifications, PlatformTransactionManager transactionManager) {
    this.notifications = notifications;
    this.newTransaction = new TransactionTemplate(transactionManager);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /** Ignora el aviso si su evento ya existe; si dos entregas chocan, la restricción única decide. */
  public void recordOnce(CompanyNotice notice) {
    if (notifications.existsByEventId(notice.eventId())) {
      return;
    }
    try {
      newTransaction.executeWithoutResult(
          status ->
              notifications.saveAndFlush(
                  CompanyNotificationEntity.create(
                      notice.eventId(),
                      notice.tenantId(),
                      notice.type().name(),
                      notice.title(),
                      notice.message(),
                      notice.link(),
                      notice.createdAt())));
    } catch (DataIntegrityViolationException ex) {
      if (!notifications.existsByEventId(notice.eventId())) {
        throw ex;
      }
      log.info("El evento {} ya tenía su notificación de empresa.", notice.eventId());
    }
  }
}
