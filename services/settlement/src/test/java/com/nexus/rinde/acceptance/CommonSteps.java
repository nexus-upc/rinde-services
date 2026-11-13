package com.nexus.rinde.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;

/** Pasos de verificación comunes: código HTTP, errores y campos. */
public class CommonSteps {

  private final ScenarioContext context;

  public CommonSteps(ScenarioContext context) {
    this.context = context;
  }

  @Entonces("la respuesta tiene código {int}")
  public void theResponseHasStatus(int expected) {
    assertThat(context.statusCode()).isEqualTo(expected);
  }

  @Y("el mensaje de error es {string}")
  public void theErrorMessageIs(String expected) throws Exception {
    assertThat(context.body().get("detail").asText()).isEqualTo(expected);
  }

  @Y("el mensaje de error contiene {string}")
  public void theErrorMessageContains(String expected) throws Exception {
    assertThat(context.body().get("detail").asText()).contains(expected);
  }

  @Y("la respuesta incluye el campo {string} con el valor {string}")
  public void theResponseHasField(String field, String expected) throws Exception {
    assertThat(context.body().get(field).asText()).isEqualTo(expected);
  }
}
