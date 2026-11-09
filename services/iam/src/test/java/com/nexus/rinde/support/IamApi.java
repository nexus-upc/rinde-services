package com.nexus.rinde.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.iam.domain.model.events.TenantRegistered;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Cliente de pruebas para los endpoints de IAM, compartido por las pruebas de integración y de
 * aceptación.
 */
public class IamApi {

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  private final CapturedEvents capturedEvents;

  public IamApi(MockMvc mockMvc, ObjectMapper objectMapper, CapturedEvents capturedEvents) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.capturedEvents = capturedEvents;
  }

  public ResultActions registerTenant(
      String tradeName,
      String ruc,
      String administratorName,
      String administratorEmail,
      String password)
      throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("tradeName", tradeName);
    body.put("ruc", ruc);
    body.put("administratorFullName", administratorName);
    body.put("administratorEmail", administratorEmail);
    body.put("password", password);
    return perform(post("/api/v1/tenants"), null, body);
  }

  public ResultActions verifyTenant(String tenantId, String token) throws Exception {
    return perform(
        post("/api/v1/tenants/" + tenantId + "/verification"), null, Map.of("token", token));
  }

  public ResultActions signIn(String email, String password) throws Exception {
    return perform(
        post("/api/v1/auth/sign-in"), null, Map.of("email", email, "password", password));
  }

  public ResultActions requestPasswordReset(String email) throws Exception {
    return perform(post("/api/v1/auth/password-reset"), null, Map.of("email", email));
  }

  public ResultActions definePassword(String token, String password) throws Exception {
    return perform(
        post("/api/v1/auth/password"), null, Map.of("token", token, "password", password));
  }

  public ResultActions listUsers(String accessToken) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/users"), accessToken));
  }

  public ResultActions listUsersWithRole(String accessToken, String role) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/users").param("role", role), accessToken));
  }

  public ResultActions currentUser(String accessToken) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/users/me"), accessToken));
  }

  public ResultActions inviteUser(String accessToken, String fullName, String email, String role)
      throws Exception {
    return perform(
        post("/api/v1/users"),
        accessToken,
        Map.of("fullName", fullName, "email", email, "role", role));
  }

  public ResultActions updateUser(String accessToken, String userId, Map<String, Object> body)
      throws Exception {
    return perform(patch("/api/v1/users/" + userId), accessToken, body);
  }

  /** Registra la empresa, confirma su correo y devuelve el id de la empresa. */
  public String registerAndVerifyTenant(String ruc, String administratorEmail, String password)
      throws Exception {
    String response =
        registerTenant("Empresa " + ruc, ruc, "Administrador " + ruc, administratorEmail, password)
            .andReturn()
            .getResponse()
            .getContentAsString();
    String tenantId = objectMapper.readTree(response).get("id").asText();
    String token = capturedEvents.last(TenantRegistered.class).orElseThrow().verificationToken();
    verifyTenant(tenantId, token);
    return tenantId;
  }

  /** Inicia sesión y devuelve solo el token de acceso. */
  public String signInForToken(String email, String password) throws Exception {
    return json(signIn(email, password)).get("accessToken").asText();
  }

  public JsonNode json(ResultActions result) throws Exception {
    return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
  }

  private ResultActions perform(
      MockHttpServletRequestBuilder request, String accessToken, Object body) throws Exception {
    return mockMvc.perform(
        authorized(request, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(objectMapper.writeValueAsString(body)));
  }

  private MockHttpServletRequestBuilder authorized(
      MockHttpServletRequestBuilder request, String accessToken) {
    return accessToken == null
        ? request
        : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
  }
}
