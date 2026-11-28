package com.nexus.rinde.notification.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IncidentReportedListenerTest {

  @Test
  void messageUsesSpanishLabelsInsteadOfInternalType() {
    assertThat(IncidentReportedListener.incidentMessage("BREAKDOWN"))
        .isEqualTo("Se reportó una incidencia por falla mecánica para revisar.");
    assertThat(IncidentReportedListener.incidentMessage("DELAY"))
        .isEqualTo("Se reportó una incidencia por demora o retraso para revisar.");
    assertThat(IncidentReportedListener.incidentMessage("OTHER"))
        .isEqualTo("Se reportó una incidencia para revisar.");
    assertThat(IncidentReportedListener.incidentMessage(null))
        .isEqualTo("Se reportó una incidencia para revisar.");
  }
}
