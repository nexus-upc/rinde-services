package com.nexus.rinde.settlement.interfaces.internal;

import com.nexus.rinde.settlement.domain.model.valueobjects.SettlementStatus;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.SettlementRepository;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Contratos HTTP de Settlement para consumo interno entre servicios de RINDE (no públicos). */
@RestController
@RequestMapping("/api/internal/v1/settlements/tenants/{tenantId}")
@Tag(name = "Internal", description = "Consultas entre servicios de RINDE (no públicas)")
public class InternalSettlementController {

  private final SettlementRepository settlementRepository;

  public InternalSettlementController(SettlementRepository settlementRepository) {
    this.settlementRepository = settlementRepository;
  }

  @Operation(summary = "Resumen de la liquidación de un viaje")
  @GetMapping("/trips/{tripId}")
  public SettlementSummaryResponse getByTrip(
      @PathVariable UUID tenantId, @PathVariable UUID tripId) {
    return settlementRepository
        .findByTripIdAndTenantId(tripId, tenantId)
        .map(
            s ->
                new SettlementSummaryResponse(
                    s.getStatus().name(), s.getAdvanceAmount(), s.getAdvanceBalance()))
        .orElseThrow(() -> new ResourceNotFoundException("No existe liquidación para este viaje."));
  }

  @Operation(summary = "Conteo de liquidaciones abiertas de una empresa")
  @GetMapping("/pending")
  public PendingCountResponse countPending(@PathVariable UUID tenantId) {
    long count = settlementRepository.countByTenantIdAndStatus(tenantId, SettlementStatus.OPEN);
    return new PendingCountResponse(count);
  }

  public record SettlementSummaryResponse(
      String status, BigDecimal advanceAmount, BigDecimal advanceBalance) {}

  public record PendingCountResponse(long count) {}
}
