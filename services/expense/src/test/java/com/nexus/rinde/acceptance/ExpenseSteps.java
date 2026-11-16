package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rinde.support.EvidenceFiles;
import com.nexus.rinde.support.ExpenseApi;
import com.nexus.rinde.support.RelativeDates;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.web.servlet.MockMvc;

/** Pasos de aceptación BDD para el bounded context Expense & Evidence. */
public class ExpenseSteps {

  private static final String DRIVER = "luis@andes.pe";

  private final ScenarioContext context;
  private final ExpenseApi api;

  public ExpenseSteps(ScenarioContext context, MockMvc mockMvc, ObjectMapper objectMapper) {
    this.context = context;
    this.api = new ExpenseApi(mockMvc, objectMapper);
  }

  @Cuando(
      "el conductor registra un gasto de tipo {string} por el monto {string} en fecha {string}"
          + " con evidencia {string} y clave {string} en el viaje {string}")
  public void driverRegistersExpenseWithEvidence(
      String category,
      String amount,
      String date,
      String fileName,
      String idempotencyKey,
      String tripName)
      throws Exception {
    registerWithEvidence(
        context.accessTokenOf(DRIVER),
        context.tripIdOf(tripName),
        category,
        amount,
        date,
        fileName,
        idempotencyKey);
  }

  @Cuando(
      "el conductor registra un gasto de tipo {string} por el monto {string} en fecha {string}"
          + " sin evidencia y clave {string} en el viaje {string}")
  public void driverRegistersExpenseWithoutEvidence(
      String category, String amount, String date, String idempotencyKey, String tripName)
      throws Exception {
    registerExpense(
        context.accessTokenOf(DRIVER),
        context.tripIdOf(tripName),
        category,
        amount,
        date,
        idempotencyKey);
  }

  @Cuando("el conductor intenta registrar un gasto con monto {string} en el viaje {string}")
  public void driverAttemptsInvalidAmount(String amount, String tripName) throws Exception {
    context.record(
        api.register(
            context.accessTokenOf(DRIVER),
            context.tripIdOf(tripName),
            "FUEL",
            new BigDecimal(amount),
            "PEN",
            RelativeDates.date("HOY+5"),
            "KEY-INVALID-" + System.nanoTime()));
  }

  @Dado(
      "que el conductor registró un gasto de tipo {string} por el monto {string} con clave"
          + " {string} en el viaje {string}")
  public void driverRegisteredExpense(
      String category, String amount, String idempotencyKey, String tripName) throws Exception {
    api.register(
        context.accessTokenOf(DRIVER),
        context.tripIdOf(tripName),
        category,
        new BigDecimal(amount),
        "PEN",
        RelativeDates.date("HOY+5"),
        idempotencyKey);
  }

  @Cuando(
      "el conductor intenta registrar un gasto de tipo {string} por el monto {string} con clave"
          + " {string} en el viaje {string}")
  public void driverAttemptsDuplicateExpense(
      String category, String amount, String idempotencyKey, String tripName) throws Exception {
    context.record(
        api.register(
            context.accessTokenOf(DRIVER),
            context.tripIdOf(tripName),
            category,
            new BigDecimal(amount),
            "PEN",
            RelativeDates.date("HOY+5"),
            idempotencyKey));
  }

  @Cuando("una persona sin token registra un gasto en el viaje {string}")
  public void unauthenticatedRegistersExpense(String tripName) throws Exception {
    context.record(
        api.register(
            null,
            context.tripIdOf(tripName),
            "FUEL",
            new BigDecimal("100.00"),
            "PEN",
            RelativeDates.date("HOY+5"),
            "KEY-NOAUTH"));
  }

  @Dado(
      "que el conductor registró un gasto {string} de tipo {string} por el monto {string} con"
          + " clave {string} en el viaje {string}")
  public void driverRegisteredExpenseNamed(
      String expenseName, String category, String amount, String idempotencyKey, String tripName)
      throws Exception {
    registerExpense(
        context.accessTokenOf(DRIVER),
        context.tripIdOf(tripName),
        category,
        amount,
        "HOY+5",
        idempotencyKey);
    rememberLastExpense(expenseName);
  }

  @Dado(
      "que el conductor registró un gasto {string} de tipo {string} por el monto {string} con"
          + " evidencia {string} y clave {string} en el viaje {string}")
  public void driverRegisteredExpenseNamedWithEvidence(
      String expenseName,
      String category,
      String amount,
      String fileName,
      String idempotencyKey,
      String tripName)
      throws Exception {
    registerWithEvidence(
        context.accessTokenOf(DRIVER),
        context.tripIdOf(tripName),
        category,
        amount,
        "HOY+5",
        fileName,
        idempotencyKey);
    rememberLastExpense(expenseName);
  }

  @Dado(
      "que el conductor {string} registró el gasto {string} de tipo {string} por el monto {string}"
          + " con clave {string} en el viaje {string}")
  public void namedDriverRegistersExpense(
      String email,
      String expenseName,
      String category,
      String amount,
      String idempotencyKey,
      String tripName)
      throws Exception {
    registerExpense(
        context.accessTokenOf(email),
        context.tripIdOf(tripName),
        category,
        amount,
        "HOY+5",
        idempotencyKey);
    assertThat(context.statusCode()).isEqualTo(201);
    rememberLastExpense(expenseName);
  }

  @Cuando(
      "el conductor {string} intenta registrar un gasto de tipo {string} por el monto {string}"
          + " con clave {string} en el viaje {string}")
  public void namedDriverAttemptsExpense(
      String email, String category, String amount, String idempotencyKey, String tripName)
      throws Exception {
    registerExpense(
        context.accessTokenOf(email),
        context.tripIdOf(tripName),
        category,
        amount,
        "HOY+5",
        idempotencyKey);
  }

  @Cuando(
      "el conductor intenta registrar un gasto de tipo {string} por el monto {string} en un viaje"
          + " que no existe")
  public void driverAttemptsExpenseOnUnknownTrip(String category, String amount)
      throws Exception {
    context.record(
        api.register(
            context.accessTokenOf(DRIVER),
            UUID.randomUUID().toString(),
            category,
            new BigDecimal(amount),
            "PEN",
            RelativeDates.date("HOY+5"),
            "KEY-UNKNOWN-" + System.nanoTime()));
  }

  @Cuando("el conductor sube el comprobante {string} al último gasto registrado")
  public void driverUploadsEvidence(String fileName) throws Exception {
    uploadTo(context.accessTokenOf(DRIVER), lastExpenseId(), fileName, null);
  }

  @Cuando(
      "el conductor sube el comprobante {string} de {int} bytes al último gasto registrado")
  public void driverUploadsEvidenceOfSize(String fileName, int size) throws Exception {
    uploadTo(context.accessTokenOf(DRIVER), lastExpenseId(), fileName, size);
  }

  @Cuando("el conductor sube sin archivo al último gasto registrado")
  public void driverUploadsWithoutFile() throws Exception {
    context.record(api.uploadWithoutFile(context.accessTokenOf(DRIVER), lastExpenseId()));
  }

  @Cuando("el conductor {string} intenta subir el comprobante {string} al último gasto registrado")
  public void userAttemptsUpload(String email, String fileName) throws Exception {
    uploadTo(context.accessTokenOf(email), lastExpenseId(), fileName, null);
  }

  @Cuando("el administrador {string} intenta subir el comprobante {string} al último gasto registrado")
  public void administratorAttemptsUpload(String email, String fileName) throws Exception {
    uploadTo(context.accessTokenOf(email), lastExpenseId(), fileName, null);
  }

  @Cuando("una persona sin token sube el comprobante {string} al último gasto registrado")
  public void unauthenticatedUploads(String fileName) throws Exception {
    uploadTo(null, lastExpenseId(), fileName, null);
  }

  @Cuando("el conductor {string} abre el comprobante del último gasto registrado")
  public void userOpensEvidence(String email) throws Exception {
    openEvidence(email, lastExpenseId());
  }

  @Cuando("el administrador {string} abre el comprobante del último gasto registrado")
  public void administratorOpensEvidence(String email) throws Exception {
    openEvidence(email, lastExpenseId());
  }

  @Y("la respuesta tiene el tipo de contenido {string}")
  public void responseHasContentType(String expected) {
    assertThat(context.lastContentType()).isEqualTo(expected);
  }

  @Y("el contenido recibido coincide con el archivo {string}")
  public void receivedContentMatchesFile(String fileName) {
    assertThat(context.lastResponseBytes()).isEqualTo(EvidenceFiles.contentOf(fileName));
  }

  @Y("la respuesta incluye el campo anidado {string} con el valor {string}")
  public void responseHasNestedField(String path, String expected) throws Exception {
    JsonNode node = context.body();
    for (String part : path.split("\\.")) {
      node = node.get(part);
    }
    assertThat(node).isNotNull();
    assertThat(node.asText()).isEqualTo(expected);
  }

  @Cuando("el conductor consulta los gastos del viaje {string}")
  public void driverQueriesExpenses(String tripName) throws Exception {
    context.record(api.listByTrip(context.accessTokenOf(DRIVER), context.tripIdOf(tripName)));
  }

  @Y("la lista de gastos contiene {int} elementos")
  public void expenseListHasSize(int expected) throws Exception {
    assertThat(context.body().isArray()).isTrue();
    assertThat(context.body().size()).isEqualTo(expected);
  }

  @Cuando("el conductor consulta el detalle del gasto {string}")
  public void driverQueriesExpenseDetail(String expenseName) throws Exception {
    context.record(
        api.detail(context.accessTokenOf(DRIVER), context.expenseIdOf(expenseName)));
  }

  @Cuando("el administrador {string} consulta el detalle del gasto {string}")
  public void adminQueriesExpenseDetail(String adminEmail, String expenseName) throws Exception {
    context.record(
        api.detail(context.accessTokenOf(adminEmail), context.expenseIdOf(expenseName)));
  }

  @Cuando("el administrador aprueba el gasto {string}")
  public void adminApprovesExpense(String expenseName) throws Exception {
    context.record(
        api.updateStatus(
            context.accessTokenOf("ana@andes.pe"),
            context.expenseIdOf(expenseName),
            "APPROVED",
            null));
  }

  @Cuando("el administrador observa el gasto {string} con el motivo {string}")
  public void adminObservesExpense(String expenseName, String reason) throws Exception {
    context.record(
        api.updateStatus(
            context.accessTokenOf("ana@andes.pe"),
            context.expenseIdOf(expenseName),
            "OBSERVED",
            reason));
  }

  @Cuando("el administrador intenta observar el gasto {string} sin motivo")
  public void adminObservesWithoutReason(String expenseName) throws Exception {
    context.record(
        api.updateStatus(
            context.accessTokenOf("ana@andes.pe"),
            context.expenseIdOf(expenseName),
            "OBSERVED",
            ""));
  }

  @Cuando("el conductor {string} intenta aprobar el gasto {string}")
  public void driverAttemptsApprove(String driverEmail, String expenseName) throws Exception {
    context.record(
        api.updateStatus(
            context.accessTokenOf(driverEmail),
            context.expenseIdOf(expenseName),
            "APPROVED",
            null));
  }

  @Dado("que el administrador {string} aprobó el gasto {string}")
  public void namedAdministratorApprovesExpense(String email, String expenseName) throws Exception {
    context.record(
        api.updateStatus(context.accessTokenOf(email), context.expenseIdOf(expenseName), "APPROVED", null));
    assertThat(context.statusCode()).isEqualTo(200);
  }

  @Cuando("el conductor sincroniza un lote con un gasto para el viaje {string} y otro para un viaje que"
      + " no existe")
  public void driverSyncsBatchWithUnknownTrip(String tripName) throws Exception {
    List<Map<String, Object>> items = new ArrayList<>();
    items.add(syncItem(context.tripIdOf(tripName), "KEY-SYNC-VALIDO-" + System.nanoTime()));
    items.add(syncItem(UUID.randomUUID().toString(), "KEY-SYNC-INEXISTENTE-" + System.nanoTime()));
    context.record(api.sync(context.accessTokenOf(DRIVER), items));
  }

  @Cuando(
      "el conductor sincroniza un lote con un gasto para el viaje {string} y otro para el viaje"
          + " {string}")
  public void driverSyncsBatchWithTwoInvalidTrips(String firstTrip, String secondTrip)
      throws Exception {
    List<Map<String, Object>> items = new ArrayList<>();
    items.add(syncItem(tripIdOrRandom(firstTrip), "KEY-TODOS-INVALIDOS-1-" + System.nanoTime()));
    items.add(syncItem(tripIdOrRandom(secondTrip), "KEY-TODOS-INVALIDOS-2-" + System.nanoTime()));
    context.record(api.sync(context.accessTokenOf(DRIVER), items));
  }

  @Cuando(
      "el conductor sincroniza el lote con las claves {string} y {string} para el viaje {string}")
  public void driverSyncsBatchWithKeys(String firstKey, String secondKey, String tripName)
      throws Exception {
    String tripId = context.tripIdOf(tripName);
    List<Map<String, Object>> items = new ArrayList<>();
    items.add(syncItem(tripId, firstKey));
    items.add(syncItem(tripId, secondKey));
    context.record(api.sync(context.accessTokenOf(DRIVER), items));
  }

  @Cuando("el conductor sincroniza un lote con {int} gastos para el viaje {string}")
  public void driverSyncsBatch(int count, String tripName) throws Exception {
    String tripId = context.tripIdOf(tripName);
    List<Map<String, Object>> items = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      items.add(syncItem(tripId, "KEY-SYNC-" + i + "-" + System.nanoTime()));
    }
    context.record(api.sync(context.accessTokenOf(DRIVER), items));
  }

  @Cuando(
      "el conductor sincroniza un lote que incluye el gasto con clave {string} y uno nuevo con"
          + " clave {string} para el viaje {string}")
  public void driverSyncsMixedBatch(String existingKey, String newKey, String tripName)
      throws Exception {
    String tripId = context.tripIdOf(tripName);
    List<Map<String, Object>> items = new ArrayList<>();
    items.add(syncItem(tripId, existingKey, "FUEL", "100.00"));
    items.add(syncItem(tripId, newKey, "TOLL", "15.00"));
    context.record(api.sync(context.accessTokenOf(DRIVER), items));
  }

  @Cuando("el conductor sincroniza un gasto con una URL de comprobante heredada para el viaje {string}")
  public void driverSyncsLegacyEvidenceItem(String tripName) throws Exception {
    Map<String, Object> item = syncItem(context.tripIdOf(tripName), "KEY-HEREDADA-" + System.nanoTime());
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("imageUrl", "https://storage.rinde.pe/evidences/heredada.jpg");
    evidence.put("fileSizeBytes", 2048L);
    item.put("evidence", evidence);
    context.record(api.sync(context.accessTokenOf(DRIVER), List.of(item)));
  }

  @Cuando("el conductor sube el comprobante {string} al primer gasto sincronizado")
  public void driverUploadsToFirstSynchronized(String fileName) throws Exception {
    String expenseId = context.body().get("items").get(0).get("id").asText();
    uploadTo(context.accessTokenOf(DRIVER), expenseId, fileName, null);
  }

  @Y("el primer gasto sincronizado tiene el estado {string} y no tiene comprobante")
  public void firstSynchronizedHasStatusWithoutEvidence(String status) throws Exception {
    JsonNode first = context.body().get("items").get(0);
    assertThat(first.get("status").asText()).isEqualTo(status);
    assertThat(first.get("evidence") == null || first.get("evidence").isNull()).isTrue();
  }

  @Cuando("una persona sin token sincroniza un lote de gastos para el viaje {string}")
  public void unauthenticatedSyncsBatch(String tripName) throws Exception {
    context.record(
        api.sync(null, List.of(syncItem(context.tripIdOf(tripName), "KEY-NOAUTH-SYNC", "FUEL", "50.00"))));
  }

  @Y("la respuesta no incluye el gasto {string}")
  public void responseDoesNotIncludeExpense(String expenseName) throws Exception {
    assertThat(context.lastResponseBody()).doesNotContain(context.expenseIdOf(expenseName));
  }

  @Y("el elemento {int} de rechazados tiene el campo {string} con el valor {string}")
  public void rejectedItemHasField(int index, String field, String expected) throws Exception {
    assertThat(context.body().get("rejected").get(index).get(field).asText()).isEqualTo(expected);
  }

  private void registerExpense(
      String token,
      String tripId,
      String category,
      String amount,
      String date,
      String idempotencyKey)
      throws Exception {
    context.record(
        api.register(
            token,
            tripId,
            category,
            new BigDecimal(amount),
            "PEN",
            RelativeDates.date(date),
            idempotencyKey));
  }

  /** Registra el gasto y, si se creó, sube su comprobante. Queda grabada la última respuesta. */
  private void registerWithEvidence(
      String token,
      String tripId,
      String category,
      String amount,
      String date,
      String fileName,
      String idempotencyKey)
      throws Exception {
    registerExpense(token, tripId, category, amount, date, idempotencyKey);
    if (context.statusCode() == 201) {
      uploadTo(token, context.body().get("id").asText(), fileName, null);
    }
  }

  /** Sube el comprobante y graba la respuesta. Con size, el archivo tiene ese tamaño exacto. */
  private void uploadTo(String token, String expenseId, String fileName, Integer size)
      throws Exception {
    byte[] content =
        size == null
            ? EvidenceFiles.contentOf(fileName)
            : EvidenceFiles.sized(fileName, size);
    context.record(api.uploadEvidence(token, expenseId, fileName, content));
  }

  private void openEvidence(String email, String expenseId) throws Exception {
    String token = context.accessTokenOf(email);
    var detail = api.detail(token, expenseId);
    if (detail.andReturn().getResponse().getStatus() != 200) {
      // Sin acceso al gasto, la respuesta del detalle es el error que se verifica.
      context.record(detail);
      return;
    }
    String detailJson = detail.andReturn().getResponse().getContentAsString();
    String contentUrl = context.readJson(detailJson).get("evidence").get("contentUrl").asText();
    context.record(api.content(token, contentUrl));
  }

  private void rememberLastExpense(String expenseName) throws Exception {
    if (context.statusCode() == 201) {
      context.rememberExpenseId(expenseName, context.body().get("id").asText());
    }
  }

  private String lastExpenseId() throws Exception {
    return context.body().get("id").asText();
  }

  /** Un viaje no existente se identifica con un UUID aleatorio. */
  private String tripIdOrRandom(String tripName) {
    String tripId = context.tripIdOf(tripName);
    return tripId != null ? tripId : UUID.randomUUID().toString();
  }

  private static Map<String, Object> syncItem(String tripId, String idempotencyKey) {
    return syncItem(tripId, idempotencyKey, "FOOD", "25.00");
  }

  private static Map<String, Object> syncItem(
      String tripId, String idempotencyKey, String category, String amount) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("tripId", tripId);
    item.put("category", category);
    item.put("amount", new BigDecimal(amount));
    item.put("currency", "PEN");
    item.put("expenseDate", RelativeDates.resolve("HOY+5"));
    item.put("idempotencyKey", idempotencyKey);
    return item;
  }
}
