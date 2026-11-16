package com.nexus.rinde.iam.demorequest.interfaces;

import com.nexus.rinde.iam.demorequest.application.DemoRequestService;
import com.nexus.rinde.iam.demorequest.domain.DemoRequest;
import com.nexus.rinde.iam.demorequest.interfaces.resources.DemoRequestSummaryResource;
import com.nexus.rinde.shared.interfaces.rest.PageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Solicitudes recibidas desde la landing, solo para el dueño de Rinde (US06). */
@RestController
@Validated
@RequestMapping("/api/v1/platform/demo-requests")
@PreAuthorize("hasAuthority('ROLE_PLATFORM_OWNER')")
@Tag(name = "Solicitudes de demostración", description = "Consulta del dueño de Rinde (US06)")
public class PlatformDemoRequestController {

  private final DemoRequestService service;

  public PlatformDemoRequestController(DemoRequestService service) {
    this.service = service;
  }

  @Operation(
      summary = "Listar las solicitudes de demostración",
      description = "Orden descendente por fecha de recepción, con paginación.")
  @GetMapping
  public ResponseEntity<PageResource<DemoRequestSummaryResource>> list(
      @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    PageResource<DemoRequest> result = service.list(page, size);
    PageResource<DemoRequestSummaryResource> body =
        new PageResource<>(
            result.items().stream().map(DemoRequestSummaryResource::from).toList(),
            result.page(),
            result.size(),
            result.total());
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
  }
}
