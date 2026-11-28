package com.nexus.rinde.trip.interfaces.rest;

import com.nexus.rinde.trip.domain.model.aggregates.Trip;
import com.nexus.rinde.trip.domain.model.queries.GetLatestTripLocationsQuery;
import com.nexus.rinde.trip.domain.model.queries.GetTripByIdQuery;
import com.nexus.rinde.trip.domain.model.queries.GetTripsAssignedToMeQuery;
import com.nexus.rinde.trip.domain.model.queries.GetTripsByTenantQuery;
import com.nexus.rinde.trip.domain.services.TripQueryService;
import com.nexus.rinde.trip.domain.model.valueobjects.TripStatus;
import com.nexus.rinde.trip.interfaces.rest.resources.LatestTripLocationResource;
import com.nexus.rinde.trip.interfaces.rest.resources.TripDetailResource;
import com.nexus.rinde.trip.interfaces.rest.resources.TripSummaryResource;
import com.nexus.rinde.trip.interfaces.rest.transform.TripLocationResourceFromEntityAssembler;
import com.nexus.rinde.trip.interfaces.rest.transform.TripResourceFromEntityAssembler;
import com.nexus.rinde.trip.interfaces.rest.transform.TripStatusFilterFromParameterAssembler;
import com.nexus.rinde.shared.infrastructure.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consultas del tablero y la aplicación del conductor. */
@RestController
@RequestMapping(value = "/api/v1/trips", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Trip queries", description = "Tablero, detalle y viajes del conductor")
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
      description = "El rol no tiene permiso para esta consulta",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
})
public class TripQueriesController {

  private final TripQueryService tripQueryService;

  public TripQueriesController(TripQueryService tripQueryService) {
    this.tripQueryService = tripQueryService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Consultar el tablero de viajes",
      description = "status acepta uno o varios estados separados por coma.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Viajes de la empresa"),
    @ApiResponse(
        responseCode = "400",
        description = "El filtro contiene un estado que no existe",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public List<TripSummaryResource> list(
      @Parameter(
              description = "Estados separados por coma",
              example = "SCHEDULED,ASSIGNED",
              schema =
                  @Schema(
                      allowableValues = {
                        "SCHEDULED",
                        "ASSIGNED",
                        "IN_ROUTE",
                        "FINISHED",
                        "SETTLED"
                      }))
          @RequestParam(required = false)
          String status) {
    Set<TripStatus> statuses = TripStatusFilterFromParameterAssembler.toStatuses(status);
    return tripQueryService
        .handle(new GetTripsByTenantQuery(TenantContext.tenantId(), statuses))
        .stream()
        .map(TripResourceFromEntityAssembler::toSummaryResource)
        .toList();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER','DRIVER')")
  @Operation(
      summary = "Consultar el detalle de un viaje",
      description =
          "Un conductor solo ve viajes asignados a él; un viaje ajeno se responde como 404.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Detalle del viaje"),
    @ApiResponse(
        responseCode = "404",
        description = "El viaje no existe en la empresa o no pertenece al conductor",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public TripDetailResource getById(@PathVariable UUID id) {
    Trip trip =
        tripQueryService.handle(
            new GetTripByIdQuery(
                TenantContext.tenantId(), id, TenantContext.userId(), TenantContext.role()));
    return TripResourceFromEntityAssembler.toDetailResource(trip);
  }

  @GetMapping("/assigned-to-me")
  @PreAuthorize("hasRole('DRIVER')")
  @Operation(
      summary = "Consultar los viajes asignados al conductor",
      description = "Los viajes se ordenan por fecha de salida; si no hay asignaciones devuelve [].")
  @ApiResponse(responseCode = "200", description = "Viajes propios ordenados por fecha")
  public List<TripSummaryResource> assignedToMe() {
    return tripQueryService
        .handle(new GetTripsAssignedToMeQuery(TenantContext.tenantId(), TenantContext.userId()))
        .stream()
        .map(TripResourceFromEntityAssembler::toSummaryResource)
        .toList();
  }

  @GetMapping("/locations/latest")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Consultar la última ubicación de los viajes en ruta",
      description =
          "Devuelve el último punto registrado de cada viaje IN_ROUTE de la empresa. Un viaje en"
              + " ruta sin puntos registrados aparece con ubicación null.")
  @ApiResponse(responseCode = "200", description = "Últimas ubicaciones de viajes en ruta")
  public List<LatestTripLocationResource> latestLocations() {
    return tripQueryService
        .handle(new GetLatestTripLocationsQuery(TenantContext.tenantId()))
        .stream()
        .map(TripLocationResourceFromEntityAssembler::toLatestResource)
        .toList();
  }
}
