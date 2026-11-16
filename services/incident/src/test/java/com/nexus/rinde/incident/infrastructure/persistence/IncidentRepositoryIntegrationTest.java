package com.nexus.rinde.incident.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.commands.ReportIncidentCommand;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentStatus;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentType;
import com.nexus.rinde.incident.domain.model.valueobjects.TripSnapshot;
import com.nexus.rinde.incident.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.nexus.rinde.incident.infrastructure.persistence.jpa.repositories.IncidentSpecifications;
import com.nexus.rinde.support.AbstractIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Persistencia del agregado Incident: aislamiento por empresa, filtros y resolución. */
class IncidentRepositoryIntegrationTest extends AbstractIntegrationTest {

  @Autowired private IncidentRepository incidents;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID otherTenantId = UUID.randomUUID();

  @Test
  @DisplayName("Guarda la incidencia y la recupera solo dentro de su empresa")
  void persistsIncidentAndIsolatesTenants() {
    Incident saved = incidents.saveAndFlush(report(tenantId, UUID.randomUUID(), UUID.randomUUID(), null));

    assertThat(incidents.findByIdAndTenantId(saved.getId(), tenantId)).isPresent();
    assertThat(incidents.findByIdAndTenantId(saved.getId(), otherTenantId)).isEmpty();
  }

  @Test
  @DisplayName("Filtra por estado, viaje y vehículo, siempre dentro de la empresa")
  void filtersByStatusTripAndVehicle() {
    UUID tripA = UUID.randomUUID();
    UUID vehicleA = UUID.randomUUID();
    Incident open = incidents.saveAndFlush(report(tenantId, tripA, vehicleA, null));
    Incident attended = incidents.saveAndFlush(report(tenantId, UUID.randomUUID(), UUID.randomUUID(), null));
    attended.startAttention();
    incidents.saveAndFlush(attended);
    incidents.saveAndFlush(report(otherTenantId, tripA, vehicleA, null));

    List<Incident> byStatus =
        incidents.findAll(IncidentSpecifications.matching(tenantId, IncidentStatus.OPEN, null, null));
    List<Incident> byTrip =
        incidents.findAll(IncidentSpecifications.matching(tenantId, null, tripA, null));
    List<Incident> byVehicle =
        incidents.findAll(IncidentSpecifications.matching(tenantId, null, null, vehicleA));

    assertThat(byStatus).extracting(Incident::getId).doesNotContain(attended.getId()).contains(open.getId());
    assertThat(byTrip).extracting(Incident::getId).containsExactly(open.getId());
    assertThat(byVehicle).extracting(Incident::getId).containsExactly(open.getId());
  }

  @Test
  @DisplayName("La clave de idempotencia se busca por empresa")
  void findsIdempotencyKeyOnlyInItsTenant() {
    incidents.saveAndFlush(report(tenantId, UUID.randomUUID(), UUID.randomUUID(), "KEY-INC-01"));

    Optional<Incident> sameTenant = incidents.findByTenantIdAndIdempotencyKey(tenantId, "KEY-INC-01");
    Optional<Incident> otherTenant =
        incidents.findByTenantIdAndIdempotencyKey(otherTenantId, "KEY-INC-01");

    assertThat(sameTenant).isPresent();
    assertThat(otherTenant).isEmpty();
  }

  @Test
  @DisplayName("Guarda la resolución al cerrar y excluye las cerradas de las abiertas")
  void persistsResolutionAndExcludesClosedFromOpen() {
    UUID operator = UUID.randomUUID();
    Incident closed = incidents.saveAndFlush(report(tenantId, UUID.randomUUID(), UUID.randomUUID(), null));
    closed.close(operator, "Vehículo revisado en taller", Instant.parse("2026-11-09T12:00:00Z"));
    incidents.saveAndFlush(closed);
    Incident open = incidents.saveAndFlush(report(tenantId, UUID.randomUUID(), UUID.randomUUID(), null));

    Incident reloaded = incidents.findByIdAndTenantId(closed.getId(), tenantId).orElseThrow();

    assertThat(reloaded.getStatus()).isEqualTo(IncidentStatus.CLOSED);
    assertThat(reloaded.getResolution().getAction()).isEqualTo("Vehículo revisado en taller");
    assertThat(reloaded.getResolution().getClosedBy()).isEqualTo(operator);
    assertThat(incidents.findByTenantIdAndStatusNotOrderByReportedAtDesc(tenantId, IncidentStatus.CLOSED))
        .extracting(Incident::getId)
        .containsExactly(open.getId());
  }

  private Incident report(UUID tenant, UUID tripId, UUID vehicleId, String key) {
    UUID driverId = UUID.randomUUID();
    TripSnapshot trip = new TripSnapshot(tripId, "IN_ROUTE", vehicleId, driverId);
    ReportIncidentCommand command =
        new ReportIncidentCommand(
            tenant, tripId, UUID.randomUUID(), IncidentType.DELAY, "Retraso por lluvia", null, key);
    return Incident.report(trip, driverId, command, Instant.now());
  }
}
