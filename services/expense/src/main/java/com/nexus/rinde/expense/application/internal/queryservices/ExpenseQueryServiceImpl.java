package com.nexus.rinde.expense.application.internal.queryservices;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.entities.Evidence;
import com.nexus.rinde.expense.domain.model.queries.GetEvidenceContentQuery;
import com.nexus.rinde.expense.domain.model.queries.GetExpenseByIdQuery;
import com.nexus.rinde.expense.domain.model.queries.GetExpensesByTripQuery;
import com.nexus.rinde.expense.domain.model.queries.ListExpensesQuery;
import com.nexus.rinde.expense.domain.model.valueobjects.EvidenceContent;
import com.nexus.rinde.expense.domain.model.valueobjects.EvidenceFile;
import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseStatus;
import com.nexus.rinde.expense.domain.model.valueobjects.PagedExpenses;
import com.nexus.rinde.expense.domain.services.EvidenceStorage;
import com.nexus.rinde.expense.domain.services.ExpenseQueryService;
import com.nexus.rinde.expense.infrastructure.persistence.jpa.repositories.ExpenseRepository;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ForbiddenOperationException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

  /** Lista los gastos de la empresa con filtros opcionales y paginación descendente (C2). */
  @Override
  @Transactional(readOnly = true)
  public PagedExpenses handle(ListExpensesQuery query) {
    if (query.page() < 0) {
      throw new BusinessRuleException("El número de página no puede ser negativo.");
    }
    if (query.size() < 1 || query.size() > 100) {
      throw new BusinessRuleException("El tamaño de página debe estar entre 1 y 100.");
    }

    LocalDate fromDate = parseDate(query.from(), "La fecha de inicio");
    LocalDate toDate = parseDate(query.to(), "La fecha de fin");
    if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
      throw new BusinessRuleException("La fecha final no puede ser anterior a la inicial.");
    }
    Set<ExpenseStatus> statuses = parseStatuses(query.status());

    Specification<Expense> spec = buildSpecification(query, statuses, fromDate, toDate);
    Sort sort = Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("createdAt"));
    Page<Expense> result =
        expenseRepository.findAll(spec, PageRequest.of(query.page(), query.size(), sort));

    return new PagedExpenses(
        result.getContent(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
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

  /** Construye los predicados JPA garantizando el aislamiento por empresa. */
  private static Specification<Expense> buildSpecification(
      ListExpensesQuery query,
      Set<ExpenseStatus> statuses,
      LocalDate fromDate,
      LocalDate toDate) {
    return (root, criteriaQuery, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("tenantId"), query.tenantId()));
      if (!statuses.isEmpty()) {
        predicates.add(root.get("status").in(statuses));
      }
      if (query.tripId() != null) {
        predicates.add(cb.equal(root.get("tripId"), query.tripId()));
      }
      if (query.driverId() != null) {
        predicates.add(cb.equal(root.get("driverId"), query.driverId()));
      }
      if (fromDate != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), fromDate));
      }
      if (toDate != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), toDate));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  /** Valida y convierte una fecha opcional en formato estricto AAAA-MM-DD. */
  private static LocalDate parseDate(String value, String label) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() != 10) {
      throw new BusinessRuleException(label + " debe tener el formato AAAA-MM-DD.");
    }
    try {
      return LocalDate.parse(trimmed);
    } catch (DateTimeParseException ex) {
      throw new BusinessRuleException(label + " debe tener el formato AAAA-MM-DD.");
    }
  }

  /** Convierte la lista CSV de estados de gasto y rechaza valores desconocidos. */
  private static Set<ExpenseStatus> parseStatuses(String raw) {
    if (raw == null || raw.isBlank()) {
      return Set.of();
    }
    Set<ExpenseStatus> statuses = EnumSet.noneOf(ExpenseStatus.class);
    for (String token : raw.split(",", -1)) {
      String trimmed = token.trim();
      if (trimmed.isEmpty()) {
        throw new BusinessRuleException("El parámetro status contiene un estado de gasto inválido.");
      }
      try {
        statuses.add(ExpenseStatus.valueOf(trimmed));
      } catch (IllegalArgumentException ex) {
        throw new BusinessRuleException("El parámetro status contiene un estado de gasto inválido.");
      }
    }
    return statuses;
  }
}
