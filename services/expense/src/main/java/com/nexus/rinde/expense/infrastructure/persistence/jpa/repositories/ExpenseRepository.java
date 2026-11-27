package com.nexus.rinde.expense.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repositorio de gastos operativos con aislamiento por empresa. */
@Repository
public interface ExpenseRepository
    extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

  @Override
  @EntityGraph(attributePaths = "evidence")
  Page<Expense> findAll(Specification<Expense> spec, Pageable pageable);

  @EntityGraph(attributePaths = "evidence")
  Optional<Expense> findByIdAndTenantId(UUID id, UUID tenantId);

  @EntityGraph(attributePaths = "evidence")
  List<Expense> findByTenantIdAndTripId(UUID tenantId, UUID tripId);

  List<Expense> findByTenantIdAndTripIdAndStatus(UUID tenantId, UUID tripId, ExpenseStatus status);

  List<Expense> findByTenantId(UUID tenantId);

  boolean existsByIdempotencyKey(String idempotencyKey);

  Optional<Expense> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
}
