package com.nexus.rinde.expense.domain.model.commands;

import java.util.UUID;

/** Comando para adjuntar o reemplazar el comprobante de un gasto con un archivo subido. */
public record AttachEvidenceCommand(
    UUID tenantId, UUID expenseId, UUID uploadedBy, String fileName, byte[] content) {}
