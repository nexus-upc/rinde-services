package com.nexus.rinde.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Cliente de pruebas para los endpoints de Incident Management. */
public class IncidentApi {

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;

  public IncidentApi(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  public ResultActions report(
      String token,
      String tripId,
      String type,
      String description,
      String evidenceUrl,
      String idempotencyKey)
      throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", type);
    body.put("description", description);
    if (evidenceUrl != null) {
      body.put("evidenceUrl", evidenceUrl);
    }
    if (idempotencyKey != null) {
      body.put("idempotencyKey", idempotencyKey);
    }
    return perform(post("/api/v1/trips/" + tripId + "/incidents"), token, body);
  }

  public ResultActions list(String token, String status) throws Exception {
    MockHttpServletRequestBuilder request = get("/api/v1/incidents");
    if (status != null) {
      request = request.param("status", status);
    }
    return mockMvc.perform(authorized(request, token));
  }

  public ResultActions detail(String token, String incidentId) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/incidents/" + incidentId), token));
  }

  public ResultActions changeStatus(String token, String incidentId, String status)
      throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", status);
    return perform(patch("/api/v1/incidents/" + incidentId + "/status"), token, body);
  }

  public ResultActions close(String token, String incidentId, String action) throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("action", action);
    return perform(post("/api/v1/incidents/" + incidentId + "/close"), token, body);
  }

  private ResultActions perform(MockHttpServletRequestBuilder request, String token, Object body)
      throws Exception {
    return mockMvc.perform(
        authorized(request, token)
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(objectMapper.writeValueAsString(body)));
  }

  private MockHttpServletRequestBuilder authorized(
      MockHttpServletRequestBuilder request, String token) {
    return token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }
}
