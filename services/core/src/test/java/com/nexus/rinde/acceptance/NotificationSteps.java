package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.notification.application.events.TripAssignedNotification;
import com.nexus.rinde.notification.domain.model.valueobjects.TripAssignmentNotice;
import com.nexus.rinde.notification.domain.services.PushAdapter;
import com.nexus.rinde.notification.infrastructure.messaging.TripAssignedListener;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.RetryStoreEntry;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.RetryStoreRepository;
import com.nexus.rinde.notification.infrastructure.services.PushAdapterUnavailableException;
import com.nexus.rinde.support.ContextDoubles;
import com.nexus.rinde.support.RelativeDates;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Pasos BDD que verifican el registro de dispositivos y el envío de avisos al conductor. */
public class NotificationSteps {

  private final ScenarioContext context;
  private final ContextDoubles doubles;
  private final RetryStoreRepository retryStoreRepository;
  private final PushAdapter pushAdapter;
  private final TripAssignedListener tripAssignedListener;
  private final MockMvc mockMvc;

  public NotificationSteps(
      ScenarioContext context,
      ContextDoubles doubles,
      RetryStoreRepository retryStoreRepository,
      PushAdapter pushAdapter,
      TripAssignedListener tripAssignedListener,
      MockMvc mockMvc) {
    this.context = context;
    this.doubles = doubles;
    this.retryStoreRepository = retryStoreRepository;
    this.pushAdapter = pushAdapter;
    this.tripAssignedListener = tripAssignedListener;
    this.mockMvc = mockMvc;
  }

  @Before
  public void resetPushAdapter() {
    reset(pushAdapter);
  }

  @Dado("que el conductor {string} registró su dispositivo {string} en la plataforma {string}")
  public void driverRegistersDevice(String driverEmail, String token, String platform)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .header("Authorization", "Bearer " + context.accessTokenOf(driverEmail))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"platform\":\"" + platform + "\"}"))
        .andExpect(status().isNoContent());
  }

  @Cuando("el conductor {string} elimina su dispositivo {string}")
  public void driverRemovesDevice(String driverEmail, String token) throws Exception {
    context.record(
        mockMvc.perform(
            delete("/api/v1/notifications/devices/{token}", token)
                .header("Authorization", "Bearer " + context.accessTokenOf(driverEmail))));
  }

  @Dado("que el proveedor push falla temporalmente")
  public void pushProviderFailsTemporarily() {
    doThrow(new PushAdapterUnavailableException("Proveedor push temporalmente no disponible."))
        .when(pushAdapter)
        .send(any());
  }

  @Cuando("llega nuevamente el mismo evento de asignación del viaje")
  public void sameAssignmentEventArrivesAgain() {
    tripAssignedListener.on(context.lastTripAssignment());
  }

  @Entonces("el viaje queda asignado al conductor {string}")
  public void theTripIsAssignedToTheDriver(String driverEmail) {
    TripAssignedNotification assignment = context.lastTripAssignment();
    assertThat(assignment.driverId().toString()).isEqualTo(context.userIdOf(driverEmail));
    assertThat(doubles.trip(assignment.tripId()).orElseThrow().status()).isEqualTo("ASSIGNED");
  }

  @Y(
      "Notifications entrega el aviso push al token {string} del conductor {string} con el código"
          + " del viaje, el destino {string} y la fecha de salida {string}")
  public void notificationsDeliversPushToToken(
      String expectedToken, String driverEmail, String destination, String departureDate) {
    TripAssignedNotification event = context.lastTripAssignment();
    RetryStoreEntry entry = retryStoreRepository.findById(event.eventId()).orElseThrow();

    assertNoticeMatchesEvent(entry, event, driverEmail, destination, departureDate);
    assertThat(entry.getDeliveryStatus()).isEqualTo(RetryStoreEntry.DELIVERED);
    assertThat(entry.getNextAttemptAt()).isNull();

    ArgumentCaptor<TripAssignmentNotice> captor =
        ArgumentCaptor.forClass(TripAssignmentNotice.class);
    verify(pushAdapter).send(captor.capture());
    assertThat(captor.getValue().deviceTokens()).containsExactly(expectedToken);
  }

  @Y(
      "Notifications prepara un aviso pendiente para el conductor {string} con el código del viaje,"
          + " el destino {string} y la fecha de salida {string}")
  public void notificationsPreparesPendingNotice(
      String driverEmail, String destination, String departureDate) {
    TripAssignedNotification event = context.lastTripAssignment();
    RetryStoreEntry entry = retryStoreRepository.findById(event.eventId()).orElseThrow();

    assertNoticeMatchesEvent(entry, event, driverEmail, destination, departureDate);
    assertThat(entry.getDeliveryStatus()).isEqualTo(RetryStoreEntry.PENDING);
    assertThat(entry.getRetryCount()).isZero();
    assertThat(entry.getNextAttemptAt()).isNotNull();
    assertThat(entry.getLastError()).contains("temporalmente no disponible");
  }

  @Y("Notifications registra un único envío push para el evento")
  public void notificationsRecordsSinglePushDelivery() {
    verify(pushAdapter, times(1)).send(any());
    assertThat(retryStoreRepository.count()).isEqualTo(1);
  }

  @Y("Notifications omite el aviso sin programar reintentos")
  public void notificationsSkipsNoticeWithoutRetries() {
    TripAssignedNotification event = context.lastTripAssignment();
    RetryStoreEntry entry = retryStoreRepository.findById(event.eventId()).orElseThrow();

    assertThat(entry.getDeliveryStatus()).isEqualTo(RetryStoreEntry.SKIPPED);
    assertThat(entry.getNextAttemptAt()).isNull();
    verify(pushAdapter, never()).send(any());
  }

  private void assertNoticeMatchesEvent(
      RetryStoreEntry entry,
      TripAssignedNotification event,
      String driverEmail,
      String destination,
      String departureDate) {
    assertThat(context.emailOfUserId(event.driverId().toString())).isEqualTo(driverEmail);
    assertThat(entry.getTenantId()).isEqualTo(event.tenantId());
    assertThat(entry.getRecipientDriverId()).isEqualTo(event.driverId());
    assertThat(entry.getTripCode()).isEqualTo(event.code());
    assertThat(entry.getDestination()).isEqualTo(destination);
    String departure = RelativeDates.resolve(departureDate);
    assertThat(entry.getDepartureDate()).isEqualTo(RelativeDates.date(departureDate));
    assertThat(entry.getMessage()).contains(event.code(), destination, departure);
  }
}
