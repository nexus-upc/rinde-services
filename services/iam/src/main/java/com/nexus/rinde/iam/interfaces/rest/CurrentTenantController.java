package com.nexus.rinde.iam.interfaces.rest;

import com.nexus.rinde.iam.domain.model.aggregates.Tenant;
import com.nexus.rinde.iam.domain.model.commands.UpdateCurrentTenantProfileCommand;
import com.nexus.rinde.iam.domain.model.queries.GetCurrentTenantProfileQuery;
import com.nexus.rinde.iam.domain.model.valueobjects.TenantId;
import com.nexus.rinde.iam.domain.services.TenantCommandService;
import com.nexus.rinde.iam.domain.services.TenantQueryService;
import com.nexus.rinde.iam.interfaces.rest.resources.CompanyProfileResource;
import com.nexus.rinde.iam.interfaces.rest.resources.UpdateCompanyProfileResource;
import com.nexus.rinde.shared.infrastructure.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfil de la empresa del usuario que inicia sesión. */
@RestController
@RequestMapping(value = "/api/v1/tenants/current", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tenants", description = "Registro, verificación y perfil de empresas")
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
public class CurrentTenantController {

  private final TenantQueryService tenantQueryService;
  private final TenantCommandService tenantCommandService;

  public CurrentTenantController(
      TenantQueryService tenantQueryService, TenantCommandService tenantCommandService) {
    this.tenantQueryService = tenantQueryService;
    this.tenantCommandService = tenantCommandService;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Consultar el perfil de la empresa actual",
      description = "Devuelve los datos públicos y de contacto de la empresa del token.")
  @ApiResponse(responseCode = "200", description = "Perfil de la empresa actual")
  public ResponseEntity<CompanyProfileResource> current() {
    Tenant tenant =
        tenantQueryService.handle(
            new GetCurrentTenantProfileQuery(new TenantId(TenantContext.tenantId())));
    return ResponseEntity.ok(toResource(tenant));
  }

  @PatchMapping
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @Operation(
      summary = "Actualizar los datos de la empresa actual",
      description = "Permite al administrador editar razón social, teléfono y dirección.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Empresa actualizada"),
    @ApiResponse(
        responseCode = "400",
        description = "Datos inválidos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<CompanyProfileResource> update(
      @Valid @RequestBody UpdateCompanyProfileResource resource) {
    Tenant updated =
        tenantCommandService.handle(
            new UpdateCurrentTenantProfileCommand(
                new TenantId(TenantContext.tenantId()),
                resource.tradeName(),
                resource.phone(),
                resource.address()));
    return ResponseEntity.ok(toResource(updated));
  }

  private static CompanyProfileResource toResource(Tenant tenant) {
    return new CompanyProfileResource(
        tenant.getId(),
        tenant.getTradeName(),
        tenant.getRuc().number(),
        tenant.getStatus(),
        tenant.getPhone(),
        tenant.getAddress());
  }
}
