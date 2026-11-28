package com.nexus.rinde.trip.interfaces.rest;

import com.nexus.rinde.trip.domain.model.aggregates.Trip;
import com.nexus.rinde.trip.domain.model.commands.AssignTripCommand;
import com.nexus.rinde.trip.domain.model.commands.FinishTripCommand;
import com.nexus.rinde.trip.domain.model.commands.RecordTripLocationsCommand;
import com.nexus.rinde.trip.domain.model.commands.ScheduleTripCommand;
import com.nexus.rinde.trip.domain.model.commands.StartTripCommand;
import com.nexus.rinde.trip.domain.services.TripCommandService;
import com.nexus.rinde.trip.domain.services.TripAssignmentResult;
import com.nexus.rinde.trip.domain.services.TripLocationBatchResult;
import com.nexus.rinde.trip.interfaces.rest.resources.AssignTripResource;
import com.nexus.rinde.trip.interfaces.rest.resources.RecordTripLocationsResource;
import com.nexus.rinde.trip.interfaces.rest.resources.RecordTripLocationsResponseResource;
import com.nexus.rinde.trip.interfaces.rest.resources.ScheduleTripResource;
import com.nexus.rinde.trip.interfaces.rest.resources.TripAssignmentResponseResource;
import com.nexus.rinde.trip.interfaces.rest.resources.TripDetailResource;
import com.nexus.rinde.trip.interfaces.rest.transform.AssignTripCommandFromResourceAssembler;
import com.nexus.rinde.trip.interfaces.rest.transform.RecordTripLocationsCommandFromResourceAssembler;
import com.nexus.rinde.trip.interfaces.rest.transform.ScheduleTripCommandFromResourceAssembler;
import com.nexus.rinde.trip.interfaces.rest.transform.TripLocationResourceFromEntityAssembler;
import com.nexus.rinde.trip.interfaces.rest.transform.TripResourceFromEntityAssembler;
import com.nexus.rinde.shared.infrastructure.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operaciones que modifican el ciclo de vida de un viaje. */
@RestController
@RequestMapping(value = "/api/v1/trips", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Trips", description = "Programación, asignación e inicio y fin de viajes")
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
public class TripsController {

  private final TripCommandService tripCommandService;

  public TripsController(TripCommandService tripCommandService) {
    this.tripCommandService = tripCommandService;
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Programar un viaje",
      description =
          "Crea un viaje SCHEDULED con código correlativo por empresa. Una fecha anterior a hoy"
              + " requiere confirmPastDate=true.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Viaje programado"),
    @ApiResponse(
        responseCode = "400",
        description = "Ruta, carga, peso o fecha inválidos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "La fecha pasada no fue confirmada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<TripDetailResource> schedule(
      @Valid @RequestBody ScheduleTripResource resource) {
    TenantContext.requireUnrestrictedTenant();
    ScheduleTripCommand command =
        ScheduleTripCommandFromResourceAssembler.toCommand(
            TenantContext.tenantId(), TenantContext.userId(), resource);
    Trip trip = tripCommandService.handle(command);
    return ResponseEntity.created(URI.create("/api/v1/trips/" + trip.getId()))
        .body(TripResourceFromEntityAssembler.toDetailResource(trip));
  }

  @PutMapping("/{id}/assignment")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Asignar vehículo y conductor",
      description =
          "Revisa primero la ocupación por fecha, luego Fleet. Si el mantenimiento está vencido,"
              + " requiere confirmOverdueMaintenance=true; DUE_SOON devuelve maintenanceAlert.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Viaje asignado"),
    @ApiResponse(
        responseCode = "400",
        description = "Falta un identificador de Fleet",
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
        description = "Estado, ocupación, habilitación o mantenimiento impiden la asignación",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public TripAssignmentResponseResource assign(
      @PathVariable UUID id, @Valid @RequestBody AssignTripResource resource) {
    AssignTripCommand command =
        AssignTripCommandFromResourceAssembler.toCommand(
            TenantContext.tenantId(), id, TenantContext.userId(), resource);
    TripAssignmentResult result = tripCommandService.handle(command);
    return TripResourceFromEntityAssembler.toAssignmentResource(result);
  }

  @PostMapping("/{id}/start")
  @PreAuthorize("hasRole('DRIVER')")
  @Operation(
      summary = "Iniciar un viaje asignado",
      description = "Solo el conductor asignado puede iniciar un viaje en estado ASSIGNED.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Viaje iniciado"),
    @ApiResponse(
        responseCode = "404",
        description = "El viaje no existe en la empresa o no pertenece al conductor",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "El viaje no está en estado ASSIGNED",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public TripDetailResource start(@PathVariable UUID id) {
    Trip trip =
        tripCommandService.handle(
            new StartTripCommand(TenantContext.tenantId(), id, TenantContext.userId()));
    return TripResourceFromEntityAssembler.toDetailResource(trip);
  }

  @PostMapping("/{id}/finish")
  @PreAuthorize("hasRole('DRIVER')")
  @Operation(
      summary = "Finalizar un viaje en curso",
      description = "Solo el conductor asignado puede finalizar un viaje en estado IN_ROUTE.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Viaje finalizado"),
    @ApiResponse(
        responseCode = "404",
        description = "El viaje no existe en la empresa o no pertenece al conductor",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "El viaje no está en estado IN_ROUTE",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public TripDetailResource finish(@PathVariable UUID id) {
    Trip trip =
        tripCommandService.handle(
            new FinishTripCommand(TenantContext.tenantId(), id, TenantContext.userId()));
    return TripResourceFromEntityAssembler.toDetailResource(trip);
  }

  @PostMapping("/{id}/locations")
  @PreAuthorize("hasRole('DRIVER')")
  @Operation(
      summary = "Registrar ubicaciones del viaje en ruta",
      description =
          "Recibe de 1 a 50 puntos capturados por el conductor asignado mientras el viaje está en"
              + " IN_ROUTE. Los puntos con recordedAt repetido para el mismo viaje se omiten sin"
              + " duplicar.")
  @ApiResponses({
    @ApiResponse(responseCode = "202", description = "Puntos procesados"),
    @ApiResponse(
        responseCode = "400",
        description = "Lote o coordenadas inválidas",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "El viaje no existe en la empresa o no pertenece al conductor",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "El viaje no está en estado IN_ROUTE",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "503",
        description = "Fleet no disponible para validar al conductor",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<RecordTripLocationsResponseResource> recordLocations(
      @PathVariable UUID id, @Valid @RequestBody RecordTripLocationsResource resource) {
    RecordTripLocationsCommand command =
        RecordTripLocationsCommandFromResourceAssembler.toCommand(
            TenantContext.tenantId(), id, TenantContext.userId(), resource);
    TripLocationBatchResult result = tripCommandService.handle(command);
    return ResponseEntity.accepted()
        .body(TripLocationResourceFromEntityAssembler.toBatchResponse(result));
  }
}
