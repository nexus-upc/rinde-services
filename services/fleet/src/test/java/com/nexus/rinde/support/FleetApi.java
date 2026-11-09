package com.nexus.rinde.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Cliente de pruebas para los endpoints del Bounded Context Fleet & Maintenance. */
public class FleetApi {

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;

  public FleetApi(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  public ResultActions registerVehicle(
      String token,
      String plateNumber,
      String brand,
      String model,
      int modelYear,
      BigDecimal payloadCapacityKg)
      throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("plateNumber", plateNumber);
    body.put("brand", brand);
    body.put("model", model);
    body.put("modelYear", modelYear);
    body.put("payloadCapacityKg", payloadCapacityKg);
    return perform(post("/api/v1/vehicles"), token, body);
  }

  public ResultActions registerVehicleRaw(String token, Map<String, Object> body) throws Exception {
    return perform(post("/api/v1/vehicles"), token, body);
  }

  public ResultActions listVehicles(String token) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/vehicles"), token));
  }

  public ResultActions getVehicle(String token, String vehicleId) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/vehicles/" + vehicleId), token));
  }

  public ResultActions getVehicleHealthStatus(String token, String vehicleId) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/vehicles/" + vehicleId + "/health-status"), token));
  }

  public ResultActions recordMaintenance(
      String token,
      String vehicleId,
      String maintenanceType,
      LocalDate executionDate,
      Integer mileage,
      BigDecimal cost,
      LocalDate nextMaintenanceDate,
      String notes)
      throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("vehicleId", vehicleId != null ? UUID.fromString(vehicleId) : null);
    body.put("maintenanceType", maintenanceType);
    body.put("executionDate", executionDate);
    body.put("mileage", mileage);
    body.put("cost", cost);
    body.put("nextMaintenanceDate", nextMaintenanceDate);
    body.put("notes", notes);
    return perform(post("/api/v1/vehicles/" + vehicleId + "/maintenances"), token, body);
  }

  public ResultActions listMaintenanceAlerts(String token) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/maintenances/alerts"), token));
  }

  public ResultActions registerDriver(
      String token,
      UUID userId,
      String fullName,
      String documentType,
      String documentNumber,
      String licenseNumber,
      String licenseCategory,
      LocalDate licenseExpirationDate)
      throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    if (userId != null) {
      body.put("userId", userId);
    }
    body.put("fullName", fullName);
    body.put("documentType", documentType);
    body.put("documentNumber", documentNumber);
    body.put("licenseNumber", licenseNumber);
    body.put("licenseCategory", licenseCategory);
    body.put("licenseExpirationDate", licenseExpirationDate);
    return perform(post("/api/v1/drivers"), token, body);
  }

  public ResultActions listDrivers(String token) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/drivers"), token));
  }

  public ResultActions getDriver(String token, String driverId) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/drivers/" + driverId), token));
  }

  public ResultActions checkDriverEligibility(String token, String driverId) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/drivers/" + driverId + "/eligibility"), token));
  }

  /** Cambia la cuenta del conductor; con userId nulo retira el vínculo. */
  public ResultActions updateDriverAccount(String token, String driverId, UUID userId)
      throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("userId", userId);
    return perform(put("/api/v1/drivers/" + driverId + "/account"), token, body);
  }

  public ResultActions getCurrentDriver(String token) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/drivers/me"), token));
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
