package com.nexus.rinde.fleet.interfaces.acl;

import com.nexus.rinde.fleet.domain.model.aggregates.Driver;
import com.nexus.rinde.fleet.domain.model.aggregates.Vehicle;
import com.nexus.rinde.fleet.domain.model.queries.CheckDriverEligibilityQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleHealthStatusQuery;
import com.nexus.rinde.fleet.domain.model.queries.ListDriversQuery;
import com.nexus.rinde.fleet.domain.model.queries.ListVehiclesQuery;
import com.nexus.rinde.fleet.domain.services.DriverQueryService;
import com.nexus.rinde.fleet.domain.services.VehicleQueryService;
import com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories.DriverRepository;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implementación de la fachada de Fleet & Maintenance. */
@Component
public class FleetContextFacadeImpl implements FleetContextFacade {

  private final VehicleQueryService vehicleQueryService;
  private final DriverQueryService driverQueryService;
  private final DriverRepository driverRepository;

  public FleetContextFacadeImpl(
      VehicleQueryService vehicleQueryService,
      DriverQueryService driverQueryService,
      DriverRepository driverRepository) {
    this.vehicleQueryService = vehicleQueryService;
    this.driverQueryService = driverQueryService;
    this.driverRepository = driverRepository;
  }

  @Override
  public Optional<VehicleHealthSummary> findVehicleHealthStatus(UUID tenantId, UUID vehicleId) {
    try {
      var status =
          vehicleQueryService.handle(new GetVehicleHealthStatusQuery(tenantId, vehicleId));
      return Optional.of(
          new VehicleHealthSummary(
              status.vehicleId(),
              status.maintenanceState().name(),
              status.availableForTrip()));
    } catch (ResourceNotFoundException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<DriverEligibilitySummary> checkDriverEligibility(UUID tenantId, UUID driverId) {
    try {
      var eligibility =
          driverQueryService.handle(new CheckDriverEligibilityQuery(tenantId, driverId));
      return Optional.of(
          new DriverEligibilitySummary(
              eligibility.driverId(), eligibility.eligible(), eligibility.reason()));
    } catch (ResourceNotFoundException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<UUID> findDriverIdByUserId(UUID tenantId, UUID userId) {
    return driverRepository
        .findByTenantIdAndUserId(tenantId, userId)
        .map(Driver::getId);
  }

  @Override
  public long countVehiclesByTenantId(UUID tenantId) {
    return vehicleQueryService.countByTenantId(tenantId);
  }

  @Override
  public List<FleetVehicleSummary> findVehiclesByTenantId(UUID tenantId) {
    return vehicleQueryService.handle(new ListVehiclesQuery(tenantId)).stream()
        .map(FleetContextFacadeImpl::toVehicleSummary)
        .toList();
  }

  @Override
  public List<FleetDriverSummary> findDriversByTenantId(UUID tenantId) {
    return driverQueryService.handle(new ListDriversQuery(tenantId)).stream()
        .map(FleetContextFacadeImpl::toDriverSummary)
        .toList();
  }

  private static FleetVehicleSummary toVehicleSummary(Vehicle vehicle) {
    return new FleetVehicleSummary(
        vehicle.getId(),
        vehicle.getPlateNumber(),
        vehicle.getBrand(),
        vehicle.getModel(),
        vehicle.getStatus().name());
  }

  private static FleetDriverSummary toDriverSummary(Driver driver) {
    return new FleetDriverSummary(
        driver.getId(),
        driver.getUserId(),
        driver.getFullName(),
        driver.getLicenseNumber(),
        driver.getLicenseCategory(),
        driver.getStatus().name());
  }
}
