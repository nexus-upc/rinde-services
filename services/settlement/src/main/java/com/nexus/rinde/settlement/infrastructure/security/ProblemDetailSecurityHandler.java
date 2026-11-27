package com.nexus.rinde.settlement.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.shared.interfaces.rest.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Responde 401 y 403 de la cadena de seguridad con el mismo formato RFC 7807 del resto de la API.
 */
@Component
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public ProblemDetailSecurityHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    write(response, HttpStatus.UNAUTHORIZED, "Se requiere un token de acceso válido.");
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    write(response, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación.");
  }

  private void write(HttpServletResponse response, HttpStatus status, String detail)
      throws IOException {
    ProblemDetail problem = ProblemDetails.of(status, detail);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), problem);
  }
}
