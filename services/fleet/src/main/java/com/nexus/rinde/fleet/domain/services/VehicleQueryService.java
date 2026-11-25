package com.nexus.rinde.fleet.domain.services;

import com.nexus.rinde.fleet.domain.model.aggregates.Vehicle;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleByIdQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleHealthStatusQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleHistoryQuery;
import com.nexus.rinde.fleet.domain.model.queries.ListVehiclesQuery;
import com.nexus.rinde.fleet.domain.model.valueobjects.VehicleHealthStatus;
import com.nexus.rinde.fleet.domain.model.valueobjects.VehicleHistory;
import java.util.List;
import java.util.Optional;

/** Servicio de aplicación para consultas sobre el agregado Vehicle. */
public interface VehicleQueryService {

  Optional<Vehicle> handle(GetVehicleByIdQuery query);

  List<Vehicle> handle(ListVehiclesQuery query);

  long countByTenantId(java.util.UUID tenantId);

  VehicleHealthStatus handle(GetVehicleHealthStatusQuery query);

  VehicleHistory handle(GetVehicleHistoryQuery query);
}
