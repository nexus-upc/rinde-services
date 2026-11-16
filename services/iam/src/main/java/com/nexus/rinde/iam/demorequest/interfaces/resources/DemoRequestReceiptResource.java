package com.nexus.rinde.iam.demorequest.interfaces.resources;

import com.nexus.rinde.iam.demorequest.domain.DemoRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Confirmación que recibe la landing tras registrar la solicitud. */
public record DemoRequestReceiptResource(
    @Schema(description = "Identificador de la solicitud") UUID id,
    @Schema(description = "Estado de la solicitud", example = "RECEIVED") String status,
    @Schema(description = "Momento en que se recibió la solicitud (UTC)") Instant receivedAt) {

  public static DemoRequestReceiptResource from(DemoRequest request) {
    return new DemoRequestReceiptResource(
        request.id(), request.status().name(), request.receivedAt());
  }
}
