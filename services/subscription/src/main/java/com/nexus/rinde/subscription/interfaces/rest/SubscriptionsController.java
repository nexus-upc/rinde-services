package com.nexus.rinde.subscription.interfaces.rest;

import com.nexus.rinde.configuration.OpenApiConfiguration;
import com.nexus.rinde.shared.infrastructure.security.TenantContext;
import com.nexus.rinde.subscription.domain.model.commands.ChoosePlanCommand;
import com.nexus.rinde.subscription.domain.model.commands.StartSubscriptionCheckoutCommand;
import com.nexus.rinde.subscription.domain.model.queries.GetCurrentSubscriptionQuery;
import com.nexus.rinde.subscription.domain.model.queries.GetSubscriptionByTenantQuery;
import com.nexus.rinde.subscription.domain.model.queries.ListActivePlansQuery;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentCheckout;
import com.nexus.rinde.subscription.domain.services.PaymentCommandService;
import com.nexus.rinde.subscription.domain.services.SubscriptionCommandService;
import com.nexus.rinde.subscription.domain.services.SubscriptionQueryService;
import com.nexus.rinde.subscription.interfaces.rest.resources.ChoosePlanResource;
import com.nexus.rinde.subscription.interfaces.rest.resources.PaymentCheckoutResource;
import com.nexus.rinde.subscription.interfaces.rest.resources.PlanResource;
import com.nexus.rinde.subscription.interfaces.rest.resources.SubscriptionResource;
import com.nexus.rinde.subscription.interfaces.rest.transform.PlanResourceFromEntityAssembler;
import com.nexus.rinde.subscription.interfaces.rest.transform.SubscriptionResourceFromDetailsAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints del catálogo compartido y las suscripciones por empresa. */
@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_SCHEME)
@Tag(name = "Subscriptions", description = "Planes, suscripción y pago de la empresa (US38 a US40)")
public class SubscriptionsController {

  private final SubscriptionCommandService commandService;
  private final SubscriptionQueryService queryService;
  private final PaymentCommandService paymentCommandService;
  private final boolean simulationEnabled;

  public SubscriptionsController(
      SubscriptionCommandService commandService,
      SubscriptionQueryService queryService,
      PaymentCommandService paymentCommandService,
      @Value("${rinde.billing.simulation.enabled:false}") boolean simulationEnabled) {
    this.commandService = commandService;
    this.queryService = queryService;
    this.paymentCommandService = paymentCommandService;
    this.simulationEnabled = simulationEnabled;
  }

  @GetMapping("/plans")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Listar planes activos",
      description = "Devuelve el catálogo compartido con precio mensual y límite de unidades.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Catálogo de planes activos"),
    @ApiResponse(
        responseCode = "401",
        description = "Falta el token o no es válido",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public List<PlanResource> listPlans() {
    return queryService.handle(new ListActivePlansQuery()).stream()
        .map(PlanResourceFromEntityAssembler::toResource)
        .toList();
  }

  @PostMapping("/subscriptions")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @Operation(
      summary = "Elegir un plan y crear una suscripción",
      description =
          "Crea una suscripción PENDING_PAYMENT para la empresa del token. No inicia un cobro ni"
              + " asigna vigencia pagada.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Suscripción pendiente creada"),
    @ApiResponse(
        responseCode = "400",
        description = "El cuerpo contiene datos inválidos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Falta el token o no es válido",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Solo un administrador puede elegir el plan",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "El plan no existe o no está activo",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "La flota excede el límite o ya hay una selección pendiente",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<SubscriptionResource> choosePlan(
      @Valid @RequestBody ChoosePlanResource resource) {
    var subscription =
        commandService.handle(new ChoosePlanCommand(TenantContext.tenantId(), resource.planId()));
    var details =
        queryService.handle(
            new GetSubscriptionByTenantQuery(TenantContext.tenantId(), subscription.getSubscriptionId()));
    return ResponseEntity.created(
            URI.create("/api/v1/subscriptions/" + subscription.getSubscriptionId()))
        .body(SubscriptionResourceFromDetailsAssembler.toResource(details));
  }

  @PostMapping("/subscriptions/{id}/checkout")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @Operation(
      summary = "Iniciar un checkout simulado",
      description =
          "Crea un intento pendiente con una referencia local. No recibe ni almacena datos de"
              + " tarjeta; el resultado se simula mediante el webhook firmado.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Checkout simulado creado"),
    @ApiResponse(responseCode = "200", description = "Se devuelve el checkout pendiente existente"),
    @ApiResponse(
        responseCode = "401",
        description = "Falta el token o no es válido",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Solo un administrador puede iniciar el checkout",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "La suscripción no existe en la empresa del token",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "La suscripción no admite pagos en su estado actual",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<PaymentCheckoutResource> checkout(@PathVariable UUID id) {
    PaymentCheckout checkout =
        paymentCommandService.handle(new StartSubscriptionCheckoutCommand(TenantContext.tenantId(), id));
    HttpStatus status = checkout.created() ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status)
        .body(
            new PaymentCheckoutResource(
                checkout.paymentId(),
                checkout.providerReference(),
                checkout.amount(),
                checkout.status(),
                checkout.simulated(), simulationEnabled));
  }

  @GetMapping("/subscriptions/current")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Consultar la suscripción actual de la empresa; 204 si aún no tiene una")
  public ResponseEntity<SubscriptionResource> currentSubscription() {
    return queryService.handle(new GetCurrentSubscriptionQuery(TenantContext.tenantId()))
        .map(details -> ResponseEntity.ok(SubscriptionResourceFromDetailsAssembler.toResource(details)))
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @GetMapping("/subscriptions/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Consultar una suscripción",
      description = "Devuelve estado, vigencia y plan solo si pertenecen a la empresa del token.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Suscripción consultada"),
    @ApiResponse(
        responseCode = "401",
        description = "Falta el token o no es válido",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "La suscripción no existe para la empresa autenticada",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public SubscriptionResource getSubscription(
      @Parameter(description = "Identificador de la suscripción") @PathVariable UUID id) {
    var details =
        queryService.handle(new GetSubscriptionByTenantQuery(TenantContext.tenantId(), id));
    return SubscriptionResourceFromDetailsAssembler.toResource(details);
  }
}
