package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.notification.application.events.TripAssignedNotification;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.RetryStoreEntry;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.RetryStoreRepository;
import com.nexus.rinde.support.ContextDoubles;
import com.nexus.rinde.support.RelativeDates;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;

/** Pasos BDD que verifican la asignación y la preparación del aviso pendiente. */
public class NotificationSteps {

  private final ScenarioContext context;
  private final ContextDoubles doubles;
  private final RetryStoreRepository retryStoreRepository;

  public NotificationSteps(
      ScenarioContext context, ContextDoubles doubles, RetryStoreRepository retryStoreRepository) {
    this.context = context;
    this.doubles = doubles;
    this.retryStoreRepository = retryStoreRepository;
  }

  @Entonces("el viaje queda asignado al conductor {string}")
  public void theTripIsAssignedToTheDriver(String driverEmail) {
    TripAssignedNotification assignment = context.lastTripAssignment();
    assertThat(assignment.driverId().toString()).isEqualTo(context.userIdOf(driverEmail));
    assertThat(doubles.trip(assignment.tripId()).orElseThrow().status()).isEqualTo("ASSIGNED");
  }

  @Y(
      "Notifications prepara un aviso pendiente para el conductor {string} con el código del viaje,"
          + " el destino {string} y la fecha de salida {string}")
  public void notificationsPreparesPendingNotice(
      String driverEmail, String destination, String departureDate) {
    TripAssignedNotification event = context.lastTripAssignment();
    RetryStoreEntry entry = retryStoreRepository.findById(event.eventId()).orElseThrow();

    assertThat(context.emailOfUserId(event.driverId().toString())).isEqualTo(driverEmail);
    assertThat(entry.getTenantId()).isEqualTo(event.tenantId());
    assertThat(entry.getRecipientDriverId()).isEqualTo(event.driverId());
    assertThat(entry.getTripCode()).isEqualTo(event.code());
    assertThat(entry.getDestination()).isEqualTo(destination);
    String departure = RelativeDates.resolve(departureDate);
    assertThat(entry.getDepartureDate()).isEqualTo(RelativeDates.date(departureDate));
    assertThat(entry.getMessage())
        .contains(event.code(), destination, departure);
    assertThat(entry.getDeliveryStatus()).isEqualTo("PENDING");
    assertThat(entry.getRetryCount()).isZero();
    assertThat(entry.getLastError()).containsIgnoringCase("no hay token de dispositivo");
  }
}
