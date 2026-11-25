package com.nexus.rinde.settlement.application.internal.queryservices;

import com.nexus.rinde.settlement.application.internal.outboundservices.acl.ExternalExpenseService;
import com.nexus.rinde.settlement.application.internal.outboundservices.acl.ExternalFleetService;
import com.nexus.rinde.settlement.application.internal.outboundservices.acl.ExternalTripService;
import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.aggregates.TripAdvance;
import com.nexus.rinde.settlement.domain.model.queries.ExportSettlementQuery;
import com.nexus.rinde.settlement.domain.model.queries.GetSettlementByIdQuery;
import com.nexus.rinde.settlement.domain.model.queries.GetSettlementByTripIdQuery;
import com.nexus.rinde.settlement.domain.model.queries.GetTripAdvancesQuery;
import com.nexus.rinde.settlement.domain.model.queries.SettlementExport;
import com.nexus.rinde.settlement.domain.model.valueobjects.ApprovedExpense;
import com.nexus.rinde.settlement.domain.services.SettlementQueryService;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.SettlementRepository;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.TripAdvanceRepository;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atiende consultas sobre las liquidaciones y los adelantos registrados. */
@Service
public class SettlementQueryServiceImpl implements SettlementQueryService {

  private static final String TRIP_NOT_FOUND_MESSAGE = "El viaje no existe.";
  private static final String SETTLEMENT_NOT_FOUND_MESSAGE = "La liquidación no existe.";

  private final SettlementRepository settlementRepository;
  private final TripAdvanceRepository tripAdvanceRepository;
  private final ExternalTripService externalTripService;
  private final ExternalFleetService externalFleetService;
  private final ExternalExpenseService externalExpenseService;

  public SettlementQueryServiceImpl(
      SettlementRepository settlementRepository,
      TripAdvanceRepository tripAdvanceRepository,
      ExternalTripService externalTripService,
      ExternalFleetService externalFleetService,
      ExternalExpenseService externalExpenseService) {
    this.settlementRepository = settlementRepository;
    this.tripAdvanceRepository = tripAdvanceRepository;
    this.externalTripService = externalTripService;
    this.externalFleetService = externalFleetService;
    this.externalExpenseService = externalExpenseService;
  }

  @Override
  @Transactional(readOnly = true)
  public Settlement handle(GetSettlementByIdQuery query) {
    Settlement settlement =
        settlementRepository
            .findByIdAndTenantId(query.settlementId(), query.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException(SETTLEMENT_NOT_FOUND_MESSAGE));
    requireAssignedDriverIfDriver(
        query.tenantId(),
        settlement.getTripId(),
        query.requesterUserId(),
        query.requesterRole(),
        SETTLEMENT_NOT_FOUND_MESSAGE);
    return settlement;
  }

  @Override
  @Transactional(readOnly = true)
  public Settlement handle(GetSettlementByTripIdQuery query) {
    requireAssignedDriverIfDriver(
        query.tenantId(), query.tripId(), query.requesterUserId(), query.requesterRole());
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
            .orElseThrow(() -> new ResourceNotFoundException(SETTLEMENT_NOT_FOUND_MESSAGE));
    List<ApprovedExpense> approved =
        externalExpenseService.findApprovedExpensesByTripId(
            query.tenantId(), settlement.getTripId());
    return new SettlementExport(settlement, approved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TripAdvance> handle(GetTripAdvancesQuery query) {
    if ("DRIVER".equals(query.requesterRole())) {
      requireAssignedDriverIfDriver(
          query.tenantId(), query.tripId(), query.requesterUserId(), query.requesterRole());
    } else {
      externalTripService
          .findStatusByTripId(query.tenantId(), query.tripId())
          .orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND_MESSAGE));
    }
    return tripAdvanceRepository.findByTenantIdAndTripIdOrderByCreatedAtAsc(
        query.tenantId(), query.tripId());
  }

  /**
   * Comprueba que el conductor solicitante tenga asignado el viaje; de lo contrario responde 404
   * para no revelar si el viaje existe.
   */
  private void requireAssignedDriverIfDriver(
      UUID tenantId, UUID tripId, UUID requesterUserId, String requesterRole) {
    requireAssignedDriverIfDriver(
        tenantId, tripId, requesterUserId, requesterRole, TRIP_NOT_FOUND_MESSAGE);
  }

  private void requireAssignedDriverIfDriver(
      UUID tenantId,
      UUID tripId,
      UUID requesterUserId,
      String requesterRole,
      String notFoundMessage) {
    if (!"DRIVER".equals(requesterRole)) {
      return;
    }
    UUID tripDriverId =
        externalTripService
            .findDriverIdByTripId(tenantId, tripId)
            .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
    UUID requesterDriverId =
        externalFleetService
            .findDriverIdByUserId(tenantId, requesterUserId)
            .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
    if (!requesterDriverId.equals(tripDriverId)) {
      throw new ResourceNotFoundException(notFoundMessage);
    }
  }
}
