package com.nexus.rinde.settlement.application.internal.commandservices;

import com.nexus.rinde.settlement.application.internal.outboundservices.acl.ExternalExpenseService;
import com.nexus.rinde.settlement.application.internal.outboundservices.acl.ExternalTripService;
import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.aggregates.TripAdvance;
import com.nexus.rinde.settlement.domain.model.commands.CloseSettlementCommand;
import com.nexus.rinde.settlement.domain.model.commands.RecalculateSettlementCommand;
import com.nexus.rinde.settlement.domain.model.commands.RegisterTripAdvanceCommand;
import com.nexus.rinde.settlement.domain.model.valueobjects.SettlementRecalculation;
import com.nexus.rinde.settlement.domain.model.valueobjects.TripExpenseReview;
import com.nexus.rinde.settlement.domain.services.SettlementCommandService;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.SettlementRepository;
import com.nexus.rinde.settlement.infrastructure.persistence.jpa.repositories.TripAdvanceRepository;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordina los comandos de adelantos y liquidación con su persistencia. */
@Service
public class SettlementCommandServiceImpl implements SettlementCommandService {

  /** Estados del viaje en los que todavía se puede entregar un adelanto. */
  private static final Set<String> ADVANCE_TRIP_STATUSES = Set.of("SCHEDULED", "ASSIGNED");

  private final SettlementRepository settlementRepository;
  private final TripAdvanceRepository tripAdvanceRepository;
  private final ExternalTripService externalTripService;
  private final ExternalExpenseService externalExpenseService;
  private final Clock clock;

  public SettlementCommandServiceImpl(
      SettlementRepository settlementRepository,
      TripAdvanceRepository tripAdvanceRepository,
      ExternalTripService externalTripService,
      ExternalExpenseService externalExpenseService,
      Clock clock) {
    this.settlementRepository = settlementRepository;
    this.tripAdvanceRepository = tripAdvanceRepository;
    this.externalTripService = externalTripService;
    this.externalExpenseService = externalExpenseService;
    this.clock = clock;
  }

  @Override
  @Transactional
  public TripAdvance handle(RegisterTripAdvanceCommand command) {
    String status =
        externalTripService
            .findStatusByTripId(command.tenantId(), command.tripId())
            .orElseThrow(() -> new ResourceNotFoundException("El viaje no existe."));
    if (!ADVANCE_TRIP_STATUSES.contains(status)) {
      throw new ConflictException(
          "Solo se puede registrar el adelanto de un viaje programado o asignado.");
    }
    if (settlementRepository.existsByTripIdAndTenantId(command.tripId(), command.tenantId())) {
      throw new ConflictException(
          "La liquidación del viaje ya fue abierta; no se pueden registrar más adelantos.");
    }

    String currency = TripAdvance.normalizeCurrency(command.currency());
    boolean otherCurrency =
        tripAdvanceRepository
            .findByTenantIdAndTripIdOrderByCreatedAtAsc(command.tenantId(), command.tripId())
            .stream()
            .anyMatch(advance -> !advance.getCurrency().equals(currency));
    if (otherCurrency) {
      throw new ConflictException("Los adelantos de un viaje deben registrarse en una sola moneda.");
    }

    TripAdvance advance =
        TripAdvance.register(
            command.tenantId(),
            command.tripId(),
            command.amount(),
            currency,
            command.deliveryMethod(),
            command.registeredBy());
    return tripAdvanceRepository.saveAndFlush(advance);
  }

  @Override
  @Transactional
  public SettlementRecalculation handle(RecalculateSettlementCommand command) {
    Settlement settlement = loadSettlement(command.settlementId(), command.tenantId());
    TripExpenseReview review =
        externalExpenseService.reviewByTripId(command.tenantId(), settlement.getTripId());
    settlement.recalculate(review.approved());
    Settlement saved = settlementRepository.saveAndFlush(settlement);
    return new SettlementRecalculation(saved, review.pendingCount());
  }

  @Override
  @Transactional
  public Settlement handle(CloseSettlementCommand command) {
    Settlement settlement = loadSettlement(command.settlementId(), command.tenantId());
    TripExpenseReview review =
        externalExpenseService.reviewByTripId(command.tenantId(), settlement.getTripId());
    settlement.close(
        review.approved(), review.pendingCount(), command.closedBy(), clock.instant());
    return settlementRepository.saveAndFlush(settlement);
  }

  private Settlement loadSettlement(UUID settlementId, UUID tenantId) {
    return settlementRepository
        .findByIdAndTenantId(settlementId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("La liquidación no existe."));
  }
}
