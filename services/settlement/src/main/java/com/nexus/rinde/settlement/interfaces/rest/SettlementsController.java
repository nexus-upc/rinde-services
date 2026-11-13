package com.nexus.rinde.settlement.interfaces.rest;

import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.commands.CloseSettlementCommand;
import com.nexus.rinde.settlement.domain.model.commands.RecalculateSettlementCommand;
import com.nexus.rinde.settlement.domain.model.commands.RegisterAdvanceCommand;
import com.nexus.rinde.settlement.domain.model.queries.ExportSettlementQuery;
import com.nexus.rinde.settlement.domain.model.queries.GetSettlementByIdQuery;
import com.nexus.rinde.settlement.domain.model.queries.GetSettlementByTripIdQuery;
import com.nexus.rinde.settlement.domain.model.queries.SettlementExport;
import com.nexus.rinde.settlement.domain.model.valueobjects.ApprovedExpense;
import com.nexus.rinde.settlement.domain.services.SettlementCommandService;
import com.nexus.rinde.settlement.domain.services.SettlementQueryService;
import com.nexus.rinde.settlement.interfaces.rest.resources.RegisterAdvanceResource;
import com.nexus.rinde.settlement.interfaces.rest.resources.SettlementResource;
import com.nexus.rinde.settlement.interfaces.rest.transform.RegisterAdvanceCommandFromResourceAssembler;
import com.nexus.rinde.settlement.interfaces.rest.transform.SettlementResourceFromEntityAssembler;
import org.springframework.http.HttpHeaders;
import com.nexus.rinde.shared.infrastructure.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/** Operaciones de consulta y gestión de liquidaciones de viaje. */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Settlements", description = "Liquidaciones de viaje y anticipos")
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
public class SettlementsController {

  private final SettlementCommandService settlementCommandService;
  private final SettlementQueryService settlementQueryService;

  public SettlementsController(
      SettlementCommandService settlementCommandService,
      SettlementQueryService settlementQueryService) {
    this.settlementCommandService = settlementCommandService;
    this.settlementQueryService = settlementQueryService;
  }

  @PostMapping("/api/v1/settlements/advances")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Registrar anticipo al conductor (US35)",
      description = "Registra el monto anticipado al conductor para cubrir los gastos del viaje.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Anticipo registrado exitosamente"),
    @ApiResponse(
        responseCode = "400",
        description = "Monto menor o igual a cero o datos inválidos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "No existe liquidación para el viaje indicado",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<SettlementResource> registerAdvance(
      @Valid @RequestBody RegisterAdvanceResource resource) {
    RegisterAdvanceCommand command =
        RegisterAdvanceCommandFromResourceAssembler.toCommand(
            TenantContext.tenantId(), TenantContext.userId(), resource);
    Settlement settlement = settlementCommandService.handle(command);
    return ResponseEntity.ok(SettlementResourceFromEntityAssembler.toResource(settlement));
  }

  @GetMapping("/api/v1/settlements/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Consultar el detalle de una liquidación (US25)",
      description = "Devuelve los datos completos de una liquidación por su identificador.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Detalle de la liquidación"),
    @ApiResponse(
        responseCode = "404",
        description = "Liquidación no encontrada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<SettlementResource> getSettlementById(@PathVariable UUID id) {
    Settlement settlement =
        settlementQueryService.handle(
            new GetSettlementByIdQuery(TenantContext.tenantId(), id));
    return ResponseEntity.ok(SettlementResourceFromEntityAssembler.toResource(settlement));
  }

  @GetMapping("/api/v1/trips/{tripId}/settlement")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Consultar la liquidación de un viaje (US28)",
      description = "Devuelve la liquidación asociada al viaje especificado.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liquidación del viaje"),
    @ApiResponse(
        responseCode = "404",
        description = "No existe liquidación para este viaje",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<SettlementResource> getSettlementByTrip(@PathVariable UUID tripId) {
    Settlement settlement =
        settlementQueryService.handle(
            new GetSettlementByTripIdQuery(TenantContext.tenantId(), tripId));
    return ResponseEntity.ok(SettlementResourceFromEntityAssembler.toResource(settlement));
  }

  @PostMapping("/api/v1/settlements/{id}/recalculation")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Recalcular totales de la liquidación (US25)",
      description =
          "Vuelve a sumar los gastos aprobados del viaje y actualiza el balance del anticipo.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liquidación recalculada"),
    @ApiResponse(
        responseCode = "404",
        description = "Liquidación no encontrada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "La liquidación ya fue cerrada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<SettlementResource> recalculate(@PathVariable UUID id) {
    Settlement settlement =
        settlementCommandService.handle(
            new RecalculateSettlementCommand(TenantContext.tenantId(), id));
    return ResponseEntity.ok(SettlementResourceFromEntityAssembler.toResource(settlement));
  }

  @PostMapping("/api/v1/settlements/{id}/close")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Cerrar la liquidación (US26)",
      description =
          "Cierra la liquidación: suma los gastos aprobados, calcula el balance y publica"
              + " SettlementClosed para que Trip la marque como liquidada y Expense bloquee los gastos.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liquidación cerrada"),
    @ApiResponse(
        responseCode = "404",
        description = "Liquidación no encontrada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "La liquidación ya fue cerrada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<SettlementResource> close(@PathVariable UUID id) {
    Settlement settlement =
        settlementCommandService.handle(
            new CloseSettlementCommand(TenantContext.tenantId(), id, TenantContext.userId()));
    return ResponseEntity.ok(SettlementResourceFromEntityAssembler.toResource(settlement));
  }

  @GetMapping(value = "/api/v1/settlements/{id}/export", produces = "text/csv")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Exportar la liquidación como CSV (US27)",
      description = "Descarga el detalle de la liquidación y sus gastos aprobados en formato CSV.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "CSV generado"),
    @ApiResponse(
        responseCode = "404",
        description = "Liquidación no encontrada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<String> export(@PathVariable UUID id) {
    SettlementExport export =
        settlementQueryService.handle(new ExportSettlementQuery(TenantContext.tenantId(), id));
    String csv = buildCsv(export);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=settlement-" + id + ".csv")
        .body(csv);
  }

  private String buildCsv(SettlementExport export) {
    Settlement s = export.settlement();
    StringBuilder sb = new StringBuilder();
    sb.append("campo,valor\n");
    sb.append("settlementId,").append(s.getId()).append('\n');
    sb.append("tripId,").append(s.getTripId()).append('\n');
    sb.append("driverId,").append(s.getDriverId()).append('\n');
    sb.append("status,").append(s.getStatus()).append('\n');
    sb.append("advance,")
        .append(s.getAdvanceAmount() != null ? s.getAdvanceAmount() : "")
        .append(' ')
        .append(s.getAdvanceCurrency() != null ? s.getAdvanceCurrency() : "")
        .append('\n');
    sb.append("expenseTotal,")
        .append(s.getExpenseTotalAmount() != null ? s.getExpenseTotalAmount() : "")
        .append(' ')
        .append(s.getExpenseTotalCurrency() != null ? s.getExpenseTotalCurrency() : "")
        .append('\n');
    sb.append("advanceBalance,")
        .append(s.getAdvanceBalance() != null ? s.getAdvanceBalance() : "")
        .append('\n');
    sb.append("closedAt,").append(s.getClosedAt() != null ? s.getClosedAt() : "").append('\n');
    sb.append("closedBy,").append(s.getClosedBy() != null ? s.getClosedBy() : "").append('\n');
    sb.append('\n');
    sb.append("expenseId,category,amount,currency,date,status\n");
    for (ApprovedExpense e : export.approvedExpenses()) {
      sb.append(e.expenseId())
          .append(',')
          .append(e.category())
          .append(',')
          .append(e.amount())
          .append(',')
          .append(e.currency())
          .append(',')
          .append(e.expenseDate())
          .append(',')
          .append(e.status())
          .append('\n');
    }
    return sb.toString();
  }
}
