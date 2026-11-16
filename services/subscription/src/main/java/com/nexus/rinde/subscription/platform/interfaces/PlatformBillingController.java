package com.nexus.rinde.subscription.platform.interfaces;

import com.nexus.rinde.shared.infrastructure.security.*;
import com.nexus.rinde.shared.interfaces.rest.PageResource;
import com.nexus.rinde.subscription.platform.application.PlatformPlanService;
import com.nexus.rinde.subscription.platform.infrastructure.PlatformBillingQueries;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/platform")
@PreAuthorize("hasAuthority('ROLE_PLATFORM_OWNER')")
@Tag(name = "Platform billing", description = "Catálogo, suscripciones, pagos y auditoría para el dueño de Rinde (US43 a US45)")
public class PlatformBillingController {
  private final PlatformPlanService plans;
  private final PlatformBillingQueries queries;

  public PlatformBillingController(PlatformPlanService plans, PlatformBillingQueries queries) {
    this.plans = plans;
    this.queries = queries;
  }

  @Operation(summary = "Resumen comercial de la plataforma (US44)")
  @GetMapping("/billing/overview")
  public PlatformBillingQueries.Overview overview() {
    return queries.overview();
  }

  @Operation(summary = "Listar los planes del catálogo (US43)")
  @GetMapping("/plans")
  public PageResource<PlatformPlanService.PlanRow> plans(
      @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return plans.list(page, size);
  }

  @Operation(summary = "Crear un plan con motivo (US43)")
  @PostMapping("/plans")
  @ResponseStatus(HttpStatus.CREATED)
  public PlatformPlanService.PlanRow create(
      @Valid @RequestBody CreatePlan body, @AuthenticationPrincipal PlatformPrincipal principal) {
    return plans.create(
        body.name().trim(), body.monthlyPrice(), body.unitLimit(), body.reason().trim(), principal);
  }

  @Operation(summary = "Retirar o volver a publicar un plan con motivo (US43)")
  @PatchMapping("/plans/{id}/availability")
  public PlatformPlanService.PlanRow availability(
      @PathVariable UUID id,
      @Valid @RequestBody Availability body,
      @AuthenticationPrincipal PlatformPrincipal principal) {
    return plans.availability(id, body.active(), body.reason().trim(), principal);
  }

  @Operation(summary = "Consultar las suscripciones de las empresas (US44)")
  @GetMapping("/subscriptions")
  public PageResource<PlatformBillingQueries.SubscriptionRow> subscriptions(
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return queries.subscriptions(tenantId, page, size);
  }

  @Operation(summary = "Consultar los pagos de prueba de las empresas (US44)")
  @GetMapping("/payments")
  public PageResource<PlatformBillingQueries.PaymentRow> payments(
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return queries.payments(tenantId, page, size);
  }

  @Operation(summary = "Auditoría de cambios del catálogo (US45)")
  @GetMapping("/audit/billing")
  public PageResource<PlatformAuditTrail.Entry> audit(
      @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return plans.audit(page, size);
  }

  public record CreatePlan(
      @NotBlank @Size(max = 80) String name,
      @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal monthlyPrice,
      @NotNull @Min(1) @Max(100000) Integer unitLimit,
      @NotBlank @Size(min = 3, max = 500) String reason) {}

  public record Availability(
      @NotNull Boolean active, @NotBlank @Size(min = 3, max = 500) String reason) {}
}
