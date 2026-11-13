package com.nexus.rinde.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Cliente de pruebas para los endpoints de Settlement. */
public class SettlementApi {

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;

  public SettlementApi(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  public ResultActions registerAdvance(String token, String tripId, BigDecimal amount, String currency)
      throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("tripId", tripId);
    body.put("amount", amount);
    if (currency != null) {
      body.put("currency", currency);
    }
    return perform(post("/api/v1/settlements/advances"), token, body);
  }

  public ResultActions getById(String token, String settlementId) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/settlements/" + settlementId), token));
  }

  public ResultActions getByTrip(String token, String tripId) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/trips/" + tripId + "/settlement"), token));
  }

  public ResultActions recalculate(String token, String settlementId) throws Exception {
    return mockMvc.perform(
        authorized(post("/api/v1/settlements/" + settlementId + "/recalculation"), token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"));
  }

  public ResultActions close(String token, String settlementId) throws Exception {
    return mockMvc.perform(
        authorized(post("/api/v1/settlements/" + settlementId + "/close"), token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"));
  }

  public ResultActions export(String token, String settlementId) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/settlements/" + settlementId + "/export"), token));
  }

  private ResultActions perform(
      MockHttpServletRequestBuilder request, String token, Object body) throws Exception {
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
