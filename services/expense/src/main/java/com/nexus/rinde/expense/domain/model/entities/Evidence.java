package com.nexus.rinde.expense.domain.model.entities;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.valueobjects.EvidenceFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Comprobante de un gasto con su archivo guardado. Los registros anteriores a V3 no tienen
 * storageKey y no tienen archivo.
 */
@Entity
@Table(schema = "expense", name = "evidences")
public class Evidence {

  @Id private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "expense_id", nullable = false, updatable = false, unique = true)
  private Expense expense;

  @Column(name = "file_name", length = 255)
  private String fileName;

  @Column(name = "content_type", length = 100)
  private String contentType;

  @Column(name = "file_size_bytes", nullable = false)
  private Long fileSizeBytes;

  @Column(name = "sha256", length = 64)
  private String sha256;

  @Column(name = "storage_key", length = 255)
  private String storageKey;

  @Column(name = "uploaded_by")
  private UUID uploadedBy;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt;

  protected Evidence() {}

  public Evidence(
      Expense expense,
      UUID tenantId,
      EvidenceFile file,
      String storageKey,
      UUID uploadedBy,
      Instant uploadedAt) {
    this.id = UUID.randomUUID();
    this.expense = expense;
    this.tenantId = tenantId;
    apply(file, storageKey, uploadedBy, uploadedAt);
  }

  /** Reemplaza el archivo y conserva el identificador del comprobante. */
  public void replaceFile(
      EvidenceFile file, String storageKey, UUID uploadedBy, Instant uploadedAt) {
    apply(file, storageKey, uploadedBy, uploadedAt);
  }

  private void apply(EvidenceFile file, String storageKey, UUID uploadedBy, Instant uploadedAt) {
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

  public void setExpense(Expense expense) {
    this.expense = expense;
  }
}
