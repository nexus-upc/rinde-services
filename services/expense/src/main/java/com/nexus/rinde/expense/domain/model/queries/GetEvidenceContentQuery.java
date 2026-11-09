package com.nexus.rinde.expense.domain.model.queries;

import java.util.UUID;

/** Consulta del archivo de un comprobante, con el actor que la pide para autorizarla. */
public record GetEvidenceContentQuery(
    UUID tenantId, UUID expenseId, UUID evidenceId, UUID userId, String role) {}
