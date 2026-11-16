package com.nexus.rinde.expense.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Representación pública del comprobante de un gasto, sin la ruta física de su archivo. */
public record EvidenceResource(
    @Schema(example = "7d1c2f0e-3b8a-4d55-9a61-2e0f4c1b9a10") UUID id,
    @Schema(example = "recibo-combustible.jpg") String fileName,
    @Schema(example = "image/jpeg") String contentType,
    @Schema(example = "204800") Long fileSizeBytes,
    @Schema(example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        String sha256,
    @Schema(example = "/api/v1/expenses/3c589391-d520-4d01-838a-1a574c7d2bb6/evidences/7d1c2f0e-3b8a-4d55-9a61-2e0f4c1b9a10/content",
        nullable = true)
        String contentUrl,
    Instant uploadedAt) {}
