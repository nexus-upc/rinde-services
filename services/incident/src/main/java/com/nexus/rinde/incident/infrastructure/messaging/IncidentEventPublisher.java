package com.nexus.rinde.incident.infrastructure.messaging;

import com.nexus.rinde.incident.domain.model.events.IncidentClosed;
import com.nexus.rinde.incident.domain.model.events.IncidentReported;
import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica en RabbitMQ los eventos de incidencia después del commit, para que otros servicios
 * reaccionen. Sin outbox: si el broker no responde, el fallo queda registrado en el log.
 */
@Component
public class IncidentEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(IncidentEventPublisher.class);
  public static final String REPORTED_ROUTING_KEY = "incident.reported";
  public static final String CLOSED_ROUTING_KEY = "incident.closed";

  private final RabbitTemplate rabbitTemplate;

  public IncidentEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onReported(IncidentReported event) {
    publish(REPORTED_ROUTING_KEY, event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onClosed(IncidentClosed event) {
    publish(CLOSED_ROUTING_KEY, event);
  }

  private void publish(String routingKey, IntegrationEvent event) {
    try {
      rabbitTemplate.convertAndSend(IncidentRabbitConfiguration.EVENTS_EXCHANGE, routingKey, event);
    } catch (AmqpException ex) {
      log.error("No se pudo publicar el evento {} ({}).", event.type(), event.eventId(), ex);
    }
  }
}
