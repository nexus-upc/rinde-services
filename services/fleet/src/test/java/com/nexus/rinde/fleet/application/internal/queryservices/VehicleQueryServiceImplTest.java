package com.nexus.rinde.fleet.application.internal.queryservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nexus.rinde.fleet.application.internal.outboundservices.acl.ExternalIncidentSummary;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.ExternalTripSummary;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.IncidentHistoryClient;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.RemoteLookupResult;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.TripHistoryClient;
import com.nexus.rinde.fleet.domain.model.aggregates.Vehicle;
import com.nexus.rinde.fleet.domain.model.entities.Maintenance;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleHistoryQuery;
import com.nexus.rinde.fleet.domain.model.valueobjects.VehicleHistory;
import com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories.MaintenanceRepository;
import com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories.VehicleRepository;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleQueryServiceImplTest {

  @Mock private VehicleRepository vehicleRepository;
  @Mock private MaintenanceRepository maintenanceRepository;
  @Mock private TripHistoryClient tripHistoryClient;
  @Mock private IncidentHistoryClient incidentHistoryClient;
  @InjectMocks private VehicleQueryServiceImpl service;

  private final UUID tenantId = UUID.randomUUID();
  private final LocalDate today = LocalDate.of(2026, 11, 26);

  private Vehicle registeredVehicle() {
    return new Vehicle(tenantId, "ABC-123", "Volvo", "FH460", 2023, new BigDecimal("18000"));
  }

  @Test
  void combinesMaintenancesTripsAndIncidentsOrderedDescending() {
    Vehicle vehicle = registeredVehicle();
    UUID vehicleId = vehicle.getId();
    when(vehicleRepository.findByIdAndTenantId(vehicleId, tenantId))
        .thenReturn(Optional.of(vehicle));

    Maintenance older =
        new Maintenance(
            tenantId,
            vehicleId,
            "CORRECTIVE",
            LocalDate.of(2026, 9, 10),
            12000,
            new BigDecimal("350"),
            LocalDate.of(2026, 12, 10),
            "Cambio de frenos");
    Maintenance latest =
        new Maintenance(
            tenantId,
            vehicleId,
            "PREVENTIVE",
            LocalDate.of(2026, 11, 9),
            15000,
            new BigDecimal("480"),
            LocalDate.of(2027, 2, 9),
            "Cambio de aceite y filtros");
    when(maintenanceRepository.findByTenantIdAndVehicleId(tenantId, vehicleId))
        .thenReturn(List.of(older, latest));

    UUID tripId = UUID.randomUUID();
    when(tripHistoryClient.findByTenantAndVehicle(tenantId, vehicleId))
        .thenReturn(
            RemoteLookupResult.available(
                List.of(
                    new ExternalTripSummary(
                        tripId,
                        "TRP-000014",
                        "FINISHED",
                        "Lima",
                        "Trujillo",
                        "Materiales de construcción",
                        new BigDecimal("12000"),
                        LocalDate.of(2026, 11, 20),
                        vehicleId,
                        UUID.randomUUID(),
                        Instant.parse("2026-11-20T07:30:00Z"),
                        Instant.parse("2026-11-20T18:00:00Z")))));

    UUID incidentId = UUID.randomUUID();
    when(incidentHistoryClient.findByTenantAndVehicle(tenantId, vehicleId))
        .thenReturn(
            RemoteLookupResult.available(
                List.of(
                    new ExternalIncidentSummary(
                        incidentId,
                        tripId,
                        vehicleId,
                        "BREAKDOWN",
                        "Falla en el freno neumático",
                        "CLOSED",
                        Instant.parse("2026-11-20T13:20:00Z")))));

    VehicleHistory history =
        service.handle(new GetVehicleHistoryQuery(tenantId, vehicleId, null, null), today);

    assertThat(history.vehicleId()).isEqualTo(vehicleId);
    assertThat(history.nextMaintenanceDate()).isEqualTo(LocalDate.of(2027, 2, 9));
    assertThat(history.partial()).isFalse();
    assertThat(history.unavailableSources()).isEmpty();
    assertThat(history.items()).hasSize(4);

    assertThat(history.items().get(0).type()).isEqualTo("INCIDENT");
    assertThat(history.items().get(0).title()).isEqualTo("Incidencia BREAKDOWN");
    assertThat(history.items().get(0).status()).isEqualTo("CLOSED");
    assertThat(history.items().get(0).detail()).isEqualTo("Falla en el freno neumático");

    assertThat(history.items().get(1).type()).isEqualTo("TRIP");
    assertThat(history.items().get(1).title()).isEqualTo("TRP-000014 Lima a Trujillo");
    assertThat(history.items().get(1).status()).isEqualTo("FINISHED");

    assertThat(history.items().get(2).type()).isEqualTo("MAINTENANCE");
    assertThat(history.items().get(2).title()).isEqualTo("Mantenimiento PREVENTIVE");
    assertThat(history.items().get(2).status()).isNull();
  }

  @Test
  void returnsNullNextMaintenanceDateAndFiltersOutEventsOutsideDefault90Days() {
    Vehicle vehicle = registeredVehicle();
    UUID vehicleId = vehicle.getId();
    when(vehicleRepository.findByIdAndTenantId(vehicleId, tenantId))
        .thenReturn(Optional.of(vehicle));
    when(maintenanceRepository.findByTenantIdAndVehicleId(tenantId, vehicleId))
        .thenReturn(List.of());

    UUID recentTripId = UUID.randomUUID();
    when(tripHistoryClient.findByTenantAndVehicle(tenantId, vehicleId))
        .thenReturn(
            RemoteLookupResult.available(
                List.of(
                    new ExternalTripSummary(
                        UUID.randomUUID(),
                        "TRP-000001",
                        "FINISHED",
                        "Lima",
                        "Ica",
                        "Antiguo",
                        new BigDecimal("1000"),
                        today.minusDays(120),
                        vehicleId,
                        UUID.randomUUID(),
                        null,
                        null),
                    new ExternalTripSummary(
                        recentTripId,
                        "TRP-000002",
                        "SCHEDULED",
                        "Lima",
                        "Chimbote",
                        "Reciente",
                        new BigDecimal("2000"),
                        today.minusDays(5),
                        vehicleId,
                        UUID.randomUUID(),
                        null,
                        null))));
    when(incidentHistoryClient.findByTenantAndVehicle(tenantId, vehicleId))
        .thenReturn(RemoteLookupResult.available(List.of()));

    VehicleHistory history =
        service.handle(new GetVehicleHistoryQuery(tenantId, vehicleId, null, null), today);

    assertThat(history.nextMaintenanceDate()).isNull();
    assertThat(history.items())
        .singleElement()
        .satisfies(item -> assertThat(item.id()).isEqualTo(recentTripId));
  }

  @Test
  void marksPartialAndListsUnavailableSourcesWhenTripAndIncidentFail() {
    Vehicle vehicle = registeredVehicle();
    UUID vehicleId = vehicle.getId();
    when(vehicleRepository.findByIdAndTenantId(vehicleId, tenantId))
        .thenReturn(Optional.of(vehicle));
    Maintenance maintenance =
        new Maintenance(
            tenantId,
            vehicleId,
            "PREVENTIVE",
            LocalDate.of(2026, 11, 10),
            20000,
            new BigDecimal("500"),
            LocalDate.of(2027, 2, 10),
            "Revisión general");
    when(maintenanceRepository.findByTenantIdAndVehicleId(tenantId, vehicleId))
        .thenReturn(List.of(maintenance));
    when(tripHistoryClient.findByTenantAndVehicle(tenantId, vehicleId))
        .thenReturn(RemoteLookupResult.unavailable());
    when(incidentHistoryClient.findByTenantAndVehicle(tenantId, vehicleId))
        .thenReturn(RemoteLookupResult.unavailable());

    VehicleHistory history =
        service.handle(
            new GetVehicleHistoryQuery(tenantId, vehicleId, "2026-11-01", "2026-11-26"), today);

    assertThat(history.partial()).isTrue();
    assertThat(history.unavailableSources()).containsExactly("TRIP", "INCIDENT");
    assertThat(history.items()).hasSize(1);
    assertThat(history.items().get(0).type()).isEqualTo("MAINTENANCE");
  }

  @Test
  void rejectsInvalidDatesAndInvertedRangeAndMissingVehicle() {
    UUID vehicleId = UUID.randomUUID();

    assertThatThrownBy(
            () -> service.handle(new GetVehicleHistoryQuery(tenantId, vehicleId, "26/11/2026", null), today))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La fecha de inicio debe tener el formato AAAA-MM-DD.");

    assertThatThrownBy(
            () -> service.handle(new GetVehicleHistoryQuery(tenantId, vehicleId, null, "invalida"), today))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La fecha de fin debe tener el formato AAAA-MM-DD.");

    assertThatThrownBy(
            () ->
                service.handle(
                    new GetVehicleHistoryQuery(tenantId, vehicleId, "2026-11-20", "2026-11-10"),
                    today))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La fecha final no puede ser anterior a la inicial.");

    when(vehicleRepository.findByIdAndTenantId(vehicleId, tenantId)).thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> service.handle(new GetVehicleHistoryQuery(tenantId, vehicleId, null, null), today))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("El vehículo no existe.");
  }
}
