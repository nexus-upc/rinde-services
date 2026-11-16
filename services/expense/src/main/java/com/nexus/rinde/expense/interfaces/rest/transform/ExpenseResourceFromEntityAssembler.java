package com.nexus.rinde.expense.interfaces.rest.transform;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.entities.Evidence;
import com.nexus.rinde.expense.interfaces.rest.resources.EvidenceResource;
import com.nexus.rinde.expense.interfaces.rest.resources.ExpenseResource;

/** Transforma la entidad Expense a su recurso REST público. */
public final class ExpenseResourceFromEntityAssembler {

  private ExpenseResourceFromEntityAssembler() {}

  public static ExpenseResource toResource(Expense expense) {
    EvidenceResource evidenceResource = null;
    if (expense.getEvidence() != null) {
      evidenceResource = toEvidenceResource(expense, expense.getEvidence());
    }

    return new ExpenseResource(
        expense.getId(),
        expense.getTenantId(),
        expense.getTripId(),
        expense.getDriverId(),
        expense.getCategory(),
        expense.getAmount().getAmount(),
        expense.getAmount().getCurrency(),
        expense.getExpenseDate(),
        expense.getStatus(),
        expense.getIdempotencyKey(),
        expense.getObservationReason(),
        expense.isLocked(),
        evidenceResource,
        expense.getCreatedAt(),
        expense.getUpdatedAt());
  }

  /** La ruta de contenido es relativa y exige JWT; no es una URL pública. */
  private static EvidenceResource toEvidenceResource(Expense expense, Evidence evidence) {
    String contentUrl =
        evidence.getStorageKey() != null
            ? "/api/v1/expenses/%s/evidences/%s/content".formatted(expense.getId(), evidence.getId())
            : null;
    return new EvidenceResource(
        evidence.getId(),
        evidence.getFileName(),
        evidence.getContentType(),
        evidence.getFileSizeBytes(),
        evidence.getSha256(),
        contentUrl,
        evidence.getUploadedAt());
  }
}
