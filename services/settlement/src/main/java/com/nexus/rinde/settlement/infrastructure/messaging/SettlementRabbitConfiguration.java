package com.nexus.rinde.settlement.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuración RabbitMQ del servicio Settlement: exchange compartido y conversión JSON. */
@Configuration
public class SettlementRabbitConfiguration {

  @Bean
  public TopicExchange rindeEventsExchange(
      @Value("${rinde.messaging.exchange:rinde.events}") String name) {
    return new TopicExchange(name, true, false);
  }

  @Bean
  public MessageConverter jsonMessageConverter(ObjectMapper mapper) {
    var converter = new Jackson2JsonMessageConverter(mapper);
    converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
    return converter;
  }
}
