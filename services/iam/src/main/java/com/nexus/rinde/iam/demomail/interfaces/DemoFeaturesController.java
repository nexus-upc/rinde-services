package com.nexus.rinde.iam.demomail.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Expone solo la disponibilidad del modo; nunca destinatarios ni enlaces. */
@RestController
@RequestMapping("/api/v1/demo/features")
@Tag(name = "Demo", description = "Disponibilidad de los modos de demostración")
public class DemoFeaturesController {
  private final boolean enabled;

  /** Con el proveedor azure el buzón no existe, aunque la propiedad del buzón siga activa. */
  public DemoFeaturesController(
      @Value("${rinde.demo.mailbox.enabled:false}") boolean enabled,
      @Value("${rinde.mail.provider:demo}") String mailProvider) {
    this.enabled = enabled && "demo".equals(mailProvider == null ? "demo" : mailProvider.trim());
  }

  @Operation(summary = "Consultar si el buzón de demostración está habilitado")
  @GetMapping
  public ResponseEntity<Features> features() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Features(enabled));
  }

  public record Features(boolean mailboxEnabled) {}
}
