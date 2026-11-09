package com.nexus.rinde.support;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Doble de IAM respaldado por WireMock para simular la consulta de estado de la empresa.
 */
@Component
public class IamStub {

  private static final String TENANT_STATUS_PATH = "/api/internal/v1/tenants/[^/]+/status";
  private final WireMockServer server = WireMockServices.server();

  public IamStub() {
    apply();
  }

  public synchronized void reset() {
    server.resetMappings();
    apply();
  }

  /** Desde ahora IAM informa la cuenta del usuario en la empresa, activa y con el rol indicado. */
  public synchronized void registerAccount(UUID tenantId, UUID userId, String role) {
    registerAccount(tenantId, userId, role, "ACTIVE");
  }

  /** Desde ahora IAM informa la cuenta del usuario en la empresa, con rol y estado indicados. */
  public synchronized void registerAccount(
      UUID tenantId, UUID userId, String role, String status) {
    server.stubFor(
        get(urlPathEqualTo("/api/internal/v1/tenants/" + tenantId + "/users/" + userId))
            .willReturn(
                okJson(
                    """
                    {"userId":"%s","tenantId":"%s","fullName":"Cuenta de prueba",\
                    "email":"cuenta@andes.pe","role":"%s","status":"%s"}"""
                        .formatted(userId, tenantId, role, status))));
  }

  /** Desde ahora IAM informa a la empresa como restringida por su suscripción vencida. */
  public synchronized void restrictTenant(UUID tenantId) {
    server.stubFor(
        get(urlPathMatching("/api/internal/v1/tenants/" + tenantId + "/status"))
            .atPriority(1)
            .willReturn(okJson("{\"status\":\"RESTRICTED\"}")));
  }

  private void apply() {
    server.stubFor(
        get(urlPathMatching(TENANT_STATUS_PATH)).willReturn(okJson("{\"status\":\"ACTIVE\"}")));
  }
}
