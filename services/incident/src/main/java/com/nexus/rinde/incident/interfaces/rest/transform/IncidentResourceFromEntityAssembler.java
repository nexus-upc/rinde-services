package com.nexus.rinde.incident.interfaces.rest.transform;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.entities.IncidentEvidence;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentResolution;
import com.nexus.rinde.incident.interfaces.rest.resources.IncidentEvidenceResource;
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
    IncidentEvidenceResource evidenceResource =
        incident.getEvidence() == null ? null : toEvidenceResource(incident, incident.getEvidence());

    return new IncidentResource(
        incident.getId(),
        incident.getTripId(),
        incident.getVehicleId(),
        incident.getReportedBy(),
        incident.getType(),
        incident.getDescription(),
        incident.getEvidenceUrl(),
        evidenceResource,
        incident.getStatus(),
        incident.getReportedAt(),
        resolutionResource,
        incident.getUpdatedAt());
  }

  /** Construye los metadatos públicos de la evidencia con su ruta autenticada. */
  private static IncidentEvidenceResource toEvidenceResource(
      Incident incident, IncidentEvidence evidence) {
    String contentUrl =
        "/api/v1/incidents/%s/evidences/%s/content".formatted(incident.getId(), evidence.getId());
    return new IncidentEvidenceResource(
        evidence.getId(),
        evidence.getFileName(),
        evidence.getContentType(),
        evidence.getFileSizeBytes(),
        evidence.getSha256(),
        contentUrl,
        evidence.getUploadedAt());
  }
}
