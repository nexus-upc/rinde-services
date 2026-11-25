package com.nexus.rinde.notification.interfaces.rest;

import com.nexus.rinde.notification.domain.model.commands.RegisterDriverDeviceCommand;
import com.nexus.rinde.notification.domain.model.commands.RemoveDriverDeviceCommand;
import com.nexus.rinde.notification.domain.services.DriverDeviceCommandService;
import com.nexus.rinde.notification.interfaces.rest.resources.RegisterDeviceResource;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Registro y baja de dispositivos móviles del conductor para notificaciones push. */
@RestController
@RequestMapping(value = "/api/v1/notifications/devices", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Dispositivos push y centro de notificaciones")
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
public class DriverDevicesController {

  private final DriverDeviceCommandService commandService;

  public DriverDevicesController(DriverDeviceCommandService commandService) {
    this.commandService = commandService;
  }

  @PostMapping
  @PreAuthorize("hasRole('DRIVER')")
  @Operation(
      summary = "Registrar o actualizar dispositivo del conductor",
      description =
          "Asocia de forma idempotente el token FCM al conductor autenticado para recibir avisos.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Dispositivo registrado"),
    @ApiResponse(
        responseCode = "400",
        description = "Token o plataforma inválidos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<Void> register(@Valid @RequestBody RegisterDeviceResource resource) {
    commandService.handle(
        new RegisterDriverDeviceCommand(
            TenantContext.tenantId(),
            TenantContext.userId(),
            resource.token(),
            resource.platform()));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{token}")
  @PreAuthorize("hasRole('DRIVER')")
  @Operation(
      summary = "Eliminar dispositivo del conductor",
      description = "Elimina el token FCM asociado al conductor autenticado de forma idempotente.")
  @ApiResponse(responseCode = "204", description = "Dispositivo eliminado")
  public ResponseEntity<Void> remove(@PathVariable String token) {
    commandService.handle(
        new RemoveDriverDeviceCommand(TenantContext.tenantId(), TenantContext.userId(), token));
    return ResponseEntity.noContent().build();
  }
}
