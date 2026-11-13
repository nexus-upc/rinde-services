package com.nexus.rinde.dashboard.interfaces.rest;

import com.nexus.rinde.dashboard.domain.model.queries.GetFleetStatusQuery;
import com.nexus.rinde.dashboard.domain.model.queries.GetOperationalMetricsQuery;
import com.nexus.rinde.dashboard.domain.model.queries.GetTripSummaryQuery;
import com.nexus.rinde.dashboard.domain.services.DashboardQueryService;
import com.nexus.rinde.dashboard.interfaces.rest.resources.FleetStatusResource;
import com.nexus.rinde.dashboard.interfaces.rest.resources.OperationalMetricsResource;
import com.nexus.rinde.dashboard.interfaces.rest.resources.TripSummaryResource;
import com.nexus.rinde.shared.infrastructure.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints del dashboard operativo para administradores y operadores. */
@RestController
@RequestMapping(value = "/api/v1/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Dashboard", description = "Panel operativo consolidado")
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
public class DashboardController {

  private final DashboardQueryService dashboardQueryService;

  public DashboardController(DashboardQueryService dashboardQueryService) {
    this.dashboardQueryService = dashboardQueryService;
  }

  @GetMapping("/trips/{tripId}/summary")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Resumen consolidado de un viaje (US17)",
      description =
          "Devuelve el detalle del viaje junto con la cantidad de gastos, el total aprobado"
              + " y el estado de la liquidación.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Resumen del viaje"),
    @ApiResponse(
        responseCode = "404",
        description = "El viaje no existe",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<TripSummaryResource> getTripSummary(@PathVariable UUID tripId) {
    TripSummaryResource summary =
        dashboardQueryService.handle(new GetTripSummaryQuery(TenantContext.tenantId(), tripId));
    return ResponseEntity.ok(summary);
  }

  @GetMapping("/fleet-status")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Estado actual de la flota (US37)",
      description =
          "Devuelve el conteo de vehículos por estado operativo, conductores habilitados"
              + " y la lista detallada de cada uno.")
  @ApiResponse(responseCode = "200", description = "Estado de la flota")
  public ResponseEntity<FleetStatusResource> getFleetStatus() {
    FleetStatusResource status =
        dashboardQueryService.handle(new GetFleetStatusQuery(TenantContext.tenantId()));
    return ResponseEntity.ok(status);
  }

  @GetMapping("/metrics")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Métricas operativas consolidadas (US37)",
      description =
          "Devuelve conteos de viajes y gastos del periodo indicado (from/to en AAAA-MM-DD)."
              + " Sin parámetros usa el mes en curso hasta hoy.")
  @ApiResponse(responseCode = "200", description = "Métricas operativas")
  public ResponseEntity<OperationalMetricsResource> getMetrics(
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
    LocalDate resolvedFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
    LocalDate resolvedTo = to != null ? to : LocalDate.now();
    OperationalMetricsResource metrics =
        dashboardQueryService.handle(
            new GetOperationalMetricsQuery(TenantContext.tenantId(), resolvedFrom, resolvedTo));
    return ResponseEntity.ok(metrics);
  }
}
