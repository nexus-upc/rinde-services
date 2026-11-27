package com.nexus.rinde.settlement.infrastructure.messaging;

import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.aggregates.TripAdvance;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.SettlementRepository;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.TripAdvanceRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre la liquidación cuando trip.finished llega por RabbitMQ e incorpora los adelantos del viaje;
 * idempotente por eventId.
 */
@Component
public class SettlementTripMessaging {

  private final SettlementRepository settlementRepository;
  private final TripAdvanceRepository tripAdvanceRepository;
  private final JdbcTemplate jdbc;

  public SettlementTripMessaging(
      SettlementRepository settlementRepository,
      TripAdvanceRepository tripAdvanceRepository,
      JdbcTemplate jdbc) {
    this.settlementRepository = settlementRepository;
    this.tripAdvanceRepository = tripAdvanceRepository;
    this.jdbc = jdbc;
  }

  @RabbitListener(
      queues =
          "${rinde.messaging.settlement.queue:${rinde.messaging.exchange:rinde.events}.settlement.trip-events}")
  @Transactional
  public void receive(Event event) {
    if (event.eventId() == null || event.tenantId() == null || event.tripId() == null)
      throw new IllegalArgumentException("Evento de viaje inválido.");

    int inserted =
        jdbc.update(
            "insert into settlement.processed_trip_events(event_id,processed_at) values(?,?)"
                + " on conflict do nothing",
            event.eventId(),
            Timestamp.from(Instant.now()));
    if (inserted == 0) return;

    if (settlementRepository.existsByTripId(event.tripId())) return;

    Settlement settlement = Settlement.create(event.tenantId(), event.tripId(), event.driverId());
    List<TripAdvance> advances =
        tripAdvanceRepository.findByTenantIdAndTripIdOrderByCreatedAtAsc(
            event.tenantId(), event.tripId());
    if (!advances.isEmpty()) {
      BigDecimal total =
          advances.stream()
              .map(TripAdvance::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      settlement.incorporateAdvance(total, advances.get(0).getCurrency());
    }
    settlementRepository.save(settlement);
  }

  public record Event(UUID eventId, UUID tenantId, UUID tripId, UUID driverId) {}

  @Configuration
  public static class Topology {

    @Bean
    public Declarables settlementTripBindings(
        @Value("${rinde.messaging.exchange:rinde.events}") String exchange,
        @Value(
                "${rinde.messaging.settlement.queue:${rinde.messaging.exchange:rinde.events}.settlement.trip-events}")
            String name) {
      var topic = new TopicExchange(exchange, true, false);
      var queue =
          QueueBuilder.durable(name)
              .deadLetterExchange(exchange + ".settlement.dead")
              .deadLetterRoutingKey(name)
              .build();
      var dead = new DirectExchange(exchange + ".settlement.dead", true, false);
      var deadQueue = QueueBuilder.durable(name + ".dead").build();
      return new Declarables(
          topic,
          queue,
          dead,
          deadQueue,
          BindingBuilder.bind(queue).to(topic).with("trip.finished"),
          BindingBuilder.bind(deadQueue).to(dead).with(name));
    }
  }
}
