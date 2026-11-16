package com.nexus.rinde.expense.application.internal.commandservices;

import com.nexus.rinde.expense.domain.model.aggregates.Expense;
import com.nexus.rinde.expense.domain.model.commands.AttachEvidenceCommand;
import com.nexus.rinde.expense.domain.model.commands.RegisterExpenseCommand;
import com.nexus.rinde.expense.domain.model.commands.UpdateExpenseStatusCommand;
import com.nexus.rinde.expense.domain.model.valueobjects.EvidenceFile;
import com.nexus.rinde.expense.domain.model.valueobjects.ExpenseStatus;
import com.nexus.rinde.expense.domain.model.valueobjects.RejectedExpense;
import com.nexus.rinde.expense.domain.model.valueobjects.SyncExpensesResult;
import com.nexus.rinde.expense.domain.model.valueobjects.SyncRejectionCode;
import com.nexus.rinde.expense.domain.services.EvidenceStorage;
import com.nexus.rinde.expense.domain.services.ExpenseCommandService;
import com.nexus.rinde.expense.domain.services.TripStatusService;
import com.nexus.rinde.expense.infrastructure.persistence.jpa.repositories.ExpenseRepository;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ForbiddenOperationException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Coordina la creación, auditoría y evidencia de gastos con su persistencia. */
@Service
public class ExpenseCommandServiceImpl implements ExpenseCommandService {

  private static final String DUPLICATE_KEY_MESSAGE = "La clave de idempotencia ya fue utilizada.";

  private final ExpenseRepository expenseRepository;
  private final TripStatusService tripStatusService;
  private final EvidenceStorage evidenceStorage;
  private final Clock clock;
  private final TransactionTemplate itemTransaction;
  private final long maxEvidenceSizeBytes;

  public ExpenseCommandServiceImpl(
      ExpenseRepository expenseRepository,
      TripStatusService tripStatusService,
      EvidenceStorage evidenceStorage,
      Clock clock,
      PlatformTransactionManager transactionManager,
      @Value("${rinde.evidence.max-size-bytes:5242880}") long maxEvidenceSizeBytes) {
    this.expenseRepository = expenseRepository;
    this.tripStatusService = tripStatusService;
    this.evidenceStorage = evidenceStorage;
    this.clock = clock;
    this.itemTransaction = new TransactionTemplate(transactionManager);
    this.itemTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.maxEvidenceSizeBytes = maxEvidenceSizeBytes;
  }

  @Override
  @Transactional
  public Expense handle(RegisterExpenseCommand command) {
    if (expenseRepository.existsByIdempotencyKey(normalize(command.idempotencyKey()))) {
      throw new ConflictException(DUPLICATE_KEY_MESSAGE);
    }

    requireTripAcceptingExpenses(command.tenantId(), command.tripId());

    Expense expense =
        Expense.create(
            command.tenantId(),
            command.tripId(),
            command.driverId(),
            command.category(),
            command.amount(),
            command.expenseDate(),
            command.idempotencyKey());

    return expenseRepository.saveAndFlush(expense);
  }

  @Override
  @Transactional
  public Expense handle(UpdateExpenseStatusCommand command) {
    Expense expense =
        expenseRepository
            .findByIdAndTenantId(command.expenseId(), command.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("El gasto no existe."));

    if (command.status() == ExpenseStatus.APPROVED) {
      expense.approve(command.reviewerId(), clock.instant());
    } else if (command.status() == ExpenseStatus.OBSERVED) {
      expense.observe(command.reviewerId(), command.reason(), clock.instant());
    }

    return expenseRepository.saveAndFlush(expense);
  }

  /**
   * Guarda el archivo del comprobante y lo asocia al gasto. Solo el conductor que registró el gasto
   * puede adjuntarlo. Al reemplazar, el archivo anterior se borra tras confirmar la transacción.
   */
  @Override
  @Transactional
  public Expense handle(AttachEvidenceCommand command) {
    Expense expense =
        expenseRepository
            .findByIdAndTenantId(command.expenseId(), command.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("El gasto no existe."));
    if (!expense.isByDriver(command.uploadedBy())) {
      throw new ForbiddenOperationException(
          "Solo el conductor que registró el gasto puede adjuntar su comprobante.");
    }
    expense.ensureNotLocked();

    EvidenceFile file =
        EvidenceFile.inspect(command.fileName(), command.content(), maxEvidenceSizeBytes);
    String storageKey = command.tenantId() + "/" + expense.getId() + "/" + UUID.randomUUID();
    evidenceStorage.save(storageKey, command.content());

    String previousKey =
        expense.attachEvidence(file, storageKey, command.uploadedBy(), clock.instant());
    Expense saved = expenseRepository.saveAndFlush(expense);
    deleteAfterCommit(previousKey);
    return saved;
  }

  /**
   * Procesa cada gasto del lote en su propia transacción, sin transacción externa. Un ítem
   * rechazado no impide guardar los demás.
   */
  @Override
  public SyncExpensesResult handleSync(List<RegisterExpenseCommand> commands) {
    List<Expense> synchronizedExpenses = new ArrayList<>();
    List<RejectedExpense> rejected = new ArrayList<>();
    for (RegisterExpenseCommand cmd : commands) {
      ItemOutcome outcome = processSyncItem(cmd);
      if (outcome.expense() != null) {
        synchronizedExpenses.add(outcome.expense());
      } else {
        rejected.add(outcome.rejection());
      }
    }
    return new SyncExpensesResult(synchronizedExpenses, rejected);
  }

  /** Una transacción propia por ítem. Si el ítem se rechaza, no se escribe nada. */
  private ItemOutcome processSyncItem(RegisterExpenseCommand cmd) {
    try {
      return itemTransaction.execute(status -> saveSyncItem(cmd));
    } catch (DataIntegrityViolationException ex) {
      // Otra empresa insertó la misma clave entre la comprobación y el guardado.
      if (expenseRepository.existsByIdempotencyKey(normalize(cmd.idempotencyKey()))) {
        return ItemOutcome.rejected(
            cmd.idempotencyKey(), SyncRejectionCode.DUPLICATE_KEY, DUPLICATE_KEY_MESSAGE);
      }
      throw ex;
    }
  }

  private ItemOutcome saveSyncItem(RegisterExpenseCommand cmd) {
    Optional<Expense> existing =
        expenseRepository.findByTenantIdAndIdempotencyKey(cmd.tenantId(), normalize(cmd.idempotencyKey()));
    if (existing.isPresent()) {
      return ItemOutcome.synchronizedWith(existing.get());
    }
    if (expenseRepository.existsByIdempotencyKey(normalize(cmd.idempotencyKey()))) {
      return ItemOutcome.rejected(
          cmd.idempotencyKey(), SyncRejectionCode.DUPLICATE_KEY, DUPLICATE_KEY_MESSAGE);
    }

    Optional<SyncRejectionCode> tripRejection = findTripRejection(cmd.tenantId(), cmd.tripId());
    if (tripRejection.isPresent()) {
      SyncRejectionCode code = tripRejection.get();
      return ItemOutcome.rejected(cmd.idempotencyKey(), code, tripRejectionMessage(code));
    }

    try {
      Expense expense =
          Expense.create(
              cmd.tenantId(),
              cmd.tripId(),
              cmd.driverId(),
              cmd.category(),
              cmd.amount(),
              cmd.expenseDate(),
              cmd.idempotencyKey());
      return ItemOutcome.synchronizedWith(expenseRepository.saveAndFlush(expense));
    } catch (BusinessRuleException | ConflictException ex) {
      return ItemOutcome.rejected(
          cmd.idempotencyKey(), SyncRejectionCode.INVALID_EXPENSE, ex.getMessage());
    }
  }

  /** Busca la clave igual que se guarda (sin espacios alrededor) para reconocer un reenvío. */
  private static String normalize(String idempotencyKey) {
    return idempotencyKey == null ? null : idempotencyKey.trim();
  }

  /** Permite gastos en viajes iniciados o finalizados (estos últimos llegan por sincronización). */
  private void requireTripAcceptingExpenses(UUID tenantId, UUID tripId) {
    Optional<SyncRejectionCode> rejection = findTripRejection(tenantId, tripId);
    if (rejection.isEmpty()) {
      return;
    }
    String message = tripRejectionMessage(rejection.get());
    if (rejection.get() == SyncRejectionCode.TRIP_NOT_FOUND) {
      throw new ResourceNotFoundException(message);
    }
    throw new ConflictException(message);
  }

  private Optional<SyncRejectionCode> findTripRejection(UUID tenantId, UUID tripId) {
    Optional<String> status = tripStatusService.findTripStatus(tenantId, tripId);
    if (status.isEmpty()) {
      return Optional.of(SyncRejectionCode.TRIP_NOT_FOUND);
    }
    if ("SETTLED".equals(status.get())) {
      return Optional.of(SyncRejectionCode.TRIP_SETTLED);
    }
    if (!"IN_ROUTE".equals(status.get()) && !"FINISHED".equals(status.get())) {
      return Optional.of(SyncRejectionCode.TRIP_NOT_STARTED);
    }
    return Optional.empty();
  }

  private static String tripRejectionMessage(SyncRejectionCode code) {
    return switch (code) {
      case TRIP_NOT_FOUND -> "El viaje no existe.";
      case TRIP_SETTLED -> "El viaje ya fue liquidado.";
      default -> "El viaje todavía no ha iniciado.";
    };
  }

  /** Borra el archivo reemplazado solo después de confirmar el cambio en la base. */
  private void deleteAfterCommit(String previousKey) {
    if (previousKey == null) {
      return;
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      evidenceStorage.delete(previousKey);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            evidenceStorage.delete(previousKey);
          }
        });
  }

  /** Resultado de un ítem: el gasto guardado o ya existente, o bien el rechazo. */
  private record ItemOutcome(Expense expense, RejectedExpense rejection) {

    static ItemOutcome synchronizedWith(Expense expense) {
      return new ItemOutcome(expense, null);
    }

    static ItemOutcome rejected(String idempotencyKey, SyncRejectionCode code, String reason) {
      return new ItemOutcome(null, new RejectedExpense(idempotencyKey, code, reason));
    }
  }
}
