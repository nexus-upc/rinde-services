package com.nexus.rinde.settlement.interfaces.acl;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.aggregates.TripAdvance;
import com.nexus.rinde.settlement.domain.model.valueobjects.AdvanceDeliveryMethod;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.SettlementRepository;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.TripAdvanceRepository;
import com.nexus.rinde.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Pruebas de integración para la fachada local de liquidaciones y adelantos. */
class SettlementContextFacadeImplTest extends AbstractIntegrationTest {

  @Autowired private SettlementContextFacade settlementFacade;
  @Autowired private SettlementRepository settlementRepository;
  @Autowired private TripAdvanceRepository tripAdvanceRepository;

  @Test
  @DisplayName("Expone el id, cierre y el medio del último adelanto aunque no haya liquidación")
  void returnsLatestAdvanceMethodAndSettlementDetails() {
    UUID tenantId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    tripAdvanceRepository.saveAndFlush(
        TripAdvance.register(
            tenantId,
            tripId,
            new BigDecimal("200.00"),
            "PEN",
            AdvanceDeliveryMethod.CASH,
            userId));
    tripAdvanceRepository.saveAndFlush(
        TripAdvance.register(
            tenantId,
            tripId,
            new BigDecimal("300.00"),
            "PEN",
            AdvanceDeliveryMethod.TRANSFER,
            userId));

    assertThat(settlementFacade.findLatestAdvanceDeliveryMethod(tripId, tenantId))
        .contains("TRANSFER");
    assertThat(settlementFacade.findByTripIdAndTenantId(tripId, tenantId)).isEmpty();

    Settlement settlement = Settlement.create(tenantId, tripId, driverId);
    Instant closedAt = Instant.parse("2026-11-20T18:00:00Z");
    settlement.close(List.of(), 0, userId, closedAt);
    settlementRepository.saveAndFlush(settlement);

    assertThat(settlementFacade.findByTripIdAndTenantId(tripId, tenantId))
        .hasValueSatisfying(
            summary -> {
              assertThat(summary.id()).isEqualTo(settlement.getId());
              assertThat(summary.status()).isEqualTo("CLOSED");
              assertThat(summary.closedAt()).isEqualTo(closedAt);
            });
  }
}
