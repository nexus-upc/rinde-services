package com.nexus.rinde.incident.interfaces.rest;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.commands.CloseIncidentCommand;
import com.nexus.rinde.incident.domain.model.commands.ReportIncidentCommand;
import com.nexus.rinde.incident.domain.model.commands.StartIncidentAttentionCommand;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentStatus;
import com.nexus.rinde.incident.domain.model.valueobjects.ReportOutcome;
import com.nexus.rinde.incident.domain.services.IncidentCommandService;
import com.nexus.rinde.incident.domain.services.IncidentQueryService;
import com.nexus.rinde.incident.interfaces.rest.resources.ChangeIncidentStatusResource;
import com.nexus.rinde.incident.interfaces.rest.resources.CloseIncidentResource;
import com.nexus.rinde.incident.interfaces.rest.resources.IncidentResource;
import com.nexus.rinde.incident.interfaces.rest.resources.ReportIncidentResource;
import com.nexus.rinde.incident.interfaces.rest.transform.IncidentResourceFromEntityAssembler;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.infrastructure.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reporte de incidencias por el conductor, consulta de la bandeja y atención o cierre. */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Incidents", description = "Reporte, consulta, atención y cierre de incidencias")
@ApiResponses({
  @ApiResponse(
      responseCode = "401",
      description = "Falta el token o no es válido",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class))),
  @ApiResponse(
      responseCode = "403",
      description = "El rol no tiene permiso para esta operación",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
})
public class IncidentsController {

  private final IncidentCommandService commandService;
  private final IncidentQueryService queryService;

  public IncidentsController(
      IncidentCommandService commandService, IncidentQueryService queryService) {
    this.commandService = commandService;
    this.queryService = queryService;
  }

  @PostMapping("/api/v1/trips/{tripId}/incidents")
  @PreAuthorize("hasRole('DRIVER')")
  @Operation(
      summary = "Reportar una incidencia durante el viaje (US29)",
      description =
          "Registra una avería, un retraso u otro acontecimiento del viaje en ruta. Solo lo"
              + " puede reportar el conductor asignado. Con clave de idempotencia, un reenvío"
              + " del mismo reporte no crea otra incidencia.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Incidencia registrada"),
    @ApiResponse(
        responseCode = "200",
        description = "La clave de idempotencia ya existía; se devuelve la incidencia registrada"),
    @ApiResponse(
        responseCode = "400",
        description = "Datos inválidos o descripción faltante",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "El conductor no está asignado al viaje",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "El viaje no existe en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "El viaje no está en ruta o la clave ya la usó otro usuario",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<IncidentResource> report(
      @PathVariable UUID tripId, @Valid @RequestBody ReportIncidentResource resource) {
    ReportOutcome outcome =
        commandService.report(
            new ReportIncidentCommand(
                TenantContext.tenantId(),
                tripId,
                TenantContext.userId(),
                resource.type(),
                resource.description(),
                resource.evidenceUrl(),
                resource.idempotencyKey()));
    IncidentResource body = IncidentResourceFromEntityAssembler.toResource(outcome.incident());
    if (!outcome.created()) {
      return ResponseEntity.ok(body);
    }
    return ResponseEntity.created(URI.create("/api/v1/incidents/" + body.id())).body(body);
  }

  @GetMapping("/api/v1/incidents")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Listar las incidencias reportadas (US30)",
      description =
          "Devuelve las incidencias de la empresa ordenadas por fecha, con filtros opcionales"
              + " por estado, viaje y vehículo.")
  @ApiResponse(responseCode = "200", description = "Listado de incidencias")
  public ResponseEntity<List<IncidentResource>> list(
      @RequestParam(required = false) IncidentStatus status,
      @RequestParam(required = false) UUID tripId,
      @RequestParam(required = false) UUID vehicleId) {
    List<Incident> incidents =
        queryService.list(TenantContext.tenantId(), status, tripId, vehicleId);
    return ResponseEntity.ok(
        incidents.stream().map(IncidentResourceFromEntityAssembler::toResource).toList());
  }

  @GetMapping("/api/v1/incidents/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Consultar el detalle de una incidencia (US30)",
      description =
          "El responsable ve cualquier incidencia de la empresa; el conductor solo las que"
              + " reportó.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Detalle de la incidencia"),
    @ApiResponse(
        responseCode = "404",
        description = "La incidencia no existe en la empresa o no es visible para el usuario",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<IncidentResource> detail(@PathVariable UUID id) {
    Incident incident =
        queryService.findVisible(
            TenantContext.tenantId(), id, TenantContext.userId(), TenantContext.role());
    return ResponseEntity.ok(IncidentResourceFromEntityAssembler.toResource(incident));
  }

  @PatchMapping("/api/v1/incidents/{id}/status")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Pasar una incidencia a atención (US31)",
      description =
          "Cambia una incidencia abierta a en atención. Para cerrarla se usa la acción de cierre.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Incidencia en atención"),
    @ApiResponse(
        responseCode = "400",
        description = "Estado solicitado no válido para esta operación",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "La incidencia no existe en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "La incidencia no está abierta",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<IncidentResource> changeStatus(
      @PathVariable UUID id, @Valid @RequestBody ChangeIncidentStatusResource resource) {
    if (resource.status() != IncidentStatus.IN_PROGRESS) {
      throw new BusinessRuleException(
          "Este cambio solo pasa la incidencia a en atención. Para cerrarla use la acción de cierre.");
    }
    Incident incident =
        commandService.startAttention(
            new StartIncidentAttentionCommand(TenantContext.tenantId(), id));
    return ResponseEntity.ok(IncidentResourceFromEntityAssembler.toResource(incident));
  }

  @PostMapping("/api/v1/incidents/{id}/close")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Cerrar una incidencia con la acción tomada (US31)",
      description =
          "Registra la acción tomada, quién la atendió y la fecha, y deja la incidencia cerrada en"
              + " el historial del vehículo.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Incidencia cerrada"),
    @ApiResponse(
        responseCode = "400",
        description = "Falta la acción tomada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "La incidencia no existe en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "La incidencia ya estaba cerrada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<IncidentResource> close(
      @PathVariable UUID id, @Valid @RequestBody CloseIncidentResource resource) {
    Incident incident =
        commandService.close(
            new CloseIncidentCommand(
                TenantContext.tenantId(), id, TenantContext.userId(), resource.action()));
    return ResponseEntity.ok(IncidentResourceFromEntityAssembler.toResource(incident));
  }
}
