package com.nexus.rinde.iam.demorequest.interfaces.resources;

import com.nexus.rinde.iam.demorequest.domain.DemoRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Solicitud de demostración tal como la ve el dueño de Rinde. */
public record DemoRequestSummaryResource(
    @Schema(description = "Identificador de la solicitud") UUID id,
    @Schema(description = "Nombre de quien solicita") String fullName,
    @Schema(description = "Nombre de la empresa") String companyName,
    @Schema(description = "Correo de contacto, en minúsculas") String email,
    @Schema(description = "Teléfono de contacto, si se indicó") String phone,
    @Schema(description = "Cantidad de unidades de la flota") int fleetSize,
    @Schema(description = "Comentario, si se indicó") String message,
    @Schema(description = "Estado de la solicitud", example = "RECEIVED") String status,
    @Schema(description = "Momento en que se recibió la solicitud (UTC)") Instant receivedAt) {

  public static DemoRequestSummaryResource from(DemoRequest request) {
    return new DemoRequestSummaryResource(
        request.id(),
        request.fullName(),
        request.companyName(),
        request.email(),
        request.phone(),
        request.fleetSize(),
        request.message(),
        request.status().name(),
        request.receivedAt());
  }
}
