package com.ledgercore.service;

import com.ledgercore.dto.request.TransferRequest;
import com.ledgercore.dto.response.BudgetAlertResponse;
import com.ledgercore.dto.response.TransactionResponse;
import com.ledgercore.entity.Account;
import com.ledgercore.entity.Transaction;
import com.ledgercore.enums.TransactionType;
import com.ledgercore.exception.AccountNotFoundException;
import com.ledgercore.exception.InsufficientBalanceException;
import com.ledgercore.repository.AccountRepository;
import com.ledgercore.repository.TransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for inter-account transfers.
 * <p>
 * CONCURRENCY SAFETY:
 * - Uses @Transactional with SERIALIZABLE isolation level
 * - Acquires row-level locks via SELECT FOR UPDATE (findByIdForUpdate)
 * - Always locks accounts in ascending UUID order to prevent deadlocks
 * <p>
 * DEADLOCK PREVENTION:
 * When two concurrent transfers attempt to lock accounts A→B and B→A simultaneously,
 * a deadlock can occur. We prevent this by always acquiring locks in ascending UUID
 * order regardless of which account is source vs. destination. This ensures a
 * consistent lock ordering and eliminates circular-wait conditions.
 */
@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;
    private final BudgetService budgetService;
    private final TransactionService transactionService;

    public TransferService(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           AuditLogService auditLogService,
                           BudgetService budgetService,
                           TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogService = auditLogService;
        this.budgetService = budgetService;
        this.transactionService = transactionService;
    }

    /**
     * Transfer funds between two accounts owned by the same user.
     * <p>
     * Locks both accounts using SELECT FOR UPDATE in ascending UUID order
     * to prevent deadlocks. Validates ownership, sufficient balance,
     * and writes audit log entries for both the debit and credit sides.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse transfer(TransferRequest request, UUID userId) {
        UUID sourceId = request.sourceAccountId();
        UUID destId = request.destinationAccountId();

        // Prevent self-transfer
        if (sourceId.equals(destId)) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different");
        }

        // DEADLOCK PREVENTION: Always lock in ascending UUID order
        UUID firstLock = sourceId.compareTo(destId) < 0 ? sourceId : destId;
        UUID secondLock = sourceId.compareTo(destId) < 0 ? destId : sourceId;

        // Acquire locks in consistent order via SELECT FOR UPDATE
        Account firstAccount = accountRepository.findByIdForUpdate(firstLock)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + firstLock));
        Account secondAccount = accountRepository.findByIdForUpdate(secondLock)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + secondLock));

        // Map back to source and destination
        Account source = firstLock.equals(sourceId) ? firstAccount : secondAccount;
        Account destination = firstLock.equals(destId) ? firstAccount : secondAccount;

        // Verify both accounts belong to the authenticated user
        if (!source.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "You do not own the source account: " + sourceId);
        }
        if (!destination.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "You do not own the destination account: " + destId);
        }

        // Check sufficient balance on source
        BigDecimal sourceBalanceBefore = source.getBalance();
        if (sourceBalanceBefore.compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in source account. Available: " +
                    sourceBalanceBefore + ", requested: " + request.amount());
        }

        // Execute transfer
        BigDecimal sourceBalanceAfter = sourceBalanceBefore.subtract(request.amount());
        BigDecimal destBalanceBefore = destination.getBalance();
        BigDecimal destBalanceAfter = destBalanceBefore.add(request.amount());

        source.setBalance(sourceBalanceAfter);
        destination.setBalance(destBalanceAfter);

        accountRepository.save(source);
        accountRepository.save(destination);

        // Generate a shared reference ID to link the two sides of the transfer
        UUID referenceId = UUID.randomUUID();

        // Write audit log entries for both sides
        auditLogService.logTransaction(
                sourceId, TransactionType.TRANSFER_OUT, request.amount(),
                sourceBalanceBefore, sourceBalanceAfter,
                "Transfer to " + destId + ": " + request.description()
        );
        auditLogService.logTransaction(
                destId, TransactionType.TRANSFER_IN, request.amount(),
                destBalanceBefore, destBalanceAfter,
                "Transfer from " + sourceId + ": " + request.description()
        );

        // Write transaction records for both sides
        Transaction sourceTransaction = new Transaction(
                source, TransactionType.TRANSFER_OUT, request.category(),
                request.amount(), request.description(), referenceId
        );
        Transaction destTransaction = new Transaction(
                destination, TransactionType.TRANSFER_IN, request.category(),
                request.amount(), request.description(), referenceId
        );
        transactionRepository.save(sourceTransaction);
        transactionRepository.save(destTransaction);

        // Evict monthly summary cache for both accounts
        transactionService.evictSummaryCache(sourceId);
        transactionService.evictSummaryCache(destId);

        // Check budget alerts for the source (spending side)
        List<BudgetAlertResponse> alerts = new ArrayList<>(
                budgetService.checkBudgetAlerts(sourceId, request.category())
        );

        return new TransactionResponse(
                sourceTransaction.getId(), sourceTransaction.getType(),
                sourceTransaction.getCategory(), sourceTransaction.getAmount(),
                sourceTransaction.getDescription(), sourceTransaction.getCreatedAt(),
                alerts
        );
    }
}
