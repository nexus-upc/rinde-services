package com.nexus.rinde.settlement.interfaces.rest.transform;

import com.nexus.rinde.settlement.domain.model.aggregates.TripAdvance;
import com.nexus.rinde.settlement.interfaces.rest.resources.TripAdvanceResource;

/** Convierte un adelanto en su representación REST. */
public class TripAdvanceResourceFromEntityAssembler {

  private TripAdvanceResourceFromEntityAssembler() {}

  public static TripAdvanceResource toResource(TripAdvance advance) {
    return new TripAdvanceResource(
        advance.getId(),
        advance.getTripId(),
        advance.getAmount(),
        advance.getCurrency(),
        advance.getDeliveryMethod().name(),
        advance.getRegisteredBy(),
        advance.getCreatedAt());
  }
}
