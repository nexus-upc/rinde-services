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
  void rejectsAzureBlobUntilImplementedInSprint4() {
    EvidenceStorageProperties properties = new EvidenceStorageProperties();
    properties.setType("azure-blob");

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("El adaptador de almacenamiento azure-blob está pendiente de implementar en S4-14.");
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
