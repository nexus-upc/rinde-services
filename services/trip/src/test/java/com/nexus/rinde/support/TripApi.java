package com.nexus.rinde.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Cliente de pruebas para los siete endpoints de Trip Management. */
public class TripApi {

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;

  public TripApi(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  public ResultActions schedule(
      String token,
      String origin,
      String destination,
      String cargoDescription,
      BigDecimal cargoWeightKg,
      LocalDate departureDate,
      boolean confirmPastDate)
      throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("origin", origin);
    body.put("destination", destination);
    body.put("cargoDescription", cargoDescription);
    if (cargoWeightKg != null) {
      body.put("cargoWeightKg", cargoWeightKg);
    }
    body.put("departureDate", departureDate);
    body.put("confirmPastDate", confirmPastDate);
    return perform(post("/api/v1/trips"), token, body);
  }

  public ResultActions scheduleRaw(String token, Map<String, Object> body) throws Exception {
    return perform(post("/api/v1/trips"), token, body);
  }

  public ResultActions assign(
      String token,
      String tripId,
      String vehicleId,
      String driverId,
      boolean confirmOverdueMaintenance)
      throws Exception {
    return perform(
        put("/api/v1/trips/" + tripId + "/assignment"),
        token,
        Map.of(
            "vehicleId",
            vehicleId,
            "driverId",
            driverId,
            "confirmOverdueMaintenance",
            confirmOverdueMaintenance));
  }

  public ResultActions list(String token, String status) throws Exception {
    MockHttpServletRequestBuilder request = get("/api/v1/trips");
    if (status != null) {
      request.param("status", status);
    }
    return mockMvc.perform(authorized(request, token));
  }

  public ResultActions detail(String token, String tripId) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/trips/" + tripId), token));
  }

  public ResultActions assignedToMe(String token) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/trips/assigned-to-me"), token));
  }

  public ResultActions start(String token, String tripId) throws Exception {
    return mockMvc.perform(authorized(post("/api/v1/trips/" + tripId + "/start"), token));
  }

  public ResultActions finish(String token, String tripId) throws Exception {
    return mockMvc.perform(authorized(post("/api/v1/trips/" + tripId + "/finish"), token));
  }

  public ResultActions recordLocations(String token, String tripId, Object body)
      throws Exception {
    return perform(post("/api/v1/trips/" + tripId + "/locations"), token, body);
  }

  public ResultActions latestLocations(String token) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/trips/locations/latest"), token));
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
