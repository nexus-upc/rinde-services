package com.nexus.rinde.notification.interfaces.rest;

import com.nexus.rinde.notification.domain.model.commands.MarkAllNotificationsReadCommand;
import com.nexus.rinde.notification.domain.model.commands.MarkNotificationReadCommand;
import com.nexus.rinde.notification.domain.model.queries.GetUnreadNotificationCountQuery;
import com.nexus.rinde.notification.domain.model.queries.ListCompanyNotificationsQuery;
import com.nexus.rinde.notification.domain.model.valueobjects.NotificationPage;
import com.nexus.rinde.notification.domain.services.CompanyNotificationCommandService;
import com.nexus.rinde.notification.domain.services.CompanyNotificationQueryService;
import com.nexus.rinde.notification.interfaces.rest.resources.NotificationPageResource;
import com.nexus.rinde.notification.interfaces.rest.resources.NotificationResource;
import com.nexus.rinde.notification.interfaces.rest.resources.UnreadNotificationCountResource;
import com.nexus.rinde.shared.infrastructure.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Centro de notificaciones operativas para administradores y operadores. */
@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
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
public class NotificationsController {

  private final CompanyNotificationQueryService queryService;
  private final CompanyNotificationCommandService commandService;

  public NotificationsController(
      CompanyNotificationQueryService queryService,
      CompanyNotificationCommandService commandService) {
    this.queryService = queryService;
    this.commandService = commandService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Listar notificaciones de la empresa",
      description = "Devuelve notificaciones paginadas con estado de lectura para el usuario.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Listado paginado de notificaciones"),
    @ApiResponse(
        responseCode = "400",
        description = "Parámetros de paginación inválidos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<NotificationPageResource> list(
      @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    NotificationPage result =
        queryService.handle(
            new ListCompanyNotificationsQuery(
                TenantContext.tenantId(), TenantContext.userId(), unreadOnly, page, size));
    List<NotificationResource> items =
        result.items().stream()
            .map(
                item ->
                    new NotificationResource(
                        item.id(),
                        item.type(),
                        item.title(),
                        item.message(),
                        item.link(),
                        item.createdAt(),
                        item.readAt()))
            .toList();
    return ResponseEntity.ok(
        new NotificationPageResource(
            items, result.page(), result.size(), result.totalItems(), result.totalPages()));
  }

  @GetMapping("/unread-count")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Contar notificaciones no leídas",
      description = "Devuelve el total de notificaciones de la empresa sin leer por el usuario.")
  @ApiResponse(responseCode = "200", description = "Conteo de no leídas")
  public ResponseEntity<UnreadNotificationCountResource> unreadCount() {
    long count =
        queryService.handle(
            new GetUnreadNotificationCountQuery(TenantContext.tenantId(), TenantContext.userId()));
    return ResponseEntity.ok(new UnreadNotificationCountResource(count));
  }

  @PatchMapping("/{id}/read")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Marcar una notificación como leída",
      description = "Registra la lectura individual de la notificación para el usuario actual.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Notificación marcada como leída"),
    @ApiResponse(
        responseCode = "404",
        description = "La notificación no existe",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
    commandService.handle(
        new MarkNotificationReadCommand(TenantContext.tenantId(), TenantContext.userId(), id));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/read-all")
  @PreAuthorize("hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')")
  @Operation(
      summary = "Marcar todas las notificaciones como leídas",
      description = "Marca como leídas todas las notificaciones pendientes para el usuario actual.")
  @ApiResponse(responseCode = "204", description = "Notificaciones marcadas como leídas")
  public ResponseEntity<Void> markAllAsRead() {
    commandService.handle(
        new MarkAllNotificationsReadCommand(TenantContext.tenantId(), TenantContext.userId()));
    return ResponseEntity.noContent().build();
  }
}
