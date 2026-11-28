package com.nexus.rinde.notification.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.nexus.rinde.notification.application.events.ExpenseRegisteredNotification;
import com.nexus.rinde.notification.application.events.IncidentReportedNotification;
import com.nexus.rinde.notification.application.events.TripFinishedNotification;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.CompanyNotificationEntity;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRepository;
import com.nexus.rinde.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Comprueba que cada routing key de rinde.events crea un aviso de empresa, que un evento repetido no
 * duplica el aviso y que un mensaje inválido queda en su cola de errores. Usa colas propias de la
 * prueba para no tocar las colas del contrato en el broker compartido.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompanyNotificationMessagingIntegrationTest extends AbstractIntegrationTest {

  private static final String QUEUE_PREFIX = "test.company-notification." + UUID.randomUUID();
  private static final String EXPENSE_QUEUE = QUEUE_PREFIX + ".expense";
  private static final String INCIDENT_QUEUE = QUEUE_PREFIX + ".incident";
  private static final String TRIP_FINISHED_QUEUE = QUEUE_PREFIX + ".trip-finished";

  @Autowired private RabbitTemplate rabbit;
  @Autowired private TopicExchange eventsExchange;
  @Autowired private AmqpAdmin admin;
  @Autowired private RabbitListenerEndpointRegistry listeners;
  @Autowired private CompanyNotificationRepository notifications;

  @DynamicPropertySource
  static void notificationQueues(DynamicPropertyRegistry registry) {
    registry.add("rinde.messaging.notification.expense-queue", () -> EXPENSE_QUEUE);
    registry.add("rinde.messaging.notification.incident-queue", () -> INCIDENT_QUEUE);
    registry.add("rinde.messaging.notification.trip-finished-queue", () -> TRIP_FINISHED_QUEUE);
  }

  @BeforeEach
  void startConsumers() {
    admin.initialize();
    for (String queue : allQueues()) {
      admin.purgeQueue(queue, false);
    }
    listeners.start();
  }

  @AfterEach
  void stopConsumers() {
    listeners.stop();
  }

  @AfterAll
  void removeQueues() {
    for (String queue : allQueues()) {
      admin.deleteQueue(queue);
    }
  }

  @Test
  void createsOneNoticePerRoutingKeyFromTheEventsExchange() {
    UUID tenantId = UUID.randomUUID();
    ExpenseRegisteredNotification expense = expense(tenantId, "180.5", "PEN");
    IncidentReportedNotification incident = incident(tenantId);
    TripFinishedNotification trip = tripFinished(tenantId);

    send("expense.registered", expense);
    send("incident.reported", incident);
    send("trip.finished", trip);

    await().atMost(Duration.ofSeconds(15)).until(() -> notifications.count() == 3);

    CompanyNotificationEntity expenseNotice =
        notifications.findByEventId(expense.eventId()).orElseThrow();
    assertThat(expenseNotice.getTenantId()).isEqualTo(tenantId);
    assertThat(expenseNotice.getType()).isEqualTo("EXPENSE_PENDING_REVIEW");
    assertThat(expenseNotice.getMessage())
        .isEqualTo("Se registró un gasto por PEN 180.50 para revisar.");
    assertThat(expenseNotice.getLink()).isEqualTo("/expenses?tripId=" + expense.tripId());

    CompanyNotificationEntity incidentNotice =
        notifications.findByEventId(incident.eventId()).orElseThrow();
    assertThat(incidentNotice.getType()).isEqualTo("INCIDENT_REPORTED");
    assertThat(incidentNotice.getLink()).isEqualTo("/incidents/" + incident.incidentId());

    CompanyNotificationEntity tripNotice =
        notifications.findByEventId(trip.eventId()).orElseThrow();
    assertThat(tripNotice.getType()).isEqualTo("SETTLEMENT_READY");
    assertThat(tripNotice.getLink()).isEqualTo("/trips/" + trip.tripId());
  }

  @Test
  void ignoresRepeatedDeliveriesOfTheSameEvent() {
    UUID tenantId = UUID.randomUUID();
    ExpenseRegisteredNotification repeated = expense(tenantId, "99", "PEN");
    ExpenseRegisteredNotification sentinel = expense(tenantId, "15", "PEN");

    send("expense.registered", repeated);
    send("expense.registered", repeated);
    send("expense.registered", sentinel);

    // La cola se consume en orden: cuando llega el centinela, las entregas repetidas ya se procesaron.
    await().atMost(Duration.ofSeconds(15))
        .until(() -> notifications.existsByEventId(sentinel.eventId()));

    assertThat(notifications.count()).isEqualTo(2);
    assertThat(notifications.findByEventId(repeated.eventId())).isPresent();
  }

  @Test
  void keepsAnInvalidExpenseMessageInItsDeadLetterQueue() {
    MessageProperties properties = new MessageProperties();
    properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
    rabbit.send(
        eventsExchange.getName(),
        "expense.registered",
        new Message("{}".getBytes(StandardCharsets.UTF_8), properties));

    Message rejected = rabbit.receive(EXPENSE_QUEUE + ".dead", 10_000);

    assertThat(rejected).isNotNull();
    assertThat(rejected.getBody()).isEqualTo("{}".getBytes(StandardCharsets.UTF_8));
    assertThat(notifications.count()).isZero();
  }

  private void send(String routingKey, Object event) {
    rabbit.convertAndSend(eventsExchange.getName(), routingKey, event);
  }

  private ExpenseRegisteredNotification expense(UUID tenantId, String amount, String currency) {
    return new ExpenseRegisteredNotification(
        UUID.randomUUID(),
        tenantId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        new BigDecimal(amount),
        currency,
        Instant.now());
  }

  private IncidentReportedNotification incident(UUID tenantId) {
    return new IncidentReportedNotification(
        UUID.randomUUID(),
        Instant.now(),
        tenantId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "ACCIDENTE");
  }

  private TripFinishedNotification tripFinished(UUID tenantId) {
    return new TripFinishedNotification(
        UUID.randomUUID(),
        Instant.now(),
        tenantId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID());
  }

  private List<String> allQueues() {
    return List.of(
        EXPENSE_QUEUE,
        EXPENSE_QUEUE + ".dead",
        INCIDENT_QUEUE,
        INCIDENT_QUEUE + ".dead",
        TRIP_FINISHED_QUEUE,
        TRIP_FINISHED_QUEUE + ".dead");
  }
}
