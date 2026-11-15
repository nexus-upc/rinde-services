package com.nexus.rinde.subscription.interfaces.rest.resources;

import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Resultado de pago enviado por la pasarela simulada, firmado en X-RINDE-Signature. */
public record PaymentWebhookResource(
    @NotBlank @Size(max = 160) String providerEventId,
    @NotBlank @Size(max = 160) String providerReference,
    @NotNull PaymentStatus status,
    @NotBlank @Pattern(regexp = "^[0-9]{1,8}\\.[0-9]{2}$")
        @Schema(example = "37.50", description = "Importe como texto decimal de dos posiciones.")
        String amount,
    @Size(max = 500) String failureReason) {}
