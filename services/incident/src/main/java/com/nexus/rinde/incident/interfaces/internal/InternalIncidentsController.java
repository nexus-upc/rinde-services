package com.nexus.rinde.incident.interfaces.internal;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.services.IncidentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultas de incidencias abiertas que usa el tablero operativo de Dashboard por HTTP. No las
 * enruta el gateway.
 */
@RestController
@RequestMapping(
    value = "/api/internal/v1/incidents/tenants/{tenantId}",
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Internal", description = "Consultas entre servicios de RINDE (no públicas)")
@SecurityRequirements
public class InternalIncidentsController {

  private final IncidentQueryService queryService;

  public InternalIncidentsController(IncidentQueryService queryService) {
    this.queryService = queryService;
  }

  @GetMapping("/open")
  @Operation(summary = "Incidencias abiertas o en atención de la empresa")
  public List<InternalIncidentResource> listOpen(@PathVariable UUID tenantId) {
    return queryService.findOpen(tenantId).stream()
        .map(InternalIncidentsController::toResource)
        .toList();
  }

  @GetMapping("/open-count")
  @Operation(summary = "Cantidad de incidencias abiertas o en atención de la empresa")
  public OpenIncidentCount countOpen(@PathVariable UUID tenantId) {
    return new OpenIncidentCount(queryService.findOpen(tenantId).size());
  }

  private static InternalIncidentResource toResource(Incident incident) {
    return new InternalIncidentResource(
        incident.getId(),
        incident.getTripId(),
        incident.getVehicleId(),
        incident.getType().name(),
        incident.getStatus().name(),
        incident.getReportedAt());
  }

  public record InternalIncidentResource(
      UUID id,
      UUID tripId,
      UUID vehicleId,
      String type,
      String status,
      Instant reportedAt) {}

  public record OpenIncidentCount(long count) {}
}
