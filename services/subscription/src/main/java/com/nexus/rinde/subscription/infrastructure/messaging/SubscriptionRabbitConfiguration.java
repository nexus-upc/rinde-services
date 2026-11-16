package com.nexus.rinde.subscription.infrastructure.messaging;

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

/** Declara también la cola receptora para conservar eventos aunque IAM todavía no haya arrancado. */
@Configuration
public class SubscriptionRabbitConfiguration {

  /** Exchange durable compartido por todos los servicios; su nombre viene de rinde.messaging.exchange. */
  @Bean
  public TopicExchange rindeEventsExchange(
      @Value("${rinde.messaging.exchange:rinde.events}") String name) {
    return new TopicExchange(name, true, false);
  }

  /** Lee y escribe los eventos en JSON; en la lectura manda el tipo del parámetro, no la cabecera. */
  @Bean
  public MessageConverter jsonMessageConverter(ObjectMapper mapper) {
    var converter = new Jackson2JsonMessageConverter(mapper);
    converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
    return converter;
  }

  @Bean
  public DirectExchange iamDeadExchange() {
    return new DirectExchange("rinde.iam.dead", true, false);
  }

  @Bean
  public Queue iamSubscriptionQueue(
      @Value("${rinde.messaging.subscription.queue:iam.subscription-events}") String name) {
    return QueueBuilder.durable(name).deadLetterExchange("rinde.iam.dead")
        .deadLetterRoutingKey(name).build();
  }

  @Bean
  public Queue iamSubscriptionDeadQueue(
      @Value("${rinde.messaging.subscription.queue:iam.subscription-events}") String name) {
    return QueueBuilder.durable(name + ".dead").build();
  }

  @Bean
  public Binding subscriptionActivatedBinding(@Qualifier("iamSubscriptionQueue") Queue queue,
      TopicExchange rindeEventsExchange) {
    return BindingBuilder.bind(queue).to(rindeEventsExchange).with("subscription.activated");
  }

  @Bean
  public Binding subscriptionSuspendedBinding(@Qualifier("iamSubscriptionQueue") Queue queue,
      TopicExchange rindeEventsExchange) {
    return BindingBuilder.bind(queue).to(rindeEventsExchange).with("subscription.suspended");
  }

  @Bean
  public Binding subscriptionDeadBinding(@Qualifier("iamSubscriptionDeadQueue") Queue queue,
      DirectExchange iamDeadExchange,
      @Value("${rinde.messaging.subscription.queue:iam.subscription-events}") String name) {
    return BindingBuilder.bind(queue).to(iamDeadExchange).with(name);
  }
}
