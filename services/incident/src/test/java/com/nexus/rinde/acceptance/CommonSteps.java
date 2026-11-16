package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.incident.domain.model.events.IncidentClosed;
import com.nexus.rinde.incident.domain.model.events.IncidentReported;
import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import com.nexus.rinde.support.CapturedEvents;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.util.List;

/** Pasos de verificación comunes: código HTTP, errores, campos y eventos publicados. */
public class CommonSteps {

  private final ScenarioContext context;
  private final CapturedEvents capturedEvents;

  public CommonSteps(ScenarioContext context, CapturedEvents capturedEvents) {
    this.context = context;
    this.capturedEvents = capturedEvents;
  }

  @Entonces("la respuesta tiene código {int}")
  public void theResponseHasStatus(int expected) {
    assertThat(context.statusCode()).isEqualTo(expected);
  }

  @Y("el mensaje de error es {string}")
  public void theErrorMessageIs(String expected) throws Exception {
    assertThat(context.body().get("detail").asText()).isEqualTo(expected);
  }

  @Y("la respuesta incluye el campo {string} con el valor {string}")
  public void theResponseHasField(String field, String expected) throws Exception {
    assertThat(context.body().get(field).asText()).isEqualTo(expected);
  }

  @Y("se publica el evento {string} para {string}")
  public void theEventIsPublishedFor(String type, String email) {
    List<IntegrationEvent> events = capturedEvents.all(IntegrationEvent.class);
    assertThat(events)
        .anySatisfy(
            event -> {
              assertThat(event.type()).isEqualTo(type);
              assertThat(emailOf(event)).isEqualTo(email);
            });
  }

  @Y("no se publica el evento {string}")
  public void theEventIsNotPublished(String type) {
    assertThat(capturedEvents.all(IntegrationEvent.class))
        .noneSatisfy(event -> assertThat(event.type()).isEqualTo(type));
  }

  private String emailOf(IntegrationEvent event) {
    if (event instanceof IncidentReported reported) {
      return context.emailOfUserId(reported.reportedBy().toString());
    }
    if (event instanceof IncidentClosed closed) {
      return context.emailOfUserId(closed.closedBy().toString());
    }
    throw new IllegalArgumentException("Evento sin correo: " + event.type());
  }
}
