package com.nexus.rinde.fleet.interfaces.rest;

import com.nexus.rinde.fleet.domain.model.aggregates.Vehicle;
import com.nexus.rinde.fleet.domain.model.commands.RecordMaintenanceCommand;
import com.nexus.rinde.fleet.domain.model.commands.RegisterVehicleCommand;
import com.nexus.rinde.fleet.domain.model.entities.Maintenance;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleByIdQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleHealthStatusQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleHistoryQuery;
import com.nexus.rinde.fleet.domain.model.queries.ListVehiclesQuery;
import com.nexus.rinde.fleet.domain.model.valueobjects.VehicleHealthStatus;
import com.nexus.rinde.fleet.domain.model.valueobjects.VehicleHistory;
import com.nexus.rinde.fleet.domain.services.MaintenanceCommandService;
import com.nexus.rinde.fleet.domain.services.VehicleCommandService;
import com.nexus.rinde.fleet.domain.services.VehicleQueryService;
import com.nexus.rinde.fleet.interfaces.rest.resources.CreateVehicleResource;
import com.nexus.rinde.fleet.interfaces.rest.resources.MaintenanceResource;
import com.nexus.rinde.fleet.interfaces.rest.resources.RecordMaintenanceResource;
import com.nexus.rinde.fleet.interfaces.rest.resources.VehicleHealthStatusResource;
import com.nexus.rinde.fleet.interfaces.rest.resources.VehicleHistoryItemResource;
import com.nexus.rinde.fleet.interfaces.rest.resources.VehicleHistoryResource;
import com.nexus.rinde.fleet.interfaces.rest.resources.VehicleResource;
import com.nexus.rinde.fleet.interfaces.rest.transform.MaintenanceResourceAssembler;
import com.nexus.rinde.fleet.interfaces.rest.transform.VehicleCommandAssembler;
import com.nexus.rinde.fleet.interfaces.rest.transform.VehicleResourceAssembler;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Operaciones sobre las unidades de transporte de la empresa. */
@RestController
@RequestMapping(value = "/api/v1/vehicles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Vehicles", description = "Gestión de vehículos de la flota")
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
public class VehiclesController {

  private final VehicleCommandService vehicleCommandService;
  private final VehicleQueryService vehicleQueryService;
  private final MaintenanceCommandService maintenanceCommandService;

  public VehiclesController(
      VehicleCommandService vehicleCommandService,
      VehicleQueryService vehicleQueryService,
      MaintenanceCommandService maintenanceCommandService) {
    this.vehicleCommandService = vehicleCommandService;
    this.vehicleQueryService = vehicleQueryService;
    this.maintenanceCommandService = maintenanceCommandService;
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Registrar un vehículo (US12)",
      description = "Registra una nueva unidad con sus datos y estado AVAILABLE. La placa debe ser única por empresa.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Vehículo registrado"),
    @ApiResponse(
        responseCode = "400",
        description = "Datos inválidos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "La placa ya existe en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<VehicleResource> create(
      @Valid @RequestBody CreateVehicleResource resource) {
    TenantContext.requireUnrestrictedTenant();
    RegisterVehicleCommand command =
        VehicleCommandAssembler.toCommand(TenantContext.tenantId(), resource);
    Vehicle vehicle = vehicleCommandService.handle(command);
    return ResponseEntity.created(URI.create("/api/v1/vehicles/" + vehicle.getId()))
        .body(VehicleResourceAssembler.toResource(vehicle));
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Listar vehículos (US12)",
      description = "Devuelve todos los vehículos pertenecientes a la empresa autenticada.")
  @ApiResponse(responseCode = "200", description = "Lista de vehículos")
  public ResponseEntity<List<VehicleResource>> list() {
    List<Vehicle> list = vehicleQueryService.handle(new ListVehiclesQuery(TenantContext.tenantId()));
    return ResponseEntity.ok(list.stream().map(VehicleResourceAssembler::toResource).toList());
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Consultar ficha del vehículo (US32)",
      description = "Devuelve los datos detallados de un vehículo por identificador.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Vehículo encontrado"),
    @ApiResponse(
        responseCode = "404",
        description = "El vehículo no existe en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<VehicleResource> getById(@PathVariable UUID id) {
    Vehicle vehicle =
        vehicleQueryService
            .handle(new GetVehicleByIdQuery(TenantContext.tenantId(), id))
            .orElseThrow(() -> new ResourceNotFoundException("El vehículo no existe."));
    return ResponseEntity.ok(VehicleResourceAssembler.toResource(vehicle));
  }

  @GetMapping("/{id}/health-status")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Consultar estado de mantenimiento del vehículo (US15, US34)",
      description = "Calcula la alerta de mantenimiento (UP_TO_DATE, DUE_SOON, OVERDUE) y disponibilidad técnica.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Estado de mantenimiento obtenido"),
    @ApiResponse(
        responseCode = "404",
        description = "El vehículo no existe en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<VehicleHealthStatusResource> getHealthStatus(@PathVariable UUID id) {
    VehicleHealthStatus status =
        vehicleQueryService.handle(new GetVehicleHealthStatusQuery(TenantContext.tenantId(), id));
    return ResponseEntity.ok(VehicleResourceAssembler.toHealthStatusResource(status));
  }

  @GetMapping("/{id}/maintenance-status")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Alias para consultar estado de mantenimiento (US15, US34)",
      description = "Endpoint alternativo para compatibilidad con la especificación de API.")
  public ResponseEntity<VehicleHealthStatusResource> getMaintenanceStatus(@PathVariable UUID id) {
    return getHealthStatus(id);
  }

  @PostMapping("/{id}/maintenances")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Registrar mantenimiento a un vehículo (US33)",
      description = "Registra un servicio de mantenimiento preventivo o correctivo para la unidad.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Mantenimiento registrado"),
    @ApiResponse(
        responseCode = "400",
        description = "Datos inválidos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "El vehículo no existe en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<MaintenanceResource> recordMaintenance(
      @PathVariable UUID id, @Valid @RequestBody RecordMaintenanceResource resource) {
    RecordMaintenanceCommand command =
        MaintenanceResourceAssembler.toCommand(TenantContext.tenantId(), id, resource);
    Maintenance maintenance = maintenanceCommandService.handle(command);
    return ResponseEntity.created(URI.create("/api/v1/maintenances/" + maintenance.getId()))
        .body(MaintenanceResourceAssembler.toResource(maintenance));
  }

  @GetMapping("/{id}/history")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Consultar historial consolidado del vehículo",
      description =
          "Unifica mantenimientos locales, viajes e incidencias del vehículo en orden"
              + " descendente por fecha.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Historial del vehículo"),
    @ApiResponse(
        responseCode = "400",
        description = "Fechas con formato inválido o rango invertido",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "El vehículo no existe en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<VehicleHistoryResource> getHistory(
      @PathVariable UUID id,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    VehicleHistory history =
        vehicleQueryService.handle(
            new GetVehicleHistoryQuery(TenantContext.tenantId(), id, from, to));
    List<VehicleHistoryItemResource> items =
        history.items().stream()
            .map(
                item ->
                    new VehicleHistoryItemResource(
                        item.type(),
                        item.id(),
                        item.date(),
                        item.title(),
                        item.status(),
                        item.detail()))
            .toList();
    return ResponseEntity.ok(
        new VehicleHistoryResource(
            history.vehicleId(),
            history.nextMaintenanceDate(),
            items,
            history.partial(),
            history.unavailableSources()));
  }
}
