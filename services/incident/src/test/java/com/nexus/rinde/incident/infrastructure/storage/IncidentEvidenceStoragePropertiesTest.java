package com.nexus.rinde.incident.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IncidentEvidenceStoragePropertiesTest {

  @Test
  void acceptsLocalStorageByDefault() {
    IncidentEvidenceStorageProperties properties = new IncidentEvidenceStorageProperties();
    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  void rejectsAzureBlobUntilImplementedInSprint4() {
    IncidentEvidenceStorageProperties properties = new IncidentEvidenceStorageProperties();
    properties.setType("azure-blob");

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "El adaptador de almacenamiento azure-blob de incidencias está pendiente de implementar"
                + " en S4-14.");
  }
}
