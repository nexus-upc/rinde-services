package com.nexus.rinde.expense.interfaces.rest;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.commands.AttachEvidenceCommand;
import com.nexus.rinde.expense.domain.model.commands.RegisterExpenseCommand;
import com.nexus.rinde.expense.domain.model.commands.UpdateExpenseStatusCommand;
import com.nexus.rinde.expense.domain.model.queries.GetEvidenceContentQuery;
import com.nexus.rinde.expense.domain.model.queries.GetExpenseByIdQuery;
import com.nexus.rinde.expense.domain.model.queries.GetExpensesByTripQuery;
import com.nexus.rinde.expense.domain.model.valueobjects.EvidenceContent;
import com.nexus.rinde.expense.domain.model.valueobjects.SyncExpensesResult;
import com.nexus.rinde.expense.domain.services.ExpenseCommandService;
import com.nexus.rinde.expense.domain.services.ExpenseQueryService;
import com.nexus.rinde.expense.domain.model.valueobjects.Money;
import com.nexus.rinde.expense.interfaces.rest.resources.ExpenseResource;
import com.nexus.rinde.expense.interfaces.rest.resources.RegisterExpenseResource;
import com.nexus.rinde.expense.interfaces.rest.resources.SyncExpenseItemResource;
import com.nexus.rinde.expense.interfaces.rest.resources.SyncExpensesResponseResource;
import com.nexus.rinde.expense.interfaces.rest.resources.SyncRejectedItemResource;
import com.nexus.rinde.expense.interfaces.rest.resources.UpdateExpenseStatusResource;
import com.nexus.rinde.expense.interfaces.rest.transform.ExpenseResourceFromEntityAssembler;
import com.nexus.rinde.expense.interfaces.rest.transform.RegisterExpenseCommandFromResourceAssembler;
import com.nexus.rinde.expense.interfaces.rest.transform.UpdateExpenseStatusCommandFromResourceAssembler;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.infrastructure.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Operaciones para registro, consulta y auditoría de gastos operativos y comprobantes. */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Expenses", description = "Registro de gastos, comprobantes y revisión")
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
public class ExpensesController {

  private final ExpenseCommandService expenseCommandService;
  private final ExpenseQueryService expenseQueryService;

  public ExpensesController(
      ExpenseCommandService expenseCommandService, ExpenseQueryService expenseQueryService) {
    this.expenseCommandService = expenseCommandService;
    this.expenseQueryService = expenseQueryService;
  }

  @PostMapping("/api/v1/trips/{tripId}/expenses")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Registrar un gasto operativo (US21)",
      description =
          "Registra un gasto vinculado a un viaje iniciado o finalizado. Nace en PENDING_SUPPORT;"
              + " pasa a REGISTERED al subir su comprobante con POST"
              + " /api/v1/expenses/{expenseId}/evidences.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Gasto registrado exitosamente"),
    @ApiResponse(
        responseCode = "400",
        description = "Monto menor o igual a cero o datos inválidos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Clave de idempotencia duplicada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<ExpenseResource> registerExpense(
      @PathVariable UUID tripId, @Valid @RequestBody RegisterExpenseResource resource) {
    RegisterExpenseCommand command =
        RegisterExpenseCommandFromResourceAssembler.toCommand(
            TenantContext.tenantId(), tripId, TenantContext.userId(), resource);
    Expense expense = expenseCommandService.handle(command);
    return ResponseEntity.created(URI.create("/api/v1/expenses/" + expense.getId()))
        .body(ExpenseResourceFromEntityAssembler.toResource(expense));
  }

  @GetMapping("/api/v1/trips/{tripId}/expenses")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Consultar los gastos de un viaje (US23)",
      description = "Devuelve el listado de gastos operativos registrados para el viaje especificado.")
  @ApiResponse(responseCode = "200", description = "Lista de gastos del viaje")
  public ResponseEntity<List<ExpenseResource>> getExpensesByTrip(@PathVariable UUID tripId) {
    List<Expense> expenses =
        expenseQueryService.handle(
            new GetExpensesByTripQuery(
                TenantContext.tenantId(),
                tripId,
                TenantContext.userId(),
                TenantContext.role()));
    return ResponseEntity.ok(
        expenses.stream().map(ExpenseResourceFromEntityAssembler::toResource).toList());
  }

  @PatchMapping("/api/v1/expenses/{id}/status")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Aprobar u observar un gasto (US24)",
      description =
          "Permite al responsable u operador aprobar un gasto o registrar observaciones"
              + " por inconsistencias.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Estado del gasto actualizado"),
    @ApiResponse(
        responseCode = "400",
        description = "Motivo de observación faltante o estado inválido",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Gasto no encontrado en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "El gasto está bloqueado por liquidación cerrada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<ExpenseResource> updateStatus(
      @PathVariable UUID id, @Valid @RequestBody UpdateExpenseStatusResource resource) {
    UpdateExpenseStatusCommand command =
        UpdateExpenseStatusCommandFromResourceAssembler.toCommand(
            TenantContext.tenantId(), id, TenantContext.userId(), resource);
    Expense expense = expenseCommandService.handle(command);
    return ResponseEntity.ok(ExpenseResourceFromEntityAssembler.toResource(expense));
  }

  @GetMapping("/api/v1/expenses/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Consultar el detalle de un gasto (US17)",
      description =
          "Devuelve los datos completos del gasto y los metadatos de su comprobante. El archivo se"
              + " lee con contentUrl.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Detalle del gasto"),
    @ApiResponse(
        responseCode = "404",
        description = "Gasto no encontrado en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<ExpenseResource> getExpenseById(@PathVariable UUID id) {
    Expense expense =
        expenseQueryService.handle(new GetExpenseByIdQuery(TenantContext.tenantId(), id));
    return ResponseEntity.ok(ExpenseResourceFromEntityAssembler.toResource(expense));
  }

  @PostMapping(
      value = "/api/v1/expenses/{expenseId}/evidences",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('DRIVER')")
  @Operation(
      summary = "Adjuntar o reemplazar el comprobante de un gasto (US21, QA-05)",
      description =
          "Solo el conductor que registró el gasto sube su comprobante en el campo file de un"
              + " formulario multipart. Se aceptan JPEG, PNG, WEBP o PDF, hasta el tamaño máximo"
              + " configurado. Un gasto pendiente de sustento pasa a REGISTERED. Si ya tenía"
              + " comprobante, el nuevo lo reemplaza.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Comprobante guardado. Devuelve el gasto con su comprobante actualizado"),
    @ApiResponse(
        responseCode = "400",
        description = "Falta el archivo, está vacío o su formato no está permitido",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Solo el conductor que registró el gasto puede adjuntar su comprobante",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Gasto no encontrado en la empresa",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "El gasto está bloqueado por liquidación cerrada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "413",
        description = "El archivo supera el tamaño máximo permitido",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<ExpenseResource> uploadEvidence(
      @PathVariable UUID expenseId,
      @RequestPart(value = "file", required = false) MultipartFile file)
      throws IOException {
    if (file == null) {
      throw new BusinessRuleException("Adjunte el comprobante en el campo file.");
    }
    Expense expense =
        expenseCommandService.handle(
            new AttachEvidenceCommand(
                TenantContext.tenantId(),
                expenseId,
                TenantContext.userId(),
                file.getOriginalFilename(),
                file.getBytes()));
    String location =
        "/api/v1/expenses/%s/evidences/%s/content"
            .formatted(expense.getId(), expense.getEvidence().getId());
    return ResponseEntity.created(URI.create(location))
        .body(ExpenseResourceFromEntityAssembler.toResource(expense));
  }

  @GetMapping(
      value = "/api/v1/expenses/{expenseId}/evidences/{evidenceId}/content",
      produces = MediaType.ALL_VALUE)
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Leer el archivo de un comprobante (US21, QA-05)",
      description =
          "Devuelve el archivo con su tipo de contenido. Lo leen el administrador y el responsable"
              + " de operaciones de la empresa, y el conductor que registró el gasto. Requiere"
              + " JWT; el enlace no es público.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Archivo del comprobante con su Content-Type"),
    @ApiResponse(
        responseCode = "403",
        description = "El usuario no puede ver el comprobante de este gasto",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Gasto o comprobante no encontrado en la empresa, o sin archivo",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<byte[]> getEvidenceContent(
      @PathVariable UUID expenseId, @PathVariable UUID evidenceId) {
    EvidenceContent evidence =
        expenseQueryService.handle(
            new GetEvidenceContentQuery(
                TenantContext.tenantId(),
                expenseId,
                evidenceId,
                TenantContext.userId(),
                TenantContext.role()));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(evidence.contentType()))
        .contentLength(evidence.content().length)
        .cacheControl(CacheControl.noStore())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline()
                .filename(evidence.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(evidence.content());
  }

  @PostMapping("/api/v1/expenses/sync")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Sincronizar lote de gastos registrados sin conexión (US22)",
      description =
          "Permite a la aplicación móvil enviar un lote de gastos capturados offline. El proceso"
              + " es idempotente y garantiza consistencia eventual en la rendición. Cada gasto"
              + " guardado nace sin comprobante; su foto se sube después con su id.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description =
            "Lote procesado. Los gastos rechazados por reglas de negocio van en rejected y no"
                + " impiden guardar los demás"),
    @ApiResponse(
        responseCode = "400",
        description = "Datos inválidos en el lote de gastos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<SyncExpensesResponseResource> syncExpenses(
      @RequestBody @Valid List<SyncExpenseItemResource> items) {
    if (items == null || items.isEmpty()) {
      return ResponseEntity.ok(new SyncExpensesResponseResource(0, 0, List.of(), 0, List.of()));
    }
    List<RegisterExpenseCommand> commands =
        items.stream()
            .map(
                item ->
                    new RegisterExpenseCommand(
                        TenantContext.tenantId(),
                        item.tripId(),
                        TenantContext.userId(),
                        item.category(),
                        new Money(item.amount(), item.currency()),
                        item.expenseDate(),
                        item.idempotencyKey()))
            .toList();

    SyncExpensesResult result = expenseCommandService.handleSync(commands);
    List<ExpenseResource> resources =
        result.synchronizedExpenses().stream()
            .map(ExpenseResourceFromEntityAssembler::toResource)
            .toList();
    List<SyncRejectedItemResource> rejected =
        result.rejected().stream()
            .map(
                item ->
                    new SyncRejectedItemResource(
                        item.idempotencyKey(), item.reason(), item.code().name()))
            .toList();

    return ResponseEntity.ok(
        new SyncExpensesResponseResource(
            items.size(), resources.size(), resources, rejected.size(), rejected));
  }
}
