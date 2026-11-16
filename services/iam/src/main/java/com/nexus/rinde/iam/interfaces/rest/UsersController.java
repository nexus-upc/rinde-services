package com.nexus.rinde.iam.interfaces.rest;

import com.nexus.rinde.iam.domain.model.aggregates.User;
import com.nexus.rinde.iam.domain.model.queries.GetUsersByTenantQuery;
import com.nexus.rinde.iam.domain.model.valueobjects.Role;
import com.nexus.rinde.iam.domain.model.valueobjects.TenantId;
import com.nexus.rinde.iam.domain.model.valueobjects.UserId;
import com.nexus.rinde.iam.domain.services.UserCommandService;
import com.nexus.rinde.iam.domain.services.UserQueryService;
import com.nexus.rinde.iam.interfaces.rest.resources.InviteUserResource;
import com.nexus.rinde.iam.interfaces.rest.resources.UpdateUserResource;
import com.nexus.rinde.iam.interfaces.rest.resources.UserResource;
import com.nexus.rinde.iam.interfaces.rest.transform.InviteUserCommandFromResourceAssembler;
import com.nexus.rinde.iam.interfaces.rest.transform.UpdateUserCommandFromResourceAssembler;
import com.nexus.rinde.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.infrastructure.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión de los usuarios de la empresa (US11). El listado por rol lo puede usar también el
 * responsable de operaciones, solo para las cuentas DRIVER. La empresa siempre sale del token.
 */
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Usuarios de la empresa (US11)")
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
      description = "El rol no puede usar esta operación o pidió un rol no permitido",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
})
public class UsersController {

  private final UserCommandService userCommandService;
  private final UserQueryService userQueryService;

  public UsersController(UserCommandService userCommandService, UserQueryService userQueryService) {
    this.userCommandService = userCommandService;
    this.userQueryService = userQueryService;
  }

  @GetMapping
  @PreAuthorize(
      "hasRole('ADMINISTRATOR') or (hasRole('OPERATIONS_MANAGER') and #role == 'DRIVER')")
  @Operation(
      summary = "Listar los usuarios de la empresa, opcionalmente por rol",
      description =
          "El administrador lista todos los usuarios o los de un rol. El responsable de"
              + " operaciones solo puede pedir role=DRIVER, para elegir la cuenta de un conductor.")
  @ApiResponse(responseCode = "200", description = "Usuarios de la empresa del solicitante")
  @ApiResponse(
      responseCode = "400",
      description = "El rol indicado no existe",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public List<UserResource> list(@RequestParam(name = "role", required = false) String role) {
    Role filter = role == null ? null : parseRole(role);
    return userQueryService
        .handle(new GetUsersByTenantQuery(new TenantId(TenantContext.tenantId()), filter))
        .stream()
        .map(UserResourceFromEntityAssembler::toResource)
        .toList();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @Operation(
      summary = "Invitar a un usuario con su rol",
      description =
          "Crea al usuario en estado INVITED. El enlace de invitación (vigente 48 horas) se"
              + " registra en el log del servidor.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Usuario invitado"),
    @ApiResponse(
        responseCode = "400",
        description = "Datos inválidos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "El correo ya está registrado",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<UserResource> invite(@Valid @RequestBody InviteUserResource resource) {
    User user =
        userCommandService.handle(
            InviteUserCommandFromResourceAssembler.toCommand(
                new TenantId(TenantContext.tenantId()), resource));
    return ResponseEntity.created(URI.create("/api/v1/users/" + user.getId()))
        .body(UserResourceFromEntityAssembler.toResource(user));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  @Operation(
      summary = "Cambiar el rol o deshabilitar al usuario",
      description = "Acepta un nuevo rol, el estado DISABLED, o ambos.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
    @ApiResponse(
        responseCode = "400",
        description = "Datos inválidos o cambio no permitido",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "El usuario no existe en la empresa del administrador",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public UserResource update(@PathVariable UUID id, @RequestBody UpdateUserResource resource) {
    User user =
        userCommandService.handle(
            UpdateUserCommandFromResourceAssembler.toCommand(
                new TenantId(TenantContext.tenantId()), new UserId(id), resource));
    return UserResourceFromEntityAssembler.toResource(user);
  }

  private static Role parseRole(String role) {
    try {
      return Role.valueOf(role);
    } catch (IllegalArgumentException ex) {
      throw new BusinessRuleException("El rol indicado no existe.");
    }
  }
}
