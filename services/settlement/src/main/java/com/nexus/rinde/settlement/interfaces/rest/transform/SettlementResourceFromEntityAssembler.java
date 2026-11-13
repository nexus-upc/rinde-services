package com.nexus.rinde.settlement.interfaces.rest.transform;

import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.interfaces.rest.resources.SettlementResource;

/** Convierte un agregado Settlement en su representación REST. */
public class SettlementResourceFromEntityAssembler {

  private SettlementResourceFromEntityAssembler() {}

  public static SettlementResource toResource(Settlement settlement) {
    return new SettlementResource(
        settlement.getId(),
        settlement.getTenantId(),
        settlement.getTripId(),
        settlement.getDriverId(),
        settlement.getStatus().name(),
        settlement.getAdvanceAmount(),
        settlement.getAdvanceCurrency(),
        settlement.getExpenseTotalAmount(),
        settlement.getExpenseTotalCurrency(),
        settlement.getAdvanceBalance(),
        settlement.getCreatedAt(),
        settlement.getUpdatedAt());
  }
}
