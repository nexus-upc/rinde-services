package com.nexus.rinde.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declara la cola durable y conserva los mensajes rechazados en una cola de errores. */
@Configuration
public class RabbitMessagingConfiguration {

  public static final String EVENTS_EXCHANGE = "rinde.events";
  public static final String DEAD_EXCHANGE = "rinde.notification.dead";

  @Bean
  public TopicExchange rindeEventsExchange(
      @Value("${rinde.messaging.exchange:rinde.events}") String name) {
    return new TopicExchange(name, true, false);
  }

  @Bean
  public DirectExchange notificationDeadExchange() {
    return new DirectExchange(DEAD_EXCHANGE, true, false);
  }

  @Bean
  public Queue notificationTripAssignedQueue(
      @Value("${rinde.messaging.notification.queue:notification.trip-assigned}") String name) {
    return QueueBuilder.durable(name)
        .deadLetterExchange(DEAD_EXCHANGE).deadLetterRoutingKey(name).build();
  }

  @Bean
  public Queue notificationDeadQueue(
      @Value("${rinde.messaging.notification.queue:notification.trip-assigned}") String name) {
    return QueueBuilder.durable(name + ".dead").build();
  }

  @Bean
  public Binding notificationTripAssignedBinding(
      @Qualifier("notificationTripAssignedQueue") Queue queue, TopicExchange rindeEventsExchange) {
    return BindingBuilder.bind(queue).to(rindeEventsExchange).with("trip.assigned");
  }

  @Bean
  public Binding notificationDeadBinding(
      @Qualifier("notificationDeadQueue") Queue queue, DirectExchange notificationDeadExchange,
      @Value("${rinde.messaging.notification.queue:notification.trip-assigned}") String name) {
    return BindingBuilder.bind(queue).to(notificationDeadExchange).with(name);
  }

  // Gastos registrados: cola, cola de errores y bindings propios.

  @Bean
  public Queue notificationExpenseRegisteredQueue(
      @Value("${rinde.messaging.notification.expense-queue:notification.expense-registered}")
          String name) {
    return QueueBuilder.durable(name)
        .deadLetterExchange(DEAD_EXCHANGE).deadLetterRoutingKey(name).build();
  }

  @Bean
  public Queue notificationExpenseRegisteredDeadQueue(
      @Value("${rinde.messaging.notification.expense-queue:notification.expense-registered}")
          String name) {
    return QueueBuilder.durable(name + ".dead").build();
  }

  @Bean
  public Binding notificationExpenseRegisteredBinding(
      @Qualifier("notificationExpenseRegisteredQueue") Queue queue,
      TopicExchange rindeEventsExchange) {
    return BindingBuilder.bind(queue).to(rindeEventsExchange).with("expense.registered");
  }

  @Bean
  public Binding notificationExpenseRegisteredDeadBinding(
      @Qualifier("notificationExpenseRegisteredDeadQueue") Queue queue,
      DirectExchange notificationDeadExchange,
      @Value("${rinde.messaging.notification.expense-queue:notification.expense-registered}")
          String name) {
    return BindingBuilder.bind(queue).to(notificationDeadExchange).with(name);
  }

  // Incidencias reportadas: cola, cola de errores y bindings propios.

  @Bean
  public Queue notificationIncidentReportedQueue(
      @Value("${rinde.messaging.notification.incident-queue:notification.incident-reported}")
          String name) {
    return QueueBuilder.durable(name)
        .deadLetterExchange(DEAD_EXCHANGE).deadLetterRoutingKey(name).build();
  }

  @Bean
  public Queue notificationIncidentReportedDeadQueue(
      @Value("${rinde.messaging.notification.incident-queue:notification.incident-reported}")
          String name) {
    return QueueBuilder.durable(name + ".dead").build();
  }

  @Bean
  public Binding notificationIncidentReportedBinding(
      @Qualifier("notificationIncidentReportedQueue") Queue queue,
      TopicExchange rindeEventsExchange) {
    return BindingBuilder.bind(queue).to(rindeEventsExchange).with("incident.reported");
  }

  @Bean
  public Binding notificationIncidentReportedDeadBinding(
      @Qualifier("notificationIncidentReportedDeadQueue") Queue queue,
      DirectExchange notificationDeadExchange,
      @Value("${rinde.messaging.notification.incident-queue:notification.incident-reported}")
          String name) {
    return BindingBuilder.bind(queue).to(notificationDeadExchange).with(name);
  }

  // Viajes finalizados: cola, cola de errores y bindings propios.

  @Bean
  public Queue notificationTripFinishedQueue(
      @Value("${rinde.messaging.notification.trip-finished-queue:notification.trip-finished}")
          String name) {
    return QueueBuilder.durable(name)
        .deadLetterExchange(DEAD_EXCHANGE).deadLetterRoutingKey(name).build();
  }

  @Bean
  public Queue notificationTripFinishedDeadQueue(
      @Value("${rinde.messaging.notification.trip-finished-queue:notification.trip-finished}")
          String name) {
    return QueueBuilder.durable(name + ".dead").build();
  }

  @Bean
  public Binding notificationTripFinishedBinding(
      @Qualifier("notificationTripFinishedQueue") Queue queue, TopicExchange rindeEventsExchange) {
    return BindingBuilder.bind(queue).to(rindeEventsExchange).with("trip.finished");
  }

  @Bean
  public Binding notificationTripFinishedDeadBinding(
      @Qualifier("notificationTripFinishedDeadQueue") Queue queue,
      DirectExchange notificationDeadExchange,
      @Value("${rinde.messaging.notification.trip-finished-queue:notification.trip-finished}")
          String name) {
    return BindingBuilder.bind(queue).to(notificationDeadExchange).with(name);
  }

  @Bean
  public MessageConverter jsonMessageConverter(ObjectMapper mapper) {
    var converter = new Jackson2JsonMessageConverter(mapper);
    converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
    return converter;
  }
}
