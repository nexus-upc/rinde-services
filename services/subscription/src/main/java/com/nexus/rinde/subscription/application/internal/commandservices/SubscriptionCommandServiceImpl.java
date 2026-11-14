package com.nexus.rinde.subscription.application.internal.commandservices;

import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import com.nexus.rinde.subscription.domain.model.aggregates.Plan;
import com.nexus.rinde.subscription.domain.model.aggregates.Subscription;
import com.nexus.rinde.subscription.domain.model.commands.ChoosePlanCommand;
import com.nexus.rinde.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.nexus.rinde.subscription.domain.services.FleetUnitCounter;
import com.nexus.rinde.subscription.domain.services.SubscriptionCommandService;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.nexus.rinde.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Valida la selección del plan frente al catálogo y la cantidad informada por Fleet. */
@Service
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

  private final PlanRepository planRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final FleetUnitCounter fleetUnitCounter;

  public SubscriptionCommandServiceImpl(
      PlanRepository planRepository,
      SubscriptionRepository subscriptionRepository,
      FleetUnitCounter fleetUnitCounter) {
    this.planRepository = planRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.fleetUnitCounter = fleetUnitCounter;
  }

  @Override
  @Transactional
  public Subscription handle(ChoosePlanCommand command) {
    Plan plan =
        planRepository
            .findByPlanIdAndActiveTrue(command.planId())
            .orElseThrow(() -> new ResourceNotFoundException("El plan no existe o no está activo."));

    long registeredUnits = fleetUnitCounter.countVehicles(command.tenantId());
    long excessUnits = registeredUnits - plan.getUnitLimit();
    if (excessUnits > 0) {
      String unitWord = excessUnits == 1 ? "unidad" : "unidades";
      throw new ConflictException(
          "El plan admite hasta "
              + plan.getUnitLimit()
              + " unidades y la flota excede el límite por "
              + excessUnits
              + " "
              + unitWord
              + ".");
    }

    if (subscriptionRepository.existsByTenantIdAndStatus(
        command.tenantId(), SubscriptionStatus.PENDING_PAYMENT)) {
      throw new ConflictException("La empresa ya tiene una suscripción pendiente de pago.");
    }

    return subscriptionRepository.saveAndFlush(
        Subscription.pendingPayment(command.tenantId(), plan.getPlanId()));
  }
}
