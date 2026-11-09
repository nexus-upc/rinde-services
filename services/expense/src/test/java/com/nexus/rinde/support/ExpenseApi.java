package com.nexus.rinde.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/** Cliente de pruebas para los endpoints de Expense & Evidence. */
public class ExpenseApi {

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;

  public ExpenseApi(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  public ResultActions register(
      String token,
      String tripId,
      String category,
      BigDecimal amount,
      String currency,
      LocalDate expenseDate,
      String idempotencyKey)
      throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("category", category);
    body.put("amount", amount);
    body.put("currency", currency != null ? currency : "PEN");
    body.put("expenseDate", expenseDate);
    body.put("idempotencyKey", idempotencyKey);
    return perform(post("/api/v1/trips/" + tripId + "/expenses"), token, body);
  }

  public ResultActions registerRaw(String token, String tripId, Map<String, Object> body)
      throws Exception {
    return perform(post("/api/v1/trips/" + tripId + "/expenses"), token, body);
  }

  public ResultActions listByTrip(String token, String tripId) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/trips/" + tripId + "/expenses"), token));
  }

  public ResultActions updateStatus(String token, String expenseId, String status, String reason)
      throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", status);
    if (reason != null) {
      body.put("reason", reason);
    }
    return perform(patch("/api/v1/expenses/" + expenseId + "/status"), token, body);
  }

  public ResultActions detail(String token, String expenseId) throws Exception {
    return mockMvc.perform(authorized(get("/api/v1/expenses/" + expenseId), token));
  }

  /** Sube el comprobante en el campo file, como lo hace el cliente multipart. */
  public ResultActions uploadEvidence(String token, String expenseId, String fileName, byte[] content)
      throws Exception {
    MockMultipartHttpServletRequestBuilder request =
        multipart("/api/v1/expenses/" + expenseId + "/evidences")
            .file(
                new MockMultipartFile(
                    "file", fileName, EvidenceFiles.contentTypeOf(fileName), content));
    return mockMvc.perform(authorized(request, token));
  }

  /** Envía la solicitud multipart sin el campo file. */
  public ResultActions uploadWithoutFile(String token, String expenseId) throws Exception {
    return mockMvc.perform(
        authorized(multipart("/api/v1/expenses/" + expenseId + "/evidences"), token));
  }

  /** Lee el archivo de un comprobante por la ruta contentUrl que devuelve el gasto. */
  public ResultActions content(String token, String contentUrl) throws Exception {
    return mockMvc.perform(authorized(get(contentUrl), token));
  }

  public ResultActions sync(String token, java.util.List<?> items) throws Exception {
    return perform(post("/api/v1/expenses/sync"), token, items);
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
