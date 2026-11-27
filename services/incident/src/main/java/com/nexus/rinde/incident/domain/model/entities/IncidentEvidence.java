package com.nexus.rinde.incident.domain.model.entities;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentEvidenceFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Metadatos persistidos del archivo de evidencia asociado a una incidencia (V2). */
@Entity
@Table(schema = "incident", name = "incident_evidences")
public class IncidentEvidence {

  @Id private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "incident_id", nullable = false, updatable = false, unique = true)
  private Incident incident;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "file_size_bytes", nullable = false)
  private Long fileSizeBytes;

  @Column(name = "sha256", nullable = false, length = 64)
  private String sha256;

  @Column(name = "storage_key", nullable = false, length = 255)
  private String storageKey;

  @Column(name = "uploaded_by", nullable = false)
  private UUID uploadedBy;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt;

  protected IncidentEvidence() {}

  public IncidentEvidence(
      Incident incident,
      UUID tenantId,
      IncidentEvidenceFile file,
      String storageKey,
      UUID uploadedBy,
      Instant uploadedAt) {
    this.id = UUID.randomUUID();
    this.incident = incident;
    this.tenantId = tenantId;
    apply(file, storageKey, uploadedBy, uploadedAt);
  }

  /** Reemplaza los metadatos del archivo conservando el identificador de la evidencia. */
  public void replaceFile(
      IncidentEvidenceFile file, String storageKey, UUID uploadedBy, Instant uploadedAt) {
    apply(file, storageKey, uploadedBy, uploadedAt);
  }

  private void apply(
      IncidentEvidenceFile file, String storageKey, UUID uploadedBy, Instant uploadedAt) {
    this.fileName = file.fileName();
    this.contentType = file.format().contentType();
    this.fileSizeBytes = file.sizeBytes();
    this.sha256 = file.sha256();
    this.storageKey = storageKey;
    this.uploadedBy = uploadedBy;
    this.uploadedAt = uploadedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public Long getFileSizeBytes() {
    return fileSizeBytes;
  }

  public String getSha256() {
    return sha256;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public UUID getUploadedBy() {
    return uploadedBy;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }
}
