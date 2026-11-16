package com.nexus.rinde.subscription.interfaces.rest;

import com.nexus.rinde.shared.infrastructure.security.TenantContext;
import com.nexus.rinde.subscription.domain.model.commands.SimulatePaymentCommand;
import com.nexus.rinde.subscription.domain.model.queries.GetSubscriptionByTenantQuery;
import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;
import com.nexus.rinde.subscription.domain.services.PaymentCommandService;
import com.nexus.rinde.subscription.domain.services.SubscriptionQueryService;
import com.nexus.rinde.subscription.interfaces.rest.resources.SubscriptionResource;
import com.nexus.rinde.subscription.interfaces.rest.transform.SubscriptionResourceFromDetailsAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Resultado de pago de prueba, autorizado por empresa; no entrega la clave HMAC al navegador. */
@RestController
@ConditionalOnProperty(name = "rinde.billing.simulation.enabled", havingValue = "true")
@RequestMapping("/api/v1/subscriptions/{subscriptionId}/payments/{paymentId}/simulation")
@Tag(name = "Subscriptions", description = "Planes, suscripción y pago de la empresa (US38 a US40)")
public class SubscriptionSimulationController {
  private final PaymentCommandService commands;
  private final SubscriptionQueryService queries;

  public SubscriptionSimulationController(PaymentCommandService commands,
      SubscriptionQueryService queries) {
    this.commands = commands;
    this.queries = queries;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @Operation(summary = "Confirmar o rechazar un pago simulado propio; solo con el modo de pruebas activo")
  public SubscriptionResource simulate(@PathVariable UUID subscriptionId, @PathVariable UUID paymentId,
      @Valid @RequestBody SimulationResource resource) {
    UUID tenantId = TenantContext.tenantId();
    commands.handle(new SimulatePaymentCommand(tenantId, subscriptionId, paymentId, resource.status()));
    return SubscriptionResourceFromDetailsAssembler.toResource(
        queries.handle(new GetSubscriptionByTenantQuery(tenantId, subscriptionId)));
  }

  public record SimulationResource(@NotNull PaymentStatus status) {}
}
