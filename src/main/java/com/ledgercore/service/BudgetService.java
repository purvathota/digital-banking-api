package com.ledgercore.service;

import com.ledgercore.dto.request.CreateBudgetRequest;
import com.ledgercore.dto.response.BudgetAlertResponse;
import com.ledgercore.dto.response.BudgetResponse;
import com.ledgercore.entity.Account;
import com.ledgercore.entity.Budget;
import com.ledgercore.enums.TransactionCategory;
import com.ledgercore.exception.AccountNotFoundException;
import com.ledgercore.repository.AccountRepository;
import com.ledgercore.repository.BudgetRepository;
import com.ledgercore.repository.TransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing monthly budgets and checking spending thresholds.
 * Generates alerts when spending reaches 80% or more of the budget limit.
 */
@Service
public class BudgetService {

    private static final BigDecimal ALERT_THRESHOLD = new BigDecimal("0.80");

    private final BudgetRepository budgetRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         AccountRepository accountRepository,
                         TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Set or update a monthly budget limit for a category on an account.
     * Upserts: updates existing budget if one exists for the same account+category.
     */
    @Transactional
    public BudgetResponse setBudget(CreateBudgetRequest request, UUID userId) {
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + request.accountId()));

        if (!account.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this account");
        }

        Optional<Budget> existing = budgetRepository
                .findByAccountIdAndCategory(request.accountId(), request.category());

        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setMonthlyLimit(request.monthlyLimit());
        } else {
            budget = new Budget(account, request.category(), request.monthlyLimit());
        }

        budgetRepository.save(budget);

        return new BudgetResponse(budget.getId(), budget.getCategory(), budget.getMonthlyLimit());
    }

    /**
     * Get all budgets for a given account.
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountId));

        if (!account.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this account");
        }

        return budgetRepository.findAllByAccountId(accountId).stream()
                .map(b -> new BudgetResponse(b.getId(), b.getCategory(), b.getMonthlyLimit()))
                .toList();
    }

    /**
     * Check if the current month's spending in the given category has reached
     * 80% or more of the budget limit. Returns alerts if threshold is exceeded.
     */
    @Transactional(readOnly = true)
    public List<BudgetAlertResponse> checkBudgetAlerts(UUID accountId,
                                                       TransactionCategory category) {
        List<BudgetAlertResponse> alerts = new ArrayList<>();

        Optional<Budget> budgetOpt = budgetRepository
                .findByAccountIdAndCategory(accountId, category);

        if (budgetOpt.isEmpty()) {
            return alerts;
        }

        Budget budget = budgetOpt.get();
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime fromDate = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime toDate = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        BigDecimal currentSpend = transactionRepository
                .sumSpendByCategory(accountId, category, fromDate, toDate);

        BigDecimal threshold = budget.getMonthlyLimit().multiply(ALERT_THRESHOLD);

        if (currentSpend.compareTo(threshold) >= 0) {
            BigDecimal percentageUsed = currentSpend
                    .divide(budget.getMonthlyLimit(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(1, RoundingMode.HALF_UP);

            String message = String.format(
                    "Warning: You have used %s%% of your %s budget (%s / %s)",
                    percentageUsed, category, currentSpend, budget.getMonthlyLimit()
            );

            alerts.add(new BudgetAlertResponse(
                    category,
                    budget.getMonthlyLimit(),
                    currentSpend,
                    percentageUsed,
                    message
            ));
        }

        return alerts;
    }
}
