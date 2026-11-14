package com.nexus.rinde.subscription.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nexus.rinde.subscription.interfaces.acl.PaymentReceiptRequested;
import com.nexus.rinde.subscription.interfaces.acl.SubscriptionActivated;
import com.nexus.rinde.subscription.interfaces.acl.SubscriptionExpiring;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Los fallos posteriores al commit quedan identificados para conciliación, sin simular un rollback. */
@ExtendWith(OutputCaptureExtension.class)
class SubscriptionEventPublisherTest {

  private static final String EXCHANGE = "test.subscription.events";

  private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
  private final SubscriptionEventPublisher publisher =
      new SubscriptionEventPublisher(rabbit, new TopicExchange(EXCHANGE));

  @Test
  void recordsTheEventIdWhenPublishingFails(CapturedOutput output) {
    doThrow(new AmqpException("Broker no disponible."))
        .when(rabbit).convertAndSend(anyString(), anyString(), any(),
            any(MessagePostProcessor.class), any(CorrelationData.class));
    SubscriptionActivated event = event();

    assertThatCode(() -> publisher.on(event)).doesNotThrowAnyException();
    assertThat(output.getOut()).contains(event.eventId().toString(), "requiere conciliación");
  }

  @Test
  void recordsANegativeBrokerConfirmation(CapturedOutput output) {
    doAnswer(invocation -> {
      CorrelationData correlation = invocation.getArgument(4);
      correlation.getFuture().complete(new CorrelationData.Confirm(false, "Rechazado"));
      return null;
    }).when(rabbit).convertAndSend(anyString(), anyString(), any(),
        any(MessagePostProcessor.class), any(CorrelationData.class));
    SubscriptionActivated event = event();

    publisher.on(event);

    assertThat(output.getOut()).contains(event.eventId().toString(), "rechazó el evento");
  }

  @Test
  void publishesTheExpiryNoticeWithItsRoutingKey(CapturedOutput output) {
    confirmEveryPublication();
    SubscriptionExpiring event =
        new SubscriptionExpiring(
            UUID.randomUUID(),
            Instant.now(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now().plus(Duration.ofDays(5)));

    publisher.on(event);

    assertPublished("subscription.expiring", event, event.eventId(), "SubscriptionExpiring");
    assertThat(output.getOut()).doesNotContain("requiere conciliación");
  }

  @Test
  void publishesThePaymentReceiptWithItsRoutingKey(CapturedOutput output) {
    confirmEveryPublication();
    PaymentReceiptRequested event =
        new PaymentReceiptRequested(
            UUID.randomUUID(),
            Instant.now(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "sim_" + UUID.randomUUID(),
            new BigDecimal("37.50"),
            Instant.now(),
            Instant.now().plus(Duration.ofDays(30)));

    publisher.on(event);

    assertPublished(
        "subscription.payment-receipt", event, event.eventId(), "PaymentReceiptRequested");
    assertThat(output.getOut()).doesNotContain("requiere conciliación");
  }

  private void confirmEveryPublication() {
    doAnswer(invocation -> {
      CorrelationData correlation = invocation.getArgument(4);
      correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
      return null;
    }).when(rabbit).convertAndSend(anyString(), anyString(), any(),
        any(MessagePostProcessor.class), any(CorrelationData.class));
  }

  /** Comprueba la ruta, el evento enviado y las propiedades del mensaje (id y tipo). */
  private void assertPublished(String routingKey, Object event, UUID eventId, String type) {
    ArgumentCaptor<MessagePostProcessor> processor =
        ArgumentCaptor.forClass(MessagePostProcessor.class);
    verify(rabbit).convertAndSend(eq(EXCHANGE), eq(routingKey), eq(event), processor.capture(),
        any(CorrelationData.class));
    Message message = processor.getValue().postProcessMessage(new Message(new byte[0]));
    assertThat(message.getMessageProperties().getMessageId()).isEqualTo(eventId.toString());
    assertThat(message.getMessageProperties().getType()).isEqualTo(type);
  }

  private SubscriptionActivated event() {
    return new SubscriptionActivated(UUID.randomUUID(), Instant.now(), UUID.randomUUID());
  }
}
