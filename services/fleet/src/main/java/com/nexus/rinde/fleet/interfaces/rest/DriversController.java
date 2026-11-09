package com.nexus.rinde.fleet.interfaces.rest;

import com.nexus.rinde.fleet.domain.model.aggregates.Driver;
import com.nexus.rinde.fleet.domain.model.commands.RegisterDriverCommand;
import com.nexus.rinde.fleet.domain.model.queries.CheckDriverEligibilityQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetDriverByIdQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetDriverByUserIdQuery;
import com.nexus.rinde.fleet.domain.model.queries.ListDriversQuery;
import com.nexus.rinde.fleet.domain.model.valueobjects.DriverEligibility;
import com.nexus.rinde.fleet.domain.services.DriverCommandService;
import com.nexus.rinde.fleet.domain.services.DriverQueryService;
import com.nexus.rinde.fleet.interfaces.rest.resources.CreateDriverResource;
import com.nexus.rinde.fleet.interfaces.rest.resources.DriverEligibilityResource;
import com.nexus.rinde.fleet.interfaces.rest.resources.DriverResource;
import com.nexus.rinde.fleet.interfaces.rest.transform.DriverCommandAssembler;
import com.nexus.rinde.fleet.interfaces.rest.transform.DriverResourceAssembler;
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
import org.springframework.web.bind.annotation.RestController;

/** Operaciones sobre los conductores de la flota. */
@RestController
@RequestMapping(value = "/api/v1/drivers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Drivers", description = "Gestión de conductores y vigencia de licencias")
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
public class DriversController {

  private final DriverCommandService driverCommandService;
  private final DriverQueryService driverQueryService;

  public DriversController(
      DriverCommandService driverCommandService, DriverQueryService driverQueryService) {
    this.driverCommandService = driverCommandService;
    this.driverQueryService = driverQueryService;
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Registrar un conductor (US13)",
      description =
          "Registra un conductor con sus datos de licencia. Si la licencia está vencida, el conductor no queda habilitado. Si indica userId, la cuenta debe ser DRIVER de la empresa y no estar vinculada a otro conductor.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Conductor registrado"),
    @ApiResponse(
        responseCode = "400",
        description = "Datos inválidos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "El documento o número de licencia ya existe en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<DriverResource> create(@Valid @RequestBody CreateDriverResource resource) {
    RegisterDriverCommand command =
        DriverCommandAssembler.toCommand(TenantContext.tenantId(), resource);
    Driver driver = driverCommandService.handle(command);
    return ResponseEntity.created(URI.create("/api/v1/drivers/" + driver.getId()))
        .body(DriverResourceAssembler.toResource(driver));
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Listar conductores (US13)",
      description = "Devuelve los conductores registrados en la empresa autenticada.")
  @ApiResponse(responseCode = "200", description = "Lista de conductores")
  public ResponseEntity<List<DriverResource>> list() {
    List<Driver> list = driverQueryService.handle(new ListDriversQuery(TenantContext.tenantId()));
    return ResponseEntity.ok(list.stream().map(DriverResourceAssembler::toResource).toList());
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('DRIVER')")
  @Operation(
      summary = "Consultar mi ficha de conductor",
      description =
          "Devuelve el conductor vinculado a la cuenta del token. Si la cuenta no está vinculada, responde 404.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Conductor vinculado a la cuenta"),
    @ApiResponse(
        responseCode = "404",
        description = "La cuenta no está vinculada a ningún conductor",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<DriverResource> currentDriver() {
    Driver driver =
        driverQueryService
            .handle(new GetDriverByUserIdQuery(TenantContext.tenantId(), TenantContext.userId()))
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "La cuenta no está vinculada a ningún conductor."));
    return ResponseEntity.ok(DriverResourceAssembler.toResource(driver));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Consultar detalle de un conductor",
      description = "Devuelve los datos de un conductor por identificador.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Conductor encontrado"),
    @ApiResponse(
        responseCode = "404",
        description = "El conductor no existe en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<DriverResource> getById(@PathVariable UUID id) {
    Driver driver =
        driverQueryService
            .handle(new GetDriverByIdQuery(TenantContext.tenantId(), id))
            .orElseThrow(() -> new ResourceNotFoundException("El conductor no existe."));
    return ResponseEntity.ok(DriverResourceAssembler.toResource(driver));
  }

  @GetMapping("/{id}/eligibility")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Consultar si el conductor está habilitado (US13, US15)",
      description = "Verifica la vigencia de la licencia y el estado habilitado del conductor.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Elegibilidad obtenida"),
    @ApiResponse(
        responseCode = "404",
        description = "El conductor no existe en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<DriverEligibilityResource> checkEligibility(@PathVariable UUID id) {
    DriverEligibility eligibility =
        driverQueryService.handle(new CheckDriverEligibilityQuery(TenantContext.tenantId(), id));
    return ResponseEntity.ok(DriverResourceAssembler.toEligibilityResource(eligibility));
  }
}
