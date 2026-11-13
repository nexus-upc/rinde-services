package com.nexus.rinde.dashboard.application.internal.queryservices;

import com.nexus.rinde.dashboard.application.internal.outboundservices.acl.DashboardFleetClient;
import com.nexus.rinde.dashboard.application.internal.outboundservices.acl.DashboardSettlementClient;
import com.nexus.rinde.dashboard.application.internal.outboundservices.acl.DashboardTripClient;
import com.nexus.rinde.dashboard.domain.model.queries.GetFleetStatusQuery;
import com.nexus.rinde.dashboard.domain.model.queries.GetOperationalMetricsQuery;
import com.nexus.rinde.dashboard.domain.model.queries.GetTripSummaryQuery;
import com.nexus.rinde.dashboard.domain.services.DashboardQueryService;
import com.nexus.rinde.dashboard.interfaces.rest.resources.FleetStatusResource;
import com.nexus.rinde.dashboard.interfaces.rest.resources.FleetStatusResource.DriverSummary;
import com.nexus.rinde.dashboard.interfaces.rest.resources.FleetStatusResource.VehicleSummary;
import com.nexus.rinde.dashboard.interfaces.rest.resources.OperationalMetricsResource;
import com.nexus.rinde.dashboard.interfaces.rest.resources.TripSummaryResource;
import com.nexus.rinde.expense.interfaces.acl.ExpenseContextFacade;
import com.nexus.rinde.expense.interfaces.acl.ExpenseSummary;
import com.nexus.rinde.fleet.interfaces.acl.FleetDriverSummary;
import com.nexus.rinde.fleet.interfaces.acl.FleetVehicleSummary;
import com.nexus.rinde.dashboard.application.internal.outboundservices.acl.DashboardSettlementClient.SettlementInfo;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import com.nexus.rinde.trip.interfaces.acl.TripSummary;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación del servicio de consulta del dashboard operativo. */
@Service
public class DashboardQueryServiceImpl implements DashboardQueryService {

  private final DashboardTripClient tripClient;
  private final DashboardSettlementClient settlementClient;
  private final ExpenseContextFacade expenseFacade;
  private final DashboardFleetClient fleetClient;

  public DashboardQueryServiceImpl(
      DashboardTripClient tripClient,
      DashboardSettlementClient settlementClient,
      ExpenseContextFacade expenseFacade,
      DashboardFleetClient fleetClient) {
    this.tripClient = tripClient;
    this.settlementClient = settlementClient;
    this.expenseFacade = expenseFacade;
    this.fleetClient = fleetClient;
  }

  @Override
  @Transactional(readOnly = true)
  public TripSummaryResource handle(GetTripSummaryQuery query) {
    TripSummary trip =
        tripClient
            .findByIdAndTenantId(query.tripId(), query.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("El viaje no existe."));

    List<ExpenseSummary> expenses =
        expenseFacade.findByTenantIdAndTripId(query.tenantId(), query.tripId());

    BigDecimal expenseTotal =
        expenses.stream()
            .filter(e -> "APPROVED".equals(e.status()))
            .map(e -> e.amount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    String expenseCurrency =
        expenses.stream()
            .filter(e -> "APPROVED".equals(e.status()))
            .map(e -> e.currency())
            .findFirst()
            .orElse("PEN");

    SettlementInfo settlement =
        settlementClient.findByTripIdAndTenantId(query.tripId(), query.tenantId()).orElse(null);

    return new TripSummaryResource(
        trip.id(),
        trip.code(),
        trip.status(),
        trip.origin(),
        trip.destination(),
        trip.cargoDescription(),
        trip.cargoWeightKg(),
        trip.departureDate(),
        trip.vehicleId(),
        trip.driverId(),
        trip.startedAt(),
        trip.finishedAt(),
        expenses.size(),
        expenseTotal,
        expenseCurrency,
        settlement != null ? settlement.status() : null,
        settlement != null ? settlement.advanceAmount() : null,
        settlement != null ? settlement.advanceBalance() : null);
  }

  @Override
  @Transactional(readOnly = true)
  public FleetStatusResource handle(GetFleetStatusQuery query) {
    UUID tenantId = query.tenantId();

    List<FleetVehicleSummary> vehicles = fleetClient.findVehiclesByTenantId(tenantId);
    List<FleetDriverSummary> drivers = fleetClient.findDriversByTenantId(tenantId);

    int available = (int) vehicles.stream().filter(v -> "AVAILABLE".equals(v.status())).count();
    int inTrip = (int) vehicles.stream().filter(v -> "IN_TRIP".equals(v.status())).count();
    int inMaintenance =
        (int) vehicles.stream().filter(v -> "IN_MAINTENANCE".equals(v.status())).count();

    int enabled = (int) drivers.stream().filter(d -> "ENABLED".equals(d.status())).count();
    int disabled = (int) drivers.stream().filter(d -> "DISABLED".equals(d.status())).count();

    List<VehicleSummary> vehicleSummaries =
        vehicles.stream()
            .map(
                v ->
                    new VehicleSummary(
                        v.id().toString(),
                        v.plateNumber(),
                        v.brand(),
                        v.model(),
                        v.status()))
            .toList();

    List<DriverSummary> driverSummaries =
        drivers.stream()
            .map(
                d ->
                    new DriverSummary(
                        d.id().toString(),
                        d.fullName(),
                        d.licenseNumber(),
                        d.licenseCategory(),
                        d.status()))
            .toList();

    return new FleetStatusResource(
        vehicles.size(),
        available,
        inTrip,
        inMaintenance,
        drivers.size(),
        enabled,
        disabled,
        vehicleSummaries,
        driverSummaries);
  }

  @Override
  @Transactional(readOnly = true)
  public OperationalMetricsResource handle(GetOperationalMetricsQuery query) {
    UUID tenantId = query.tenantId();

    List<TripSummary> trips = tripClient.findByTenantId(tenantId);
    List<ExpenseSummary> expenses = expenseFacade.findByTenantId(tenantId);
    long pendingSettlements = settlementClient.countPendingByTenantId(tenantId);

    int scheduled = (int) trips.stream().filter(t -> "SCHEDULED".equals(t.status())).count();
    int assigned = (int) trips.stream().filter(t -> "ASSIGNED".equals(t.status())).count();
    int inRoute = (int) trips.stream().filter(t -> "IN_ROUTE".equals(t.status())).count();
    int finished = (int) trips.stream().filter(t -> "FINISHED".equals(t.status())).count();
    int settled = (int) trips.stream().filter(t -> "SETTLED".equals(t.status())).count();

    int approved = (int) expenses.stream().filter(e -> "APPROVED".equals(e.status())).count();
    int pending =
        (int)
            expenses.stream()
                .filter(
                    e ->
                        "PENDING_SUPPORT".equals(e.status())
                            || "REGISTERED".equals(e.status()))
                .count();
    int observed = (int) expenses.stream().filter(e -> "OBSERVED".equals(e.status())).count();

    return new OperationalMetricsResource(
        trips.size(),
        scheduled,
        assigned,
        inRoute,
        finished,
        settled,
        expenses.size(),
        approved,
        pending,
        observed,
        pendingSettlements);
  }
}
