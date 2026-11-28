package com.nexus.rinde.trip.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.trip.domain.model.events.TripAssigned;
import com.nexus.rinde.trip.domain.model.events.TripFinished;
import com.nexus.rinde.trip.domain.model.events.TripStarted;
import com.nexus.rinde.trip.domain.model.valueobjects.Availability;
import com.nexus.rinde.trip.domain.model.valueobjects.Cargo;
import com.nexus.rinde.trip.domain.model.valueobjects.MaintenanceState;
import com.nexus.rinde.trip.domain.model.valueobjects.Route;
import com.nexus.rinde.trip.domain.model.valueobjects.TripStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripTest {

  private static final UUID TENANT = UUID.randomUUID();
  private static final UUID USER = UUID.randomUUID();
  private static final UUID VEHICLE = UUID.randomUUID();
  private static final UUID DRIVER = UUID.randomUUID();
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);
  private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");

  @Test
  void schedulesTheTripInScheduledStatusWithItsFirstChange() {
    Trip trip = scheduled(TODAY.plusDays(5));

    assertThat(trip.getStatus()).isEqualTo(TripStatus.SCHEDULED);
    assertThat(trip.getStatusChanges()).hasSize(1);
  }

  @Test
  void rejectsAPastDepartureUnlessItIsConfirmed() {
    assertThatThrownBy(() -> scheduled(TODAY.minusDays(10)))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("anterior a hoy");
  }

  @Test
  void rejectsACodeOutsideTheTripFormat() {
    assertThatThrownBy(
            () ->
                Trip.schedule(
                    TENANT,
                    "VIAJE-1",
                    route(),
                    cargo(),
                    TODAY.plusDays(5),
                    false,
                    TODAY,
                    NOW,
                    USER))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void assignsAvailableResourcesAndRegistersTripAssigned() {
    Trip trip = scheduled(TODAY.plusDays(5));

    trip.assign(VEHICLE, DRIVER, available(MaintenanceState.UP_TO_DATE), false, USER, NOW);

    assertThat(trip.getStatus()).isEqualTo(TripStatus.ASSIGNED);
    assertThat(trip.pendingDomainEvents()).hasSize(1).first().isInstanceOf(TripAssigned.class);
    assertThat(trip.getAssignment().overdueMaintenanceConfirmedBy()).isNull();
  }

  @Test
  void rejectsADriverThatFleetDoesNotEnable() {
    Trip trip = scheduled(TODAY.plusDays(5));

    assertThatThrownBy(
            () ->
                trip.assign(
                    VEHICLE,
                    DRIVER,
                    new Availability(MaintenanceState.UP_TO_DATE, false),
                    false,
                    USER,
                    NOW))
        .isInstanceOf(ConflictException.class)
        .hasMessage("El conductor no está habilitado para ser asignado.");
  }

  @Test
  void overdueMaintenanceNeedsConfirmationAndKeepsWhoConfirmed() {
    Trip trip = scheduled(TODAY.plusDays(5));

    assertThatThrownBy(
            () -> trip.assign(VEHICLE, DRIVER, available(MaintenanceState.OVERDUE), false, USER, NOW))
        .isInstanceOf(ConflictException.class);

    trip.assign(VEHICLE, DRIVER, available(MaintenanceState.OVERDUE), true, USER, NOW);

    assertThat(trip.getAssignment().overdueMaintenanceConfirmedBy()).isEqualTo(USER);
  }

  @Test
  void finishRequiresATripInRoute() {
    Trip trip = scheduled(TODAY.plusDays(5));
    trip.assign(VEHICLE, DRIVER, available(MaintenanceState.UP_TO_DATE), false, USER, NOW);

    assertThatThrownBy(() -> trip.finish(USER, NOW))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("ASSIGNED");
  }

  @Test
  void startAndFinishRegisterTheirEventsInOrder() {
    Trip trip = scheduled(TODAY.plusDays(5));
    trip.assign(VEHICLE, DRIVER, available(MaintenanceState.UP_TO_DATE), false, USER, NOW);

    trip.start(USER, NOW);
    trip.finish(USER, NOW);

    assertThat(trip.getStatus()).isEqualTo(TripStatus.FINISHED);
    assertThat(trip.getFinishedAt()).isEqualTo(NOW);
    assertThat(trip.pendingDomainEvents())
        .hasExactlyElementsOfTypes(TripAssigned.class, TripStarted.class, TripFinished.class);
  }

  @Test
  void locationRecordingRequiresInRouteStatus() {
    Trip trip = scheduled(TODAY.plusDays(5));
    trip.assign(VEHICLE, DRIVER, available(MaintenanceState.UP_TO_DATE), false, USER, NOW);

    assertThatThrownBy(trip::ensureInRouteForLocation)
        .isInstanceOf(ConflictException.class)
        .hasMessage("Solo se registra la ubicación de un viaje en ruta.");

    trip.start(USER, NOW);
    trip.ensureInRouteForLocation();

    trip.finish(USER, NOW);
    assertThatThrownBy(trip::ensureInRouteForLocation)
        .isInstanceOf(ConflictException.class)
        .hasMessage("Solo se registra la ubicación de un viaje en ruta.");
  }

  private static Trip scheduled(LocalDate departureDate) {
    return Trip.schedule(
        TENANT, "TRP-000001", route(), cargo(), departureDate, false, TODAY, NOW, USER);
  }

  private static Availability available(MaintenanceState maintenance) {
    return new Availability(maintenance, true);
  }

  private static Route route() {
    return new Route("Lima", "Arequipa");
  }

  private static Cargo cargo() {
    return new Cargo("Repuestos", new BigDecimal("850.25"));
  }
}
