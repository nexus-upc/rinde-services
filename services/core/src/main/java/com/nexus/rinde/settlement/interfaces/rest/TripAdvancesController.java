package com.nexus.rinde.settlement.interfaces.rest;

import com.nexus.rinde.settlement.domain.model.aggregates.TripAdvance;
import com.nexus.rinde.settlement.domain.model.commands.RegisterTripAdvanceCommand;
import com.nexus.rinde.settlement.domain.model.queries.GetTripAdvancesQuery;
import com.nexus.rinde.settlement.domain.services.SettlementCommandService;
import com.nexus.rinde.settlement.domain.services.SettlementQueryService;
import com.nexus.rinde.settlement.interfaces.rest.resources.RegisterTripAdvanceResource;
import com.nexus.rinde.settlement.interfaces.rest.resources.TripAdvanceResource;
import com.nexus.rinde.settlement.interfaces.rest.transform.RegisterTripAdvanceCommandFromResourceAssembler;
import com.nexus.rinde.settlement.interfaces.rest.transform.TripAdvanceResourceFromEntityAssembler;
import com.nexus.rinde.shared.infrastructure.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

/** Adelantos de viáticos entregados al conductor antes de la salida del viaje (US35). */
@RestController
@RequestMapping(
    value = "/api/v1/trips/{tripId}/advances",
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Adelantos", description = "Adelantos de viáticos entregados al conductor")
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
public class TripAdvancesController {

  private final SettlementCommandService settlementCommandService;
  private final SettlementQueryService settlementQueryService;

  public TripAdvancesController(
      SettlementCommandService settlementCommandService,
      SettlementQueryService settlementQueryService) {
    this.settlementCommandService = settlementCommandService;
    this.settlementQueryService = settlementQueryService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Registrar adelanto de viáticos (US35)",
      description =
          "Registra el monto y el medio de entrega del adelanto. Solo se acepta mientras el viaje"
              + " esté programado o asignado. El total de los adelantos se incorpora a la"
              + " liquidación cuando el viaje finaliza.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Adelanto registrado"),
    @ApiResponse(
        responseCode = "400",
        description = "Monto menor o igual a cero, medio de entrega no válido o datos incompletos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "El viaje no existe para la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description =
            "El viaje ya está en curso o finalizado, su liquidación ya se abrió o el adelanto"
                + " está en otra moneda",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<TripAdvanceResource> register(
      @PathVariable UUID tripId, @Valid @RequestBody RegisterTripAdvanceResource resource) {
    RegisterTripAdvanceCommand command =
        RegisterTripAdvanceCommandFromResourceAssembler.toCommand(
            TenantContext.tenantId(), TenantContext.userId(), tripId, resource);
    TripAdvance advance = settlementCommandService.handle(command);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(TripAdvanceResourceFromEntityAssembler.toResource(advance));
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Consultar los adelantos de un viaje (US35)",
      description =
          "Lista los adelantos registrados para el viaje, en orden de registro. Los ven el"
              + " responsable y el conductor de la empresa.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de adelantos, puede estar vacía"),
    @ApiResponse(
        responseCode = "404",
        description = "El viaje no existe para la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<List<TripAdvanceResource>> list(@PathVariable UUID tripId) {
    List<TripAdvanceResource> advances =
        settlementQueryService
            .handle(new GetTripAdvancesQuery(TenantContext.tenantId(), tripId))
            .stream()
            .map(TripAdvanceResourceFromEntityAssembler::toResource)
            .toList();
    return ResponseEntity.ok(advances);
  }
}
