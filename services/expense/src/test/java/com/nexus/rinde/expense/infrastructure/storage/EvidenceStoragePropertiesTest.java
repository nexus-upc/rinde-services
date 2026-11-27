package com.nexus.rinde.expense.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EvidenceStoragePropertiesTest {

  @Test
  void acceptsLocalStorageByDefault() {
    EvidenceStorageProperties properties = new EvidenceStorageProperties();
    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  void acceptsAzureBlobWithConnectionStringAndContainer() {
    EvidenceStorageProperties properties = new EvidenceStorageProperties();
    properties.setType("azure-blob");
    properties.getBlob().setConnectionString("UseDevelopmentStorage=true");
    properties.getBlob().setContainer("rinde-evidences-private");

    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  void rejectsAzureBlobWithoutConnectionStringOrContainer() {
    EvidenceStorageProperties properties = new EvidenceStorageProperties();
    properties.setType("azure-blob");

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("RINDE_EVIDENCE_BLOB_CONNECTION_STRING");
  }

  @Test
  void rejectsUnknownStorageType() {
    EvidenceStorageProperties properties = new EvidenceStorageProperties();
    properties.setType("s3");

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no válido");
  }
}
