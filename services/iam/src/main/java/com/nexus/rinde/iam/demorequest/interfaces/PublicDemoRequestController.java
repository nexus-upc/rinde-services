package com.nexus.rinde.iam.demorequest.interfaces;

import com.nexus.rinde.iam.demorequest.application.DemoRequestService;
import com.nexus.rinde.iam.demorequest.domain.DemoRequest;
import com.nexus.rinde.iam.demorequest.domain.DemoRequestAlreadyReceivedException;
import com.nexus.rinde.iam.demorequest.interfaces.resources.DemoRequestReceiptResource;
import com.nexus.rinde.iam.demorequest.interfaces.resources.DemoRequestResource;
import com.nexus.rinde.shared.interfaces.rest.ProblemDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Formulario público de la landing para pedir una demostración (US06). No requiere token. */
@RestController
@RequestMapping(
    value = "/api/v1/public/demo-requests",
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Solicitudes de demostración", description = "Formulario público de la landing (US06)")
@SecurityRequirements
public class PublicDemoRequestController {

  private final DemoRequestService service;

  public PublicDemoRequestController(DemoRequestService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Registrar una solicitud de demostración",
      description =
          "Sin autenticación. Si el mismo correo envió una solicitud hace menos de 10 minutos,"
              + " responde 429 y no registra otra.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Solicitud registrada",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = DemoRequestReceiptResource.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Datos inválidos; el cuerpo indica el mensaje de cada campo",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "429",
        description = "El mismo correo envió una solicitud en los últimos 10 minutos",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<DemoRequestReceiptResource> submit(
      @Valid @RequestBody DemoRequestResource resource) {
    DemoRequest request =
        service.submit(
            new DemoRequestService.Submission(
                resource.fullName(),
                resource.companyName(),
                resource.email(),
                resource.phone(),
                resource.fleetSize(),
                resource.message()));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(DemoRequestReceiptResource.from(request));
  }

  @ExceptionHandler(DemoRequestAlreadyReceivedException.class)
  public ResponseEntity<ProblemDetail> alreadyReceived(DemoRequestAlreadyReceivedException ex) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(ProblemDetails.of(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
  }
}
