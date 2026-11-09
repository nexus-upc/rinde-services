package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.iam.domain.model.events.UserInvited;
import com.nexus.rinde.support.CapturedEvents;
import com.nexus.rinde.support.IamApi;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.springframework.test.web.servlet.MockMvc;

/** Pasos de la gestión de usuarios de la empresa (US11). */
public class UserSteps {

  private final ScenarioContext context;
  private final CapturedEvents capturedEvents;
  private final IamApi api;

  public UserSteps(
      ScenarioContext context,
      CapturedEvents capturedEvents,
      MockMvc mockMvc,
      ObjectMapper objectMapper) {
    this.context = context;
    this.capturedEvents = capturedEvents;
    this.api = new IamApi(mockMvc, objectMapper, capturedEvents);
  }

  @Dado("que el administrador {string} inició sesión")
  public void theAdministratorSignedIn(String email) throws Exception {
    String token = api.signInForToken(email, ScenarioContext.PASSWORD);
    context.rememberAccessToken(email, token);
    context.setCurrentAdministratorToken(token);
  }

  @Dado("que el conductor {string} inició sesión")
  public void theDriverSignedIn(String email) throws Exception {
    if (context.accessTokenOf(email) == null) {
      String token = api.signInForToken(email, ScenarioContext.PASSWORD);
      context.rememberAccessToken(email, token);
    }
  }

  @Cuando("el administrador invita a {string} con el correo {string} y el rol {string}")
  public void theAdministratorInvites(String fullName, String email, String role) throws Exception {
    context.record(api.inviteUser(context.currentAdministratorToken(), fullName, email, role));
    if (context.statusCode() == 201) {
      context.rememberUserId(email, context.body().get("id").asText());
    }
  }

  @Dado("que el administrador invitó a {string} con el correo {string} y el rol {string}")
  public void theAdministratorAlreadyInvited(String fullName, String email, String role)
      throws Exception {
    theAdministratorInvites(fullName, email, role);
    assertThat(context.statusCode()).isEqualTo(201);
  }

  @Cuando("{string} define la contraseña {string} con su enlace de invitación")
  public void definePasswordWithTheInvitation(String email, String password) throws Exception {
    context.record(api.definePassword(invitationTokenOf(email), password));
  }

  @Dado("que {string} definió la contraseña {string} con su enlace de invitación")
  public void alreadyDefinedThePassword(String email, String password) throws Exception {
    context
        .record(api.definePassword(invitationTokenOf(email), password))
        .andExpect(status().isNoContent());
    String userToken = api.signInForToken(email, password);
    context.rememberAccessToken(email, userToken);
  }

  @Cuando("el administrador lista los usuarios")
  public void theAdministratorListsTheUsers() throws Exception {
    context.record(api.listUsers(context.currentAdministratorToken()));
  }

  @Cuando("{string} intenta listar los usuarios")
  public void tryToListTheUsers(String email) throws Exception {
    context.record(api.listUsers(context.accessTokenOf(email)));
  }

  @Cuando("el administrador lista los usuarios con el rol {string}")
  public void theAdministratorListsTheUsersWithRole(String role) throws Exception {
    context.record(api.listUsersWithRole(context.currentAdministratorToken(), role));
  }

  @Cuando("{string} lista los usuarios con el rol {string}")
  public void aUserListsTheUsersWithRole(String email, String role) throws Exception {
    context.record(api.listUsersWithRole(context.accessTokenOf(email), role));
  }

  @Cuando("{string} lista los usuarios")
  public void aUserListsTheUsers(String email) throws Exception {
    context.record(api.listUsers(context.accessTokenOf(email)));
  }

  @Cuando("el administrador cambia el rol del usuario {string} a {string}")
  public void theAdministratorChangesTheRole(String email, String role) throws Exception {
    context.record(
        api.updateUser(
            context.currentAdministratorToken(), context.userIdOf(email), Map.of("role", role)));
  }

  /** Un identificador que no pertenece a ningún usuario debe responder 404 sin revelar nada. */
  @Cuando("el administrador cambia el rol de un usuario inexistente a {string}")
  public void theAdministratorChangesRoleOfUnknownUser(String role) throws Exception {
    context.record(
        api.updateUser(
            context.currentAdministratorToken(), UUID.randomUUID().toString(), Map.of("role", role)));
  }

  @Cuando("el administrador deshabilita al usuario {string}")
  public void theAdministratorDisablesTheUser(String email) throws Exception {
    context.record(
        api.updateUser(
            context.currentAdministratorToken(),
            context.userIdOf(email),
            Map.of("status", "DISABLED")));
  }

  @Dado("que el administrador deshabilitó al usuario {string}")
  public void theAdministratorAlreadyDisabledTheUser(String email) throws Exception {
    theAdministratorDisablesTheUser(email);
    assertThat(context.statusCode()).isEqualTo(200);
  }

  @Y("el usuario queda con el rol {string} y el estado {string}")
  public void theUserHasRoleAndStatus(String role, String status) throws Exception {
    JsonNode body = context.body();
    assertThat(body.get("role").asText()).isEqualTo(role);
    assertThat(body.get("status").asText()).isEqualTo(status);
  }

  @Y("la lista contiene {int} usuarios")
  public void theListHasUsers(int expected) throws Exception {
    assertThat(context.body().size()).isEqualTo(expected);
  }

  @Y("la lista incluye al usuario {string}")
  public void theListIncludesTheUser(String email) throws Exception {
    assertThat(emailsInTheList()).contains(email);
  }

  @Y("la lista no incluye al usuario {string}")
  public void theListDoesNotIncludeTheUser(String email) throws Exception {
    assertThat(emailsInTheList()).doesNotContain(email);
  }

  private java.util.List<String> emailsInTheList() throws Exception {
    return StreamSupport.stream(context.body().spliterator(), false)
        .map(user -> user.get("email").asText())
        .toList();
  }

  private String invitationTokenOf(String email) {
    return capturedEvents.all(UserInvited.class).stream()
        .filter(event -> event.email().equals(email))
        .reduce((first, second) -> second)
        .orElseThrow()
        .invitationToken();
  }
}
