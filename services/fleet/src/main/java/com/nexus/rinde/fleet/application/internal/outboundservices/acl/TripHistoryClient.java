package com.nexus.rinde.fleet.application.internal.outboundservices.acl;

import java.util.List;
import java.util.UUID;

/** Puerto de consulta de viajes asociados a un vehículo. */
public interface TripHistoryClient {

  RemoteLookupResult<List<ExternalTripSummary>> findByTenantAndVehicle(UUID tenantId, UUID vehicleId);
}
