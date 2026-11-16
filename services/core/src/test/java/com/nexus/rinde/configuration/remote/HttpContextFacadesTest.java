package com.nexus.rinde.configuration.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nexus.rinde.expense.infrastructure.remote.HttpExpenseContextFacade;
import com.nexus.rinde.fleet.infrastructure.remote.HttpFleetContextFacade;
import com.nexus.rinde.incident.infrastructure.remote.HttpIncidentContextFacade;
import com.nexus.rinde.trip.infrastructure.remote.HttpTripContextFacade;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Prueba los contratos internos que core consume de Trip, Fleet, Expense e Incident separados. */
class HttpContextFacadesTest {

  private final UUID tenantId = UUID.randomUUID();

  @Test
  void readsTheOpenIncidentCountAndReportsAnUnavailableService() {
    var builder = RestClient.builder().baseUrl("http://incident.test");
    var server = MockRestServiceServer.bindTo(builder).build();
    var facade = new HttpIncidentContextFacade(builder.build());
    String path = "http://incident.test/api/internal/v1/incidents/tenants/" + tenantId + "/open-count";
    server
        .expect(requestTo(path))
        .andRespond(
            withSuccess(
                "{\"count\":4}", MediaType.APPLICATION_JSON));
    server.expect(requestTo(path)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThat(facade.countOpenByTenantId(tenantId)).isEqualTo(4L);
    assertThatThrownBy(() -> facade.countOpenByTenantId(tenantId))
        .isInstanceOf(RemoteServiceUnavailableException.class);
    server.verify();
  }

  @Test
  void readsATripAndTreatsAMissingOneAsAbsent() {
    var builder = RestClient.builder().baseUrl("http://trip.test");
    var server = MockRestServiceServer.bindTo(builder).build();
    var facade = new HttpTripContextFacade(builder.build());
    UUID tripId = UUID.randomUUID();
    String base = "http://trip.test/api/internal/v1/trips/tenants/" + tenantId;
    server
        .expect(requestTo(base + "/" + tripId))
        .andRespond(
            withSuccess(
                "{\"tripId\":\"" + tripId + "\",\"code\":\"TRP-000001\",\"status\":\"FINISHED\"}",
                MediaType.APPLICATION_JSON));
    server.expect(requestTo(base + "/" + tenantId)).andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThat(facade.findByIdAndTenantId(tripId, tenantId))
        .hasValueSatisfying(trip -> assertThat(trip.status()).isEqualTo("FINISHED"));
    assertThat(facade.findByIdAndTenantId(tenantId, tenantId)).isEmpty();
    server.verify();
  }

  @Test
  void countsAndListsFleetUnitsOfTheTenant() {
    var builder = RestClient.builder().baseUrl("http://fleet.test");
    var server = MockRestServiceServer.bindTo(builder).build();
    var facade = new HttpFleetContextFacade(builder.build());
    String base = "http://fleet.test/api/internal/v1/fleet/tenants/" + tenantId;
    server
        .expect(requestTo(base + "/vehicles/count"))
        .andRespond(withSuccess("{\"count\":3}", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(base + "/vehicles"))
        .andRespond(
            withSuccess(
                "[{\"id\":\"" + UUID.randomUUID() + "\",\"plateNumber\":\"ABC-123\"}]",
                MediaType.APPLICATION_JSON));

    assertThat(facade.countVehiclesByTenantId(tenantId)).isEqualTo(3);
    assertThat(facade.findVehiclesByTenantId(tenantId))
        .singleElement()
        .satisfies(vehicle -> assertThat(vehicle.plateNumber()).isEqualTo("ABC-123"));
    server.verify();
  }

  @Test
  void readsApprovedExpensesAndReportsAnUnavailableService() {
    var builder = RestClient.builder().baseUrl("http://expense.test");
    var server = MockRestServiceServer.bindTo(builder).build();
    var facade = new HttpExpenseContextFacade(builder.build());
    UUID tripId = UUID.randomUUID();
    String base = "http://expense.test/api/internal/v1/expenses/tenants/" + tenantId;
    server
        .expect(requestTo(base + "/trips/" + tripId + "/approved"))
        .andRespond(
            withSuccess(
                "[{\"id\":\"" + UUID.randomUUID() + "\",\"amount\":35.50,\"status\":\"APPROVED\"}]",
                MediaType.APPLICATION_JSON));
    server.expect(requestTo(base)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThat(facade.findApprovedExpensesByTripId(tenantId, tripId))
        .singleElement()
        .satisfies(expense -> assertThat(expense.amount()).isEqualByComparingTo(new BigDecimal("35.50")));
    assertThatThrownBy(() -> facade.findByTenantId(tenantId))
        .isInstanceOf(RemoteServiceUnavailableException.class);
    server.verify();
  }
}
