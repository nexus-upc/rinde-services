package com.nexus.rinde.incident.infrastructure.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.nexus.rinde.incident.domain.services.IncidentEvidenceStorage;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adaptador de evidencias de incidencias sobre un contenedor privado de Azure Blob Storage (C4). */
@Component
@ConditionalOnProperty(name = "rinde.evidence.storage.type", havingValue = "azure-blob")
public class AzureBlobIncidentEvidenceStorage implements IncidentEvidenceStorage {

  private static final Logger log =
      LoggerFactory.getLogger(AzureBlobIncidentEvidenceStorage.class);
  private static final String MISSING_FILE_MESSAGE =
      "El archivo del comprobante no está disponible en el almacenamiento.";

  private final BlobContainerClient containerClient;

  @Autowired
  public AzureBlobIncidentEvidenceStorage(IncidentEvidenceStorageProperties properties) {
    this(
        new BlobContainerClientBuilder()
            .connectionString(properties.getBlob().getConnectionString().trim())
            .containerName(properties.getBlob().getContainer().trim())
            .buildClient());
  }

  AzureBlobIncidentEvidenceStorage(BlobContainerClient containerClient) {
    this.containerClient = containerClient;
  }

  @Override
  public void save(String storageKey, byte[] content) {
    String blobName = normalizeKey(storageKey);
    try {
      containerClient.getBlobClient(blobName).upload(BinaryData.fromBytes(content), false);
    } catch (BlobStorageException ex) {
      throw new UncheckedIOException(new IOException("No se pudo guardar la evidencia.", ex));
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
      throw new UncheckedIOException(new IOException("No se pudo leer la evidencia.", ex));
    }
  }

  @Override
  public void delete(String storageKey) {
    try {
      containerClient.getBlobClient(normalizeKey(storageKey)).deleteIfExists();
    } catch (RuntimeException ex) {
      log.warn("No se pudo eliminar una evidencia reemplazada en Blob Storage: {}", ex.getMessage());
    }
  }

  /** Verifica que la clave del objeto sea relativa y no contenga segmentos de recorrido. */
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
