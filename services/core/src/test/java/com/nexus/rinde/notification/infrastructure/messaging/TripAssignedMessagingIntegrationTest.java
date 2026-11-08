package com.nexus.rinde.notification.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nexus.rinde.notification.application.events.TripAssignedNotification;
import com.nexus.rinde.notification.application.internal.commandservices.TripAssignmentNotificationService;
import com.nexus.rinde.notification.domain.services.PushAdapter;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.RetryStoreEntry;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.RetryStoreRepository;
import com.nexus.rinde.notification.infrastructure.services.PushAdapterUnavailableException;
import com.nexus.rinde.support.AbstractIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Comprueba el JSON que publica Trip, los avisos repetidos, los fallos push y los mensajes inválidos
 * con RabbitMQ y PostgreSQL.
 */
class TripAssignedMessagingIntegrationTest extends AbstractIntegrationTest {

  @Autowired private RabbitTemplate rabbit;
  @Autowired private TopicExchange eventsExchange;
  @Autowired private AmqpAdmin admin;
  @Autowired private RabbitListenerEndpointRegistry listeners;
  @Autowired private RetryStoreRepository notices;
  @Autowired private Environment environment;
  @MockitoBean private PushAdapter pushAdapter;
  @MockitoSpyBean private TripAssignmentNotificationService notificationService;

  @BeforeEach
  void startConsumer() {
    admin.initialize();
    admin.purgeQueue(queue(), false);
    admin.purgeQueue(queue() + ".dead", false);
    listeners.start();
  }

  @AfterEach
  void stopConsumer() {
    listeners.stop();
  }

  @AfterAll
  static void removeQueues(@Autowired AmqpAdmin admin, @Autowired Environment environment) {
    String queue = environment.getRequiredProperty("rinde.messaging.notification.queue");
    admin.deleteQueue(queue);
    admin.deleteQueue(queue + ".dead");
  }

  @Test
  void receivesTheTripPublisherContractWithoutLoadingItsJavaType() {
    TripAssignedNotification event = event();
    send(event);
    awaitNotice(event.eventId());

    RetryStoreEntry notice = notices.findById(event.eventId()).orElseThrow();
    assertThat(notice.getTenantId()).isEqualTo(event.tenantId());
    assertThat(notice.getTripId()).isEqualTo(event.tripId());
    assertThat(notice.getRecipientDriverId()).isEqualTo(event.driverId());
    assertThat(notice.getMessage()).contains(event.code(), event.destination());
    assertThat(notice.getDeliveryStatus()).isEqualTo(RetryStoreEntry.DELIVERED);
    verify(pushAdapter).send(any());
  }

  @Test
  void ignoresRepeatedBrokerDeliveries() {
    TripAssignedNotification event = event();
    send(event);
    send(event);
    TripAssignedNotification sentinel = event();
    send(sentinel);
    awaitNotice(sentinel.eventId());

    assertThat(notices.count()).isEqualTo(2);
    verify(pushAdapter, times(2)).send(any());
  }

  @Test
  void keepsTheNoticePendingWhenPushIsUnavailable() {
    doThrow(new PushAdapterUnavailableException("Proveedor push no disponible."))
        .when(pushAdapter).send(any());
    TripAssignedNotification event = event();
    send(event);
    awaitNotice(event.eventId());

    RetryStoreEntry notice = notices.findById(event.eventId()).orElseThrow();
    assertThat(notice.getDeliveryStatus()).isEqualTo(RetryStoreEntry.PENDING);
    assertThat(notice.getRetryCount()).isZero();
    assertThat(notice.getNextAttemptAt()).isNotNull();
    assertThat(notice.getLastError()).contains("Proveedor push");
    verify(pushAdapter).send(any());
  }

  @Test
  void retriesATransientDatabaseFailureBeforeAcknowledgingTheMessage() {
    doThrow(new DataAccessResourceFailureException("Base temporalmente no disponible."))
        .doCallRealMethod().when(notificationService).handle(any(TripAssignedNotification.class));
    TripAssignedNotification event = event();
    send(event);
    awaitNotice(event.eventId());

    verify(notificationService, times(2)).handle(any(TripAssignedNotification.class));
    verify(pushAdapter).send(any());
    assertThat(notices.count()).isEqualTo(1);
  }

  @Test
  void preservesAnInvalidMessageInTheDeadLetterQueue() {
    MessageProperties properties = new MessageProperties();
    properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
    rabbit.send(eventsExchange.getName(), "trip.assigned",
        new Message("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8), properties));

    Message rejected = rabbit.receive(queue() + ".dead", 10_000);
    assertThat(rejected).isNotNull();
    assertThat(rejected.getBody()).isEqualTo("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    assertThat(notices.count()).isZero();
  }

  private String queue() {
    return environment.getRequiredProperty("rinde.messaging.notification.queue");
  }

  private void send(TripAssignedNotification event) {
    rabbit.convertAndSend(eventsExchange.getName(), "trip.assigned", event);
  }

  private void awaitNotice(UUID eventId) {
    await().atMost(Duration.ofSeconds(10)).until(() -> notices.existsById(eventId));
  }

  private TripAssignedNotification event() {
    return new TripAssignedNotification(UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
        UUID.randomUUID(), "TRP-000001", "Arequipa", LocalDate.now().plusDays(5), UUID.randomUUID(),
        UUID.randomUUID());
  }
}
