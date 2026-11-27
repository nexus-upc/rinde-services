package com.nexus.rinde.expense.infrastructure.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.nexus.rinde.expense.domain.services.EvidenceStorage;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adaptador de comprobantes sobre un contenedor privado de Azure Blob Storage (C4). */
@Component
@ConditionalOnProperty(name = "rinde.evidence.storage.type", havingValue = "azure-blob")
public class AzureBlobEvidenceStorage implements EvidenceStorage {

  private static final Logger log = LoggerFactory.getLogger(AzureBlobEvidenceStorage.class);
  private static final String MISSING_FILE_MESSAGE =
      "El archivo del comprobante no está disponible en el almacenamiento.";

  private final BlobContainerClient containerClient;

  @Autowired
  public AzureBlobEvidenceStorage(EvidenceStorageProperties properties) {
    this(
        new BlobContainerClientBuilder()
            .connectionString(properties.getBlob().getConnectionString().trim())
            .containerName(properties.getBlob().getContainer().trim())
            .buildClient());
  }

  AzureBlobEvidenceStorage(BlobContainerClient containerClient) {
    this.containerClient = containerClient;
  }

  @Override
  public void save(String storageKey, byte[] content) {
    String blobName = normalizeKey(storageKey);
    try {
      containerClient.getBlobClient(blobName).upload(BinaryData.fromBytes(content), false);
    } catch (BlobStorageException ex) {
      throw new UncheckedIOException(new IOException("No se pudo guardar el comprobante.", ex));
    }
  }

  @Override
  public byte[] load(String storageKey) {
    String blobName = normalizeKey(storageKey);
    try {
      return containerClient.getBlobClient(blobName).downloadContent().toBytes();
    } catch (BlobStorageException ex) {
      if (ex.getStatusCode() == 404 || BlobErrorCode.BLOB_NOT_FOUND.equals(ex.getErrorCode())) {
        throw new ResourceNotFoundException(MISSING_FILE_MESSAGE);
      }
      throw new UncheckedIOException(new IOException("No se pudo leer el comprobante.", ex));
    }
  }

  @Override
  public void delete(String storageKey) {
    try {
      containerClient.getBlobClient(normalizeKey(storageKey)).deleteIfExists();
    } catch (RuntimeException ex) {
      log.warn("No se pudo borrar un comprobante reemplazado en Blob Storage: {}", ex.getMessage());
    }
  }

  /** Valida que la clave de objeto sea relativa y no contenga segmentos de recorrido. */
  private static String normalizeKey(String storageKey) {
    if (storageKey == null || storageKey.isBlank()) {
      throw new IllegalArgumentException("Clave de almacenamiento no válida.");
    }
    String normalized = storageKey.replace('\\', '/').trim();
    if (normalized.startsWith("/") || normalized.contains("..")) {
      throw new IllegalArgumentException("Clave de almacenamiento no válida.");
    }
    return normalized;
  }
}
