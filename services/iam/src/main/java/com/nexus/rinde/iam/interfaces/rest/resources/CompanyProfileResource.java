package com.nexus.rinde.iam.interfaces.rest.resources;

import com.nexus.rinde.iam.domain.model.valueobjects.TenantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Perfil de empresa actual incluyendo teléfono y dirección de contacto. */
public record CompanyProfileResource(
    @Schema(example = "3f6c1b9e-8a2d-4c57-9a43-0d2f6b1e7a10") UUID id,
    @Schema(example = "Transportes Andes SAC") String tradeName,
    @Schema(example = "20123456789") String ruc,
    @Schema(example = "ACTIVE") TenantStatus status,
    @Schema(nullable = true, example = "+51 1 445 8899") String phone,
    @Schema(nullable = true, example = "Av. Argentina 2450, Lima") String address) {}
