package com.ledgercore.service;

import com.ledgercore.dto.request.CreateAccountRequest;
import com.ledgercore.dto.request.DepositRequest;
import com.ledgercore.dto.request.WithdrawRequest;
import com.ledgercore.dto.response.AccountResponse;
import com.ledgercore.dto.response.BudgetAlertResponse;
import com.ledgercore.dto.response.TransactionResponse;
import com.ledgercore.entity.Account;
import com.ledgercore.entity.Transaction;
import com.ledgercore.entity.User;
import com.ledgercore.enums.TransactionType;
import com.ledgercore.exception.AccountNotFoundException;
import com.ledgercore.exception.InsufficientBalanceException;
import com.ledgercore.repository.AccountRepository;
import com.ledgercore.repository.TransactionRepository;
import com.ledgercore.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service for account management and financial operations (deposit, withdraw).
 * <p>
 * All balance-modifying operations use:
 * - @Transactional with SERIALIZABLE isolation level
 * - SELECT FOR UPDATE row-level locking via findByIdForUpdate()
 * <p>
 * This combination ensures complete protection against race conditions,
 * including phantom reads and concurrent balance modifications.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;
    private final BudgetService budgetService;
    private final TransactionService transactionService;

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository,
                          TransactionRepository transactionRepository,
                          AuditLogService auditLogService,
                          BudgetService budgetService,
                          TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogService = auditLogService;
        this.budgetService = budgetService;
        this.transactionService = transactionService;
    }

    /**
     * Create a new account for the authenticated user with zero balance.
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + userId));

        Account account = new Account(user, request.accountType());
        accountRepository.save(account);

        return toResponse(account);
    }

    /**
     * Deposit funds into an account.
     * <p>
     * Uses SERIALIZABLE isolation and SELECT FOR UPDATE to prevent
     * concurrent modifications to the account balance.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse deposit(UUID accountId, DepositRequest request, UUID userId) {
        // Lock the account row with SELECT FOR UPDATE
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountId));

        // Verify ownership
        if (!account.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this account");
        }

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.amount());

        // Update balance
        account.setBalance(balanceAfter);
        accountRepository.save(account);

        // Write immutable audit log entry
        auditLogService.logTransaction(
                accountId, TransactionType.DEPOSIT, request.amount(),
                balanceBefore, balanceAfter, request.description()
        );

        // Write transaction record
        Transaction transaction = new Transaction(
                account, TransactionType.DEPOSIT, request.category(),
                request.amount(), request.description(), null
        );
        transactionRepository.save(transaction);

        // Evict monthly summary cache
        transactionService.evictSummaryCache(accountId);

        // Check budget alerts
        List<BudgetAlertResponse> alerts = budgetService
                .checkBudgetAlerts(accountId, request.category());

        return new TransactionResponse(
                transaction.getId(), transaction.getType(), transaction.getCategory(),
                transaction.getAmount(), transaction.getDescription(),
                transaction.getCreatedAt(), alerts
        );
    }

    /**
     * Withdraw funds from an account.
     * <p>
     * Rejects the withdrawal if the account has insufficient balance.
     * Uses SERIALIZABLE isolation and SELECT FOR UPDATE.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse withdraw(UUID accountId, WithdrawRequest request, UUID userId) {
        // Lock the account row with SELECT FOR UPDATE
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountId));

        // Verify ownership
        if (!account.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this account");
        }

        BigDecimal balanceBefore = account.getBalance();

        // Check sufficient balance BEFORE subtracting
        if (balanceBefore.compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + balanceBefore +
                    ", requested: " + request.amount());
        }

        BigDecimal balanceAfter = balanceBefore.subtract(request.amount());

        // Update balance
        account.setBalance(balanceAfter);
        accountRepository.save(account);

        // Write immutable audit log entry
        auditLogService.logTransaction(
                accountId, TransactionType.WITHDRAWAL, request.amount(),
                balanceBefore, balanceAfter, request.description()
        );

        // Write transaction record
        Transaction transaction = new Transaction(
                account, TransactionType.WITHDRAWAL, request.category(),
                request.amount(), request.description(), null
        );
        transactionRepository.save(transaction);

        // Evict monthly summary cache
        transactionService.evictSummaryCache(accountId);

        // Check budget alerts
        List<BudgetAlertResponse> alerts = budgetService
                .checkBudgetAlerts(accountId, request.category());

        return new TransactionResponse(
                transaction.getId(), transaction.getType(), transaction.getCategory(),
                transaction.getAmount(), transaction.getDescription(),
                transaction.getCreatedAt(), alerts
        );
    }

    /**
     * Get all accounts for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(UUID userId) {
        return accountRepository.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getCreatedAt()
        );
    }
}
