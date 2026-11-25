package com.nexus.rinde.fleet.application.internal.outboundservices.acl;

import java.util.List;
import java.util.UUID;

/** Puerto de consulta de incidencias asociadas a un vehículo. */
public interface IncidentHistoryClient {

  RemoteLookupResult<List<ExternalIncidentSummary>> findByTenantAndVehicle(
      UUID tenantId, UUID vehicleId);
}
