package com.nexus.rinde.settlement.infrastructure.messaging;

import com.nexus.rinde.settlement.interfaces.acl.SettlementClosed;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Publica settlement.closed en RabbitMQ después del commit; idempotente por eventId. */
@Component
public class SettlementEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(SettlementEventPublisher.class);
  private static final String ROUTING_KEY = "settlement.closed";

  private final RabbitTemplate rabbit;
  private final String exchange;

  public SettlementEventPublisher(RabbitTemplate rabbit, TopicExchange rindeEventsExchange) {
    this.rabbit = rabbit;
    this.exchange = rindeEventsExchange.getName();
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(SettlementClosed event) {
    CorrelationData correlation = new CorrelationData(event.eventId().toString());
    try {
      rabbit.convertAndSend(
          exchange,
          ROUTING_KEY,
          event,
          message -> {
            message.getMessageProperties().setMessageId(event.eventId().toString());
            message.getMessageProperties().setType(event.type());
            return message;
          },
          correlation);
      var confirmation = correlation.getFuture().get(5, TimeUnit.SECONDS);
      if (!confirmation.isAck() || correlation.getReturned() != null) {
        throw new AmqpException("El broker rechazó settlement.closed o no encontró una cola receptora.");
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Se interrumpió la publicación de settlement.closed ({}). Requiere conciliación.",
          event.eventId(), ex);
    } catch (Exception ex) {
      log.error("No se pudo publicar settlement.closed ({}). La transacción ya terminó; requiere conciliación.",
          event.eventId(), ex);
    }
  }
}
