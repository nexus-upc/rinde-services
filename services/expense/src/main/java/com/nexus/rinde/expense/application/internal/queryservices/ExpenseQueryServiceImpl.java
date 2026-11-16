package com.nexus.rinde.expense.application.internal.queryservices;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.entities.Evidence;
import com.nexus.rinde.expense.domain.model.queries.GetEvidenceContentQuery;
import com.nexus.rinde.expense.domain.model.queries.GetExpenseByIdQuery;
import com.nexus.rinde.expense.domain.model.queries.GetExpensesByTripQuery;
import com.nexus.rinde.expense.domain.model.valueobjects.EvidenceContent;
import com.nexus.rinde.expense.domain.model.valueobjects.EvidenceFile;
import com.nexus.rinde.expense.domain.services.EvidenceStorage;
import com.nexus.rinde.expense.domain.services.ExpenseQueryService;
import com.nexus.rinde.expense.infrastructure.persistence.jpa.repositories.ExpenseRepository;
import com.nexus.rinde.shared.domain.exceptions.ForbiddenOperationException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atiende consultas sobre los gastos operativos registrados y sus comprobantes. */
@Service
public class ExpenseQueryServiceImpl implements ExpenseQueryService {

  /** Roles de la empresa que pueden leer cualquier comprobante de sus gastos. */
  private static final Set<String> COMPANY_READERS = Set.of("ADMINISTRATOR", "OPERATIONS_MANAGER");

  private final ExpenseRepository expenseRepository;
  private final EvidenceStorage evidenceStorage;

  public ExpenseQueryServiceImpl(
      ExpenseRepository expenseRepository, EvidenceStorage evidenceStorage) {
    this.expenseRepository = expenseRepository;
    this.evidenceStorage = evidenceStorage;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Expense> handle(GetExpensesByTripQuery query) {
    return expenseRepository.findByTenantIdAndTripId(query.tenantId(), query.tripId());
  }

  @Override
  @Transactional(readOnly = true)
  public Expense handle(GetExpenseByIdQuery query) {
    return expenseRepository
        .findByIdAndTenantId(query.expenseId(), query.tenantId())
        .orElseThrow(() -> new ResourceNotFoundException("El gasto no existe."));
  }

  /**
   * Devuelve el archivo del comprobante a la empresa o al conductor que registró el gasto. Verifica
   * la huella guardada antes de responder.
   */
  @Override
  @Transactional(readOnly = true)
  public EvidenceContent handle(GetEvidenceContentQuery query) {
    Expense expense =
        expenseRepository
            .findByIdAndTenantId(query.expenseId(), query.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("El gasto no existe."));
    Evidence evidence = expense.getEvidence();
    if (evidence == null || !evidence.getId().equals(query.evidenceId())) {
      throw new ResourceNotFoundException("El comprobante no existe.");
    }

    boolean companyReader = COMPANY_READERS.contains(query.role());
    boolean ownerDriver = "DRIVER".equals(query.role()) && expense.isByDriver(query.userId());
    if (!companyReader && !ownerDriver) {
      throw new ForbiddenOperationException("No tiene permiso para ver este comprobante.");
    }
    if (evidence.getStorageKey() == null) {
      throw new ResourceNotFoundException("El comprobante no tiene archivo almacenado.");
    }

    byte[] content = evidenceStorage.load(evidence.getStorageKey());
    if (!EvidenceFile.sha256Hex(content).equals(evidence.getSha256())) {
      throw new IllegalStateException("El archivo del comprobante no coincide con su huella.");
    }
    return new EvidenceContent(evidence.getFileName(), evidence.getContentType(), content);
  }
}
