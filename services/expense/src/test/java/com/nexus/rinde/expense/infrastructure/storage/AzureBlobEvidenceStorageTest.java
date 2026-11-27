package com.nexus.rinde.expense.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

/** Pruebas unitarias con dobles para AzureBlobEvidenceStorage sin tocar cuentas reales (C4). */
class AzureBlobEvidenceStorageTest {

  private BlobContainerClient containerClient;
  private BlobClient blobClient;
  private AzureBlobEvidenceStorage storage;

  @BeforeEach
  void setUp() {
    containerClient = mock(BlobContainerClient.class);
    blobClient = mock(BlobClient.class);
    when(containerClient.getBlobClient(any())).thenReturn(blobClient);
    storage = new AzureBlobEvidenceStorage(containerClient);
  }

  @Test
  void savesBytesInPrivateBlobWithoutOverwrite() {
    byte[] payload = "comprobante-prueba".getBytes(StandardCharsets.UTF_8);

    storage.save("tenant-1/exp-1/file-1", payload);

    ArgumentCaptor<BinaryData> dataCaptor = ArgumentCaptor.forClass(BinaryData.class);
    verify(containerClient).getBlobClient("tenant-1/exp-1/file-1");
    verify(blobClient).upload(dataCaptor.capture(), eq(false));
    assertThat(dataCaptor.getValue().toBytes()).isEqualTo(payload);
  }

  @Test
  void loadsStoredBytesAndTranslatesMissingBlobTo404DomainException() {
    byte[] expected = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01};
    when(blobClient.downloadContent()).thenReturn(BinaryData.fromBytes(expected));

    assertThat(storage.load("tenant-1/exp-1/file-1")).isEqualTo(expected);

    HttpResponse response404 = mock(HttpResponse.class);
    when(response404.getStatusCode()).thenReturn(404);
    when(blobClient.downloadContent())
        .thenThrow(new BlobStorageException("BlobNotFound", response404, null));

    assertThatThrownBy(() -> storage.load("tenant-1/exp-1/missing"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("El archivo del comprobante no está disponible en el almacenamiento.");
  }

  @Test
  void deletesQuietlyAndRejectsInvalidTraversalKeys() {
    doThrow(new RuntimeException("transient network error")).when(blobClient).deleteIfExists();

    assertThatCode(() -> storage.delete("tenant-1/exp-1/old-file")).doesNotThrowAnyException();
    assertThatThrownBy(() -> storage.save("../escape", new byte[] {1}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> storage.load("/leading-slash"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
