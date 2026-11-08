package com.nexus.rinde.iam.infrastructure.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Prueba el contrato HTTP propio, el aislamiento de empresa y la indisponibilidad de IAM. */
class HttpIamContextFacadeTest {
  private final RestClient.Builder builder = RestClient.builder().baseUrl("http://iam.test");
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
  private final HttpIamContextFacade facade = new HttpIamContextFacade(builder.build());
  private final UUID tenantId = UUID.randomUUID();

  @AfterEach
  void verifyRequests() { server.verify(); }

  @Test
  void readsTheStatusFromTheSeparateService() {
    server.expect(requestTo("http://iam.test/api/internal/v1/tenants/" + tenantId + "/status"))
        .andRespond(withSuccess("{\"tenantId\":\"" + tenantId + "\",\"status\":\"RESTRICTED\"}", MediaType.APPLICATION_JSON));
    assertThat(facade.findTenantStatus(tenantId)).contains("RESTRICTED");
  }

  @Test
  void treatsAMissingTenantAsAbsent() {
    server.expect(requestTo("http://iam.test/api/internal/v1/tenants/" + tenantId + "/status"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));
    assertThat(facade.findTenantStatus(tenantId)).isEmpty();
  }

  @Test
  void doesNotAcceptAStatusOfAnotherTenant() {
    server.expect(requestTo("http://iam.test/api/internal/v1/tenants/" + tenantId + "/status"))
        .andRespond(withSuccess("{\"tenantId\":\"" + UUID.randomUUID() + "\",\"status\":\"ACTIVE\"}",
            MediaType.APPLICATION_JSON));
    assertThatThrownBy(() -> facade.findTenantStatus(tenantId)).isInstanceOf(IamUnavailableException.class);
  }

  @Test
  void doesNotFallBackToAStaleTokenWhenIamIsDown() {
    server.expect(requestTo("http://iam.test/api/internal/v1/tenants/" + tenantId + "/status"))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    assertThatThrownBy(() -> facade.findTenantStatus(tenantId)).isInstanceOf(IamUnavailableException.class);
  }

  @Test
  void availabilityFailureDuringTokenReadingReturns503() throws Exception {
    var response = new MockHttpServletResponse();
    new IamAvailabilityFilter(new ObjectMapper()).doFilter(new MockHttpServletRequest(), response,
        (request, result) -> { throw new IamUnavailableException(); });
    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(response.getContentAsString()).contains("503", "identidad");
  }
}
