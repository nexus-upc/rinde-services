package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexus.rinde.notification.application.events.ExpenseRegisteredNotification;
import com.nexus.rinde.notification.application.events.IncidentReportedNotification;
import com.nexus.rinde.notification.infrastructure.messaging.ExpenseRegisteredListener;
import com.nexus.rinde.notification.infrastructure.messaging.IncidentReportedListener;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.CompanyNotificationRepository;
import com.nexus.rinde.support.ContextDoubles;
import com.nexus.rinde.support.TenantAccess;
import com.nexus.rinde.support.TestTokens;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Pasos BDD del centro de notificaciones: avisos de gasto e incidencia, lecturas por usuario y errores. */
public class NotificationCenterSteps {

  private final ScenarioContext context;
  private final ContextDoubles doubles;
  private final TestTokens tokens;
  private final MockMvc mockMvc;
  private final ExpenseRegisteredListener expenseListener;
  private final IncidentReportedListener incidentListener;
  private final CompanyNotificationRepository notifications;
  private final Map<String, ExpenseRegisteredNotification> expenses = new HashMap<>();
  private final Map<String, UUID> trips = new HashMap<>();

  public NotificationCenterSteps(
      ScenarioContext context,
      ContextDoubles doubles,
      TestTokens tokens,
      MockMvc mockMvc,
      ExpenseRegisteredListener expenseListener,
      IncidentReportedListener incidentListener,
      CompanyNotificationRepository notifications) {
    this.context = context;
    this.doubles = doubles;
    this.tokens = tokens;
    this.mockMvc = mockMvc;
    this.expenseListener = expenseListener;
    this.incidentListener = incidentListener;
    this.notifications = notifications;
  }

  @Dado("que el operador {string} inició sesión")
  public void theOperatorSignsIn(String email) {
    context.rememberAccessToken(email, new TenantAccess(doubles, tokens).accessTokenFor(email));
  }

  @Dado("que se registró el gasto {string} por {string} en el viaje {string}")
  public void anExpenseWasRegistered(String name, String amount, String trip) {
    registerExpense(name, amount, trip);
  }

  @Cuando("se registra el gasto {string} por {string} en el viaje {string}")
  public void anExpenseIsRegistered(String name, String amount, String trip) {
    registerExpense(name, amount, trip);
  }

  @Cuando("llega nuevamente el mismo evento del gasto {string}")
  public void theSameExpenseEventArrivesAgain(String name) {
    expenseListener.on(expenses.get(name));
  }

  @Dado("que se reportó la incidencia de tipo {string} en el viaje {string}")
  public void anIncidentWasReported(String incidentType, String trip) {
    incidentListener.on(
        new IncidentReportedNotification(
            UUID.randomUUID(),
            Instant.now(),
            tenantId(),
            UUID.randomUUID(),
            tripId(trip),
            UUID.randomUUID(),
            UUID.randomUUID(),
            incidentType));
  }

  @Cuando("el usuario {string} consulta sus notificaciones")
  public void listsNotifications(String email) throws Exception {
    list(email, false, null);
  }

  @Cuando("el usuario {string} consulta sus notificaciones no leídas")
  public void listsUnreadNotifications(String email) throws Exception {
    list(email, true, null);
  }

  @Cuando("el usuario {string} consulta sus notificaciones de tipo {string}")
  public void listsNotificationsByType(String email, String type) throws Exception {
    list(email, false, type);
  }

  @Cuando("el usuario {string} consulta sus notificaciones no leídas de tipo {string}")
  public void listsUnreadNotificationsByType(String email, String type) throws Exception {
    list(email, true, type);
  }

  @Cuando("el usuario {string} consulta el contador de no leídas")
  public void checksUnreadCount(String email) throws Exception {
    context.record(
        mockMvc.perform(
            get("/api/v1/notifications/unread-count")
                .header("Authorization", "Bearer " + context.accessTokenOf(email))));
  }

  @Cuando("el usuario {string} marca como leída la notificación del gasto {string}")
  public void marksExpenseNoticeAsRead(String email, String name) throws Exception {
    UUID notificationId =
        notifications.findByEventId(expenses.get(name).eventId()).orElseThrow().getId();
    context.record(
        mockMvc.perform(
            patch("/api/v1/notifications/{id}/read", notificationId)
                .header("Authorization", "Bearer " + context.accessTokenOf(email))));
  }

  @Cuando("el usuario {string} marca como leída una notificación inexistente")
  public void marksMissingNoticeAsRead(String email) throws Exception {
    context.record(
        mockMvc.perform(
            patch("/api/v1/notifications/{id}/read", UUID.randomUUID())
                .header("Authorization", "Bearer " + context.accessTokenOf(email))));
  }

  @Cuando("el usuario {string} marca todas las notificaciones como leídas")
  public void marksAllNoticesAsRead(String email) throws Exception {
    context.record(
        mockMvc.perform(
            post("/api/v1/notifications/read-all")
                .header("Authorization", "Bearer " + context.accessTokenOf(email))));
  }

  @Entonces("la lista de notificaciones tiene {int} elementos")
  public void theListHasItems(int expected) throws Exception {
    assertThat(context.body().get("items").size()).isEqualTo(expected);
  }

  @Y("la primera notificación tiene el tipo {string} y el título {string}")
  public void firstNoticeHasTypeAndTitle(String type, String title) throws Exception {
    JsonNode first = context.body().get("items").get(0);
    assertThat(first.get("type").asText()).isEqualTo(type);
    assertThat(first.get("title").asText()).isEqualTo(title);
  }

  @Y("el mensaje de la primera notificación es {string}")
  public void firstNoticeMessageIs(String message) throws Exception {
    assertThat(context.body().get("items").get(0).get("message").asText()).isEqualTo(message);
  }

  @Entonces("el contador de no leídas es {int}")
  public void unreadCountIs(int expected) throws Exception {
    assertThat(context.body().get("count").asLong()).isEqualTo(expected);
  }

  private void registerExpense(String name, String amount, String trip) {
    String[] currencyAndValue = amount.split(" ");
    ExpenseRegisteredNotification event =
        new ExpenseRegisteredNotification(
            UUID.randomUUID(),
            tenantId(),
            UUID.randomUUID(),
            tripId(trip),
            UUID.randomUUID(),
            new BigDecimal(currencyAndValue[1]),
            currencyAndValue[0],
            Instant.now());
    expenses.put(name, event);
    expenseListener.on(event);
  }

  private void list(String email, boolean unreadOnly, String type) throws Exception {
    MockHttpServletRequestBuilder request =
        get("/api/v1/notifications")
            .param("unreadOnly", String.valueOf(unreadOnly))
            .header("Authorization", "Bearer " + context.accessTokenOf(email));
    if (type != null) {
      request.param("type", type);
    }
    context.record(mockMvc.perform(request));
  }

  private UUID tenantId() {
    return UUID.fromString(context.currentTenantId());
  }

  private UUID tripId(String trip) {
    return trips.computeIfAbsent(trip, key -> UUID.randomUUID());
  }
}
