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
  void acceptsAzureBlobWithConnectionStringAndContainer() {
    IncidentEvidenceStorageProperties properties = new IncidentEvidenceStorageProperties();
    properties.setType("azure-blob");
    properties.getBlob().setConnectionString("UseDevelopmentStorage=true");
    properties.getBlob().setContainer("rinde-incidents-private");

    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  void rejectsAzureBlobWithoutConnectionStringOrContainer() {
    IncidentEvidenceStorageProperties properties = new IncidentEvidenceStorageProperties();
    properties.setType("azure-blob");

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("RINDE_EVIDENCE_BLOB_CONNECTION_STRING");
  }

  @Test
  void rejectsUnknownStorageType() {
    IncidentEvidenceStorageProperties properties = new IncidentEvidenceStorageProperties();
    properties.setType("s3");

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no válido");
  }
}
