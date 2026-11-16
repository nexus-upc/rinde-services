package com.nexus.rinde.subscription.infrastructure.messaging;

import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import com.nexus.rinde.subscription.interfaces.acl.PaymentReceiptRequested;
import com.nexus.rinde.subscription.interfaces.acl.PlanChanged;
import com.nexus.rinde.subscription.interfaces.acl.SubscriptionActivated;
import com.nexus.rinde.subscription.interfaces.acl.SubscriptionExpiring;
import com.nexus.rinde.subscription.interfaces.acl.SubscriptionSuspended;
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

/** Publica después del commit y comprueba la confirmación del broker y la existencia de una ruta. */
@Component
public class SubscriptionEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionEventPublisher.class);
  private final RabbitTemplate rabbit;
  private final String exchange;

  public SubscriptionEventPublisher(RabbitTemplate rabbit, TopicExchange rindeEventsExchange) {
    this.rabbit = rabbit;
    this.exchange = rindeEventsExchange.getName();
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(SubscriptionActivated event) {
    publish("subscription.activated", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(SubscriptionSuspended event) {
    publish("subscription.suspended", event);
  }

  /** Fleet separado ajusta con este evento el límite de unidades de la empresa. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(PlanChanged event) {
    publish("subscription.plan-changed", event);
  }

  /** Notifications consume el aviso de vencimiento desde su cola; ya no sale por un evento interno. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(SubscriptionExpiring event) {
    publish("subscription.expiring", event);
  }

  /** Notifications entrega el comprobante del pago confirmado por el canal de correo simulado. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(PaymentReceiptRequested event) {
    publish("subscription.payment-receipt", event);
  }

  private void publish(String routingKey, IntegrationEvent event) {
    CorrelationData correlation = new CorrelationData(event.eventId().toString());
    try {
      rabbit.convertAndSend(exchange, routingKey, event, message -> {
        message.getMessageProperties().setMessageId(event.eventId().toString());
        message.getMessageProperties().setType(event.type());
        return message;
      }, correlation);
      var confirmation = correlation.getFuture().get(5, TimeUnit.SECONDS);
      if (!confirmation.isAck() || correlation.getReturned() != null) {
        throw new AmqpException("El broker rechazó el evento o no encontró una cola receptora.");
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Se interrumpió la publicación de {} ({}). Requiere conciliación.",
          event.type(), event.eventId(), ex);
    } catch (Exception ex) {
      log.error("No se pudo publicar {} ({}). La transacción ya terminó; requiere conciliación.",
          event.type(), event.eventId(), ex);
    }
  }
}
