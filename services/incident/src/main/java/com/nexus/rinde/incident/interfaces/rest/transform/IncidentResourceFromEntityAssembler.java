package com.nexus.rinde.incident.interfaces.rest.transform;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentResolution;
import com.nexus.rinde.incident.interfaces.rest.resources.IncidentResolutionResource;
import com.nexus.rinde.incident.interfaces.rest.resources.IncidentResource;

/** Transforma la entidad Incident a su recurso REST público. */
public final class IncidentResourceFromEntityAssembler {

  private IncidentResourceFromEntityAssembler() {}

  public static IncidentResource toResource(Incident incident) {
    IncidentResolution resolution = incident.getResolution();
    IncidentResolutionResource resolutionResource =
        resolution == null
            ? null
            : new IncidentResolutionResource(
                resolution.getAction(), resolution.getClosedBy(), resolution.getClosedAt());

    return new IncidentResource(
        incident.getId(),
        incident.getTripId(),
        incident.getVehicleId(),
        incident.getReportedBy(),
        incident.getType(),
        incident.getDescription(),
        incident.getEvidenceUrl(),
        incident.getStatus(),
        incident.getReportedAt(),
        resolutionResource,
        incident.getUpdatedAt());
  }
}
