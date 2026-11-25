package com.nexus.rinde.iam.interfaces.rest;

import com.nexus.rinde.iam.domain.model.aggregates.Tenant;
import com.nexus.rinde.iam.domain.model.aggregates.User;
import com.nexus.rinde.iam.domain.model.commands.ChangeCurrentUserPasswordCommand;
import com.nexus.rinde.iam.domain.model.commands.UpdateCurrentUserProfileCommand;
import com.nexus.rinde.iam.domain.model.queries.GetTenantByIdQuery;
import com.nexus.rinde.iam.domain.model.queries.GetUserByIdQuery;
import com.nexus.rinde.iam.domain.model.valueobjects.TenantId;
import com.nexus.rinde.iam.domain.model.valueobjects.UserId;
import com.nexus.rinde.iam.domain.services.TenantQueryService;
import com.nexus.rinde.iam.domain.services.UserCommandService;
import com.nexus.rinde.iam.domain.services.UserQueryService;
import com.nexus.rinde.iam.interfaces.rest.resources.ChangePasswordResource;
import com.nexus.rinde.iam.interfaces.rest.resources.CurrentUserResource;
import com.nexus.rinde.iam.interfaces.rest.resources.UpdateCurrentUserResource;
import com.nexus.rinde.iam.interfaces.rest.transform.CurrentUserResourceFromEntityAssembler;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perfil del usuario que inicia sesión, para cualquier rol. Solo usa el usuario y la empresa del
 * token, por eso no recibe identificadores en la ruta.
 */
@RestController
@RequestMapping(value = "/api/v1/users/me", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Perfil del usuario que inicia sesión, para cualquier rol")
public class CurrentUserController {

  private final UserQueryService userQueryService;
  private final TenantQueryService tenantQueryService;
  private final UserCommandService userCommandService;

  public CurrentUserController(
      UserQueryService userQueryService,
      TenantQueryService tenantQueryService,
      UserCommandService userCommandService) {
    this.userQueryService = userQueryService;
    this.tenantQueryService = tenantQueryService;
    this.userCommandService = userCommandService;
  }

  @GetMapping
  @Operation(
      summary = "Consultar el perfil del usuario que inicia sesión",
      description = "Devuelve sus datos y los de su empresa. La web lo usa para mostrar la sesión.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Perfil del usuario y de su empresa"),
    @ApiResponse(
        responseCode = "401",
        description = "Falta el token o no es válido",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "El usuario o su empresa ya no existen",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public CurrentUserResource me() {
    TenantId tenantId = new TenantId(TenantContext.tenantId());
    User user =
        userQueryService
            .handle(new GetUserByIdQuery(tenantId, new UserId(TenantContext.userId())))
            .orElseThrow(() -> new ResourceNotFoundException("El usuario ya no existe."));
    Tenant tenant =
        tenantQueryService
            .handle(new GetTenantByIdQuery(tenantId))
            .orElseThrow(() -> new ResourceNotFoundException("La empresa ya no existe."));
    return CurrentUserResourceFromEntityAssembler.toResource(user, tenant);
  }

  @PatchMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Actualizar el nombre completo del usuario actual",
      description = "Permite a cualquier usuario autenticado actualizar su nombre visible.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Perfil del usuario actualizado"),
    @ApiResponse(
        responseCode = "400",
        description = "Datos inválidos",
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
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<CurrentUserResource> updateProfile(
      @Valid @RequestBody UpdateCurrentUserResource resource) {
    TenantId tenantId = new TenantId(TenantContext.tenantId());
    User updated =
        userCommandService.handle(
            new UpdateCurrentUserProfileCommand(
                tenantId, new UserId(TenantContext.userId()), resource.fullName()));
    Tenant tenant =
        tenantQueryService
            .handle(new GetTenantByIdQuery(tenantId))
            .orElseThrow(() -> new ResourceNotFoundException("La empresa ya no existe."));
    return ResponseEntity.ok(CurrentUserResourceFromEntityAssembler.toResource(updated, tenant));
  }

  @PostMapping("/password")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Cambiar la contraseña del usuario actual",
      description = "Valida la contraseña vigente y registra una nueva contraseña para la cuenta.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Contraseña actualizada"),
    @ApiResponse(
        responseCode = "400",
        description = "Contraseña actual incorrecta o nueva contraseña inválida",
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
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordResource resource) {
    userCommandService.handle(
        new ChangeCurrentUserPasswordCommand(
            new TenantId(TenantContext.tenantId()),
            new UserId(TenantContext.userId()),
            resource.currentPassword(),
            resource.newPassword()));
    return ResponseEntity.noContent().build();
  }
}
