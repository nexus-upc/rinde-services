package com.nexus.rinde.iam.demomail.interfaces;

import com.nexus.rinde.iam.demomail.application.DemoMailboxService;
import com.nexus.rinde.iam.domain.model.valueobjects.TokenPurpose;
import com.nexus.rinde.shared.interfaces.rest.PageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/platform/demo-mailbox/messages")
@PreAuthorize("hasAuthority('ROLE_PLATFORM_OWNER')")
@ConditionalOnProperty(name = "rinde.demo.mailbox.enabled", havingValue = "true")
@ConditionalOnProperty(name = "rinde.mail.provider", havingValue = "demo", matchIfMissing = true)
@Tag(name = "Platform mailbox", description = "Buzón de demostración del dueño de Rinde: verificación, invitación y recuperación")
public class DemoMailboxController {
  private final DemoMailboxService mailbox;

  public DemoMailboxController(DemoMailboxService mailbox) {
    this.mailbox = mailbox;
  }

  @Operation(summary = "Listar los mensajes de acceso de demostración")
  @GetMapping
  public ResponseEntity<PageResource<DemoMailboxService.Summary>> list(
      @RequestParam(defaultValue = "") @Size(max = 150) String search,
      @RequestParam(required = false) TokenPurpose purpose,
      @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return privateResponse(mailbox.list(search, purpose, page, size));
  }

  @Operation(summary = "Abrir un mensaje y su enlace vigente")
  @GetMapping("/{id}")
  public ResponseEntity<DemoMailboxService.Detail> open(@PathVariable UUID id) {
    return privateResponse(mailbox.open(id));
  }

  private <T> ResponseEntity<T> privateResponse(T value) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("Referrer-Policy", "no-referrer")
        .body(value);
  }
}
