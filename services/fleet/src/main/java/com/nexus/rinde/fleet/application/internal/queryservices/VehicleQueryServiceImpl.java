package com.nexus.rinde.fleet.application.internal.queryservices;

import com.nexus.rinde.fleet.domain.model.aggregates.Vehicle;
import com.nexus.rinde.fleet.domain.model.entities.Maintenance;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleByIdQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleHealthStatusQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleHistoryQuery;
import com.nexus.rinde.fleet.domain.model.queries.ListVehiclesQuery;
import com.nexus.rinde.fleet.domain.model.valueobjects.VehicleHealthStatus;
import com.nexus.rinde.fleet.domain.model.valueobjects.VehicleHistory;
import com.nexus.rinde.fleet.domain.services.VehicleQueryService;
import com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories.MaintenanceRepository;
import com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories.VehicleRepository;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Implementación del servicio de aplicación de consultas para Vehicle. */
@Service
@Transactional(readOnly = true)
public class VehicleQueryServiceImpl implements VehicleQueryService {

  private final VehicleRepository vehicleRepository;
  private final MaintenanceRepository maintenanceRepository;

  public VehicleQueryServiceImpl(
      VehicleRepository vehicleRepository, MaintenanceRepository maintenanceRepository) {
    this.vehicleRepository = vehicleRepository;
    this.maintenanceRepository = maintenanceRepository;
  }

  @Override
  public Optional<Vehicle> handle(GetVehicleByIdQuery query) {
    return vehicleRepository.findByIdAndTenantId(query.vehicleId(), query.tenantId());
  }

  @Override
  public List<Vehicle> handle(ListVehiclesQuery query) {
    return vehicleRepository.findByTenantId(query.tenantId());
  }

  @Override
  public long countByTenantId(java.util.UUID tenantId) {
    return vehicleRepository.countByTenantId(tenantId);
  }

  @Override
  public VehicleHealthStatus handle(GetVehicleHealthStatusQuery query) {
    Vehicle vehicle =
        vehicleRepository
            .findByIdAndTenantId(query.vehicleId(), query.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("El vehículo no existe."));

    List<Maintenance> maintenances =
        maintenanceRepository.findByTenantIdAndVehicleId(query.tenantId(), query.vehicleId());

    return vehicle.evaluateHealth(LocalDate.now(), maintenances);
  }

  @Override
  public VehicleHistory handle(GetVehicleHistoryQuery query) {
    throw new ResponseStatusException(
        HttpStatus.NOT_IMPLEMENTED, "Funcionalidad pendiente de implementar en S4-13.");
  }
}
