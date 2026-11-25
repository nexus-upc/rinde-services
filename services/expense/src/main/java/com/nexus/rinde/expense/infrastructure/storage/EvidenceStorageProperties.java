package com.nexus.rinde.expense.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración del adaptador de almacenamiento para comprobantes de gastos. */
@ConfigurationProperties(prefix = "rinde.evidence.storage")
public class EvidenceStorageProperties {

  private String type = "local";
  private String localPath = "./data/evidences";
  private BlobProperties blob = new BlobProperties();

  @PostConstruct
  void validate() {
    String normalized = type == null ? "local" : type.trim();
    if ("azure-blob".equals(normalized)) {
      throw new IllegalStateException(
          "El adaptador de almacenamiento azure-blob está pendiente de implementar en S4-14.");
    }
    if (!"local".equals(normalized)) {
      throw new IllegalStateException(
          "Tipo de almacenamiento de comprobantes no válido: " + normalized);
    }
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLocalPath() {
    return localPath;
  }

  public void setLocalPath(String localPath) {
    this.localPath = localPath;
  }

  public BlobProperties getBlob() {
    return blob;
  }

  public void setBlob(BlobProperties blob) {
    this.blob = blob;
  }

  /** Propiedades de conexión para Azure Blob Storage. */
  public static class BlobProperties {

    private String connectionString = "";
    private String container = "";

    public String getConnectionString() {
      return connectionString;
    }

    public void setConnectionString(String connectionString) {
      this.connectionString = connectionString;
    }

    public String getContainer() {
      return container;
    }

    public void setContainer(String container) {
      this.container = container;
    }
  }
}
