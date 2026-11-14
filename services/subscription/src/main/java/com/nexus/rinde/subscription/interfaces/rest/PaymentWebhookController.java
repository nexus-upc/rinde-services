package com.nexus.rinde.subscription.interfaces.rest;

import com.nexus.rinde.configuration.OpenApiConfiguration;
import com.nexus.rinde.subscription.domain.model.commands.ProcessPaymentWebhookCommand;
import com.nexus.rinde.subscription.domain.services.PaymentCommandService;
import com.nexus.rinde.subscription.infrastructure.services.PaymentWebhookSignature;
import com.nexus.rinde.subscription.interfaces.rest.resources.PaymentWebhookResource;
import com.nexus.rinde.subscription.interfaces.rest.resources.PaymentWebhookResultResource;
import com.nexus.rinde.subscription.interfaces.rest.transform.PaymentWebhookResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Recibe el resultado firmado que entrega la pasarela simulada de pagos. */
@RestController
@RequestMapping("/api/v1/billing/webhooks")
@Tag(name = "Billing", description = "Webhook de pago simulado para US39")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_SCHEME)
public class PaymentWebhookController {

  private final PaymentCommandService paymentCommandService;
  private final PaymentWebhookSignature paymentWebhookSignature;

  public PaymentWebhookController(
      PaymentCommandService paymentCommandService, PaymentWebhookSignature paymentWebhookSignature) {
    this.paymentCommandService = paymentCommandService;
    this.paymentWebhookSignature = paymentWebhookSignature;
  }

  @PostMapping("/payment")
  @SecurityRequirements
  @Operation(
      summary = "Procesar el resultado de un pago simulado",
      description =
          "No usa JWT: valida X-RINDE-Signature con HMAC-SHA256 y procesa cada providerEventId"
              + " una sola vez.")
  public ResponseEntity<PaymentWebhookResultResource> receivePayment(
      @RequestHeader("X-RINDE-Signature") String signature,
      @Valid @RequestBody PaymentWebhookResource resource) {
    paymentWebhookSignature.verify(resource, signature);
    var result =
        paymentCommandService.handle(
            new ProcessPaymentWebhookCommand(
                resource.providerEventId(),
                resource.providerReference(),
                resource.status(),
                resource.amount(),
                resource.failureReason()));
    return ResponseEntity.ok(PaymentWebhookResultAssembler.toResource(result));
  }
}
