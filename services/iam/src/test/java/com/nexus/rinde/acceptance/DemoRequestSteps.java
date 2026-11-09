package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Pasos de US06: envío, validación y repetición de solicitudes de demostración. */
public class DemoRequestSteps {

  private static final String PATH = "/api/v1/public/demo-requests";
  private final ScenarioContext context;
  private final MockMvc mvc;
  private final ObjectMapper mapper;
  private final JdbcTemplate jdbc;
  private final Map<String, Object> payload = new LinkedHashMap<>();

  public DemoRequestSteps(
      ScenarioContext context, MockMvc mvc, ObjectMapper mapper, JdbcTemplate jdbc) {
    this.context = context;
    this.mvc = mvc;
    this.mapper = mapper;
    this.jdbc = jdbc;
  }

  @Dado("que el visitante completa la solicitud de demostración con datos válidos")
  public void validPayload() {
    usePayloadWithEmail("ana.rojas@transportesandes.pe");
  }

  @Dado("que ya se envió una solicitud de demostración con el correo {string}")
  public void alreadySent(String email) throws Exception {
    usePayloadWithEmail(email);
    send();
    assertThat(context.statusCode()).isEqualTo(201);
  }

  @Dado("que la solicitud se recibió hace más de 10 minutos")
  public void receivedLongAgo() {
    jdbc.update("update iam.demo_requests set received_at = received_at - interval '11 minutes'");
  }

  @Y("el campo {string} tiene el valor {string}")
  public void fieldHasValue(String field, String value) {
    payload.put(field, valueOf(field, value));
  }

  @Y("el mensaje tiene {int} caracteres")
  public void messageLength(int length) {
    payload.put("message", "a".repeat(length));
  }

  @Cuando("el visitante envía la solicitud de demostración")
  public void submit() throws Exception {
    send();
  }

  @Cuando("el visitante envía otra solicitud de demostración con el correo {string}")
  public void submitAgain(String email) throws Exception {
    usePayloadWithEmail(email);
    send();
  }

  @Y("la respuesta incluye un identificador y la fecha de recepción")
  public void receiptHasIdAndDate() throws Exception {
    assertThat(context.body().get("id").asText()).isNotBlank();
    assertThat(context.body().get("receivedAt").asText()).isNotBlank();
  }

  @Y("el error del campo {string} es {string}")
  public void fieldErrorIs(String field, String expected) throws Exception {
    assertThat(context.body().get("errors").get(field).asText()).isEqualTo(expected);
  }

  @Y("no se registra ninguna solicitud de demostración")
  public void noneRegistered() {
    assertThat(countAll()).isZero();
  }

  @Y("se registra una sola solicitud de demostración")
  public void oneRegistered() {
    assertThat(countAll()).isEqualTo(1);
  }

  @Y("se registran {int} solicitudes de demostración")
  public void registered(int expected) {
    assertThat(countAll()).isEqualTo(expected);
  }

  @Y("se registra una solicitud de demostración para el correo {string}")
  public void registeredForEmail(String email) {
    assertThat(countByEmail(email)).isEqualTo(1);
  }

  @Y("se registra la solicitud con el teléfono {string}")
  public void registeredWithPhone(String phone) {
    Long count =
        jdbc.queryForObject(
            "select count(*) from iam.demo_requests where phone = ?", Long.class, phone);
    assertThat(count).isEqualTo(1L);
  }

  private void usePayloadWithEmail(String email) {
    payload.clear();
    payload.put("fullName", "Ana Rojas Paredes");
    payload.put("companyName", "Transportes Andes SAC");
    payload.put("email", email);
    payload.put("fleetSize", 12);
  }

  private void send() throws Exception {
    context.record(
        mvc.perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload))));
  }

  /** Un valor vacío equivale a un campo ausente; la cantidad se envía como número. */
  private static Object valueOf(String field, String raw) {
    if (raw.isBlank()) {
      return null;
    }
    return "fleetSize".equals(field) ? Integer.valueOf(raw) : raw;
  }

  private long countAll() {
    return jdbc.queryForObject("select count(*) from iam.demo_requests", Long.class);
  }

  private long countByEmail(String email) {
    return jdbc.queryForObject(
        "select count(*) from iam.demo_requests where email = ?",
        Long.class,
        email.toLowerCase(Locale.ROOT));
  }
}
