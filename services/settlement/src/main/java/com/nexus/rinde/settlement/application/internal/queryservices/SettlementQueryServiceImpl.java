package com.nexus.rinde.settlement.application.internal.queryservices;

import com.nexus.rinde.settlement.application.internal.outboundservices.acl.ExternalExpenseService;
import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.queries.ExportSettlementQuery;
import com.nexus.rinde.settlement.domain.model.queries.GetSettlementByIdQuery;
import com.nexus.rinde.settlement.domain.model.queries.GetSettlementByTripIdQuery;
import com.nexus.rinde.settlement.domain.model.queries.SettlementExport;
import com.nexus.rinde.settlement.domain.model.valueobjects.ApprovedExpense;
import com.nexus.rinde.settlement.domain.services.SettlementQueryService;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.SettlementRepository;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atiende consultas sobre las liquidaciones registradas. */
@Service
public class SettlementQueryServiceImpl implements SettlementQueryService {

  private final SettlementRepository settlementRepository;
  private final ExternalExpenseService externalExpenseService;

  public SettlementQueryServiceImpl(
      SettlementRepository settlementRepository, ExternalExpenseService externalExpenseService) {
    this.settlementRepository = settlementRepository;
    this.externalExpenseService = externalExpenseService;
  }

  @Override
  @Transactional(readOnly = true)
  public Settlement handle(GetSettlementByIdQuery query) {
    return settlementRepository
        .findByIdAndTenantId(query.settlementId(), query.tenantId())
        .orElseThrow(() -> new ResourceNotFoundException("La liquidación no existe."));
  }

  @Override
  @Transactional(readOnly = true)
  public Settlement handle(GetSettlementByTripIdQuery query) {
    return settlementRepository
        .findByTripIdAndTenantId(query.tripId(), query.tenantId())
        .orElseThrow(() -> new ResourceNotFoundException("No existe liquidación para este viaje."));
  }

  @Override
  @Transactional(readOnly = true)
  public SettlementExport handle(ExportSettlementQuery query) {
    Settlement settlement =
        settlementRepository
            .findByIdAndTenantId(query.settlementId(), query.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("La liquidación no existe."));
    List<ApprovedExpense> approved =
        externalExpenseService.findApprovedExpensesByTripId(
            query.tenantId(), settlement.getTripId());
    return new SettlementExport(settlement, approved);
  }
}
