package com.nexus.rinde.incident.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.http.HttpResponse;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Pruebas unitarias con dobles para AzureBlobIncidentEvidenceStorage sin cuentas reales (C4). */
class AzureBlobIncidentEvidenceStorageTest {

  private BlobContainerClient containerClient;
  private BlobClient blobClient;
  private AzureBlobIncidentEvidenceStorage storage;

  @BeforeEach
  void setUp() {
    containerClient = mock(BlobContainerClient.class);
    blobClient = mock(BlobClient.class);
    when(containerClient.getBlobClient(any())).thenReturn(blobClient);
    storage = new AzureBlobIncidentEvidenceStorage(containerClient);
  }

  @Test
  void savesBytesWithoutOverwriteAndLoadsContent() {
    byte[] payload = "foto-incidencia".getBytes(StandardCharsets.UTF_8);
    when(blobClient.downloadContent()).thenReturn(BinaryData.fromBytes(payload));

    storage.save("tenant-1/inc-1/file-1", payload);
    byte[] loaded = storage.load("tenant-1/inc-1/file-1");

    ArgumentCaptor<BinaryData> dataCaptor = ArgumentCaptor.forClass(BinaryData.class);
    verify(containerClient, times(2)).getBlobClient("tenant-1/inc-1/file-1");
    verify(blobClient).upload(dataCaptor.capture(), eq(false));
    assertThat(dataCaptor.getValue().toBytes()).isEqualTo(payload);
    assertThat(loaded).isEqualTo(payload);
  }

  @Test
  void translatesBlobNotFoundToResourceNotFoundAndValidatesKeys() {
    HttpResponse response404 = mock(HttpResponse.class);
    when(response404.getStatusCode()).thenReturn(404);
    when(blobClient.downloadContent())
        .thenThrow(new BlobStorageException("BlobNotFound", response404, null));

    assertThatThrownBy(() -> storage.load("tenant-1/inc-1/missing"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("El archivo del comprobante no está disponible en el almacenamiento.");

    doThrow(new RuntimeException("transient error")).when(blobClient).deleteIfExists();
    assertThatCode(() -> storage.delete("tenant-1/inc-1/old")).doesNotThrowAnyException();

    assertThatThrownBy(() -> storage.save("../traversal", new byte[] {1}))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
