package com.nexus.rinde.settlement.application.internal.commandservices;

import com.nexus.rinde.settlement.application.internal.outboundservices.acl.ExternalExpenseService;
import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.commands.CloseSettlementCommand;
import com.nexus.rinde.settlement.domain.model.commands.RecalculateSettlementCommand;
import com.nexus.rinde.settlement.domain.model.commands.RegisterAdvanceCommand;
import com.nexus.rinde.settlement.domain.model.valueobjects.ApprovedExpense;
import com.nexus.rinde.settlement.domain.services.SettlementCommandService;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.SettlementRepository;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordina los comandos de liquidación con su persistencia. */
@Service
public class SettlementCommandServiceImpl implements SettlementCommandService {

  private final SettlementRepository settlementRepository;
  private final ExternalExpenseService externalExpenseService;
  private final Clock clock;

  public SettlementCommandServiceImpl(
      SettlementRepository settlementRepository,
      ExternalExpenseService externalExpenseService,
      Clock clock) {
    this.settlementRepository = settlementRepository;
    this.externalExpenseService = externalExpenseService;
    this.clock = clock;
  }

  @Override
  @Transactional
  public Settlement handle(RegisterAdvanceCommand command) {
    Settlement settlement =
        settlementRepository
            .findByTripIdAndTenantId(command.tripId(), command.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("No existe liquidación para este viaje."));

    settlement.registerAdvance(command.amount(), command.currency(), clock.instant());
    return settlementRepository.saveAndFlush(settlement);
  }

  @Override
  @Transactional
  public Settlement handle(RecalculateSettlementCommand command) {
    Settlement settlement = loadSettlement(command.settlementId(), command.tenantId());
    List<ApprovedExpense> approved =
        externalExpenseService.findApprovedExpensesByTripId(
            command.tenantId(), settlement.getTripId());
    settlement.recalculate(approved);
    return settlementRepository.saveAndFlush(settlement);
  }

  @Override
  @Transactional
  public Settlement handle(CloseSettlementCommand command) {
    Settlement settlement = loadSettlement(command.settlementId(), command.tenantId());
    List<ApprovedExpense> approved =
        externalExpenseService.findApprovedExpensesByTripId(
            command.tenantId(), settlement.getTripId());
    settlement.close(approved, command.closedBy(), clock.instant());
    return settlementRepository.saveAndFlush(settlement);
  }

  private Settlement loadSettlement(java.util.UUID settlementId, java.util.UUID tenantId) {
    return settlementRepository
        .findByIdAndTenantId(settlementId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("La liquidación no existe."));
  }
}
