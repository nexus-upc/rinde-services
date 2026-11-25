package com.nexus.rinde.expense.infrastructure.messaging;

import com.nexus.rinde.expense.domain.model.events.ExpenseRegistered;
import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Publica en RabbitMQ el registro de gastos nuevos después de confirmar la transacción. */
@Component
public class ExpenseEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(ExpenseEventPublisher.class);
  public static final String EXPENSE_REGISTERED_ROUTING_KEY = "expense.registered";

  private final RabbitTemplate rabbitTemplate;

  public ExpenseEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onRegistered(ExpenseRegistered event) {
    publish(EXPENSE_REGISTERED_ROUTING_KEY, event);
  }

  private void publish(String routingKey, IntegrationEvent event) {
    try {
      rabbitTemplate.convertAndSend(ExpenseRabbitConfiguration.EVENTS_EXCHANGE, routingKey, event);
    } catch (AmqpException ex) {
      log.error("No se pudo publicar el evento {} ({}).", event.type(), event.eventId(), ex);
    }
  }
}
