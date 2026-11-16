package com.nexus.rinde.settlement.interfaces.internal;

import com.nexus.rinde.settlement.interfaces.acl.SettlementContextFacade;
import com.nexus.rinde.settlement.interfaces.acl.SettlementSummary;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos consumidos por el Dashboard del monolito. No expuestos por el gateway.
 */
@RestController
@RequestMapping(
    value = "/api/internal/v1/settlements/tenants/{tenantId}",
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Internal", description = "Consultas entre servicios de RINDE (no públicas)")
@SecurityRequirements
public class InternalSettlementController {

  private final SettlementContextFacade settlementFacade;

  public InternalSettlementController(SettlementContextFacade settlementFacade) {
    this.settlementFacade = settlementFacade;
  }

  @GetMapping("/trips/{tripId}")
  @Operation(summary = "Liquidación de un viaje de la empresa")
  public SettlementSummary byTrip(@PathVariable UUID tenantId, @PathVariable UUID tripId) {
    return settlementFacade
        .findByTripIdAndTenantId(tripId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("No existe liquidación para el viaje."));
  }

  @GetMapping("/open/count")
  @Operation(summary = "Cantidad de liquidaciones abiertas de la empresa")
  public OpenSettlementCount openCount(@PathVariable UUID tenantId) {
    return new OpenSettlementCount(settlementFacade.countOpenByTenantId(tenantId));
  }

  public record OpenSettlementCount(long count) {}
}
