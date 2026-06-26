package com.ledgercore.service;

import com.ledgercore.dto.response.BudgetAlertResponse;
import com.ledgercore.dto.response.MonthlySummaryResponse;
import com.ledgercore.dto.response.TransactionResponse;
import com.ledgercore.entity.Transaction;
import com.ledgercore.enums.TransactionCategory;
import com.ledgercore.repository.TransactionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for querying transaction history and generating
 * monthly spending summaries with Redis caching.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Paginated transaction history with optional date range filters.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(UUID accountId,
                                                     LocalDateTime from,
                                                     LocalDateTime to,
                                                     Pageable pageable) {
        // Default date range: last 30 years to now (effectively "all time")
        if (from == null) {
            from = LocalDateTime.of(2000, 1, 1, 0, 0);
        }
        if (to == null) {
            to = LocalDateTime.now().plusDays(1);
        }

        Page<Transaction> transactions = transactionRepository
                .findByAccountIdAndCreatedAtBetween(accountId, from, to, pageable);

        return transactions.map(tx -> new TransactionResponse(
                tx.getId(),
                tx.getType(),
                tx.getCategory(),
                tx.getAmount(),
                tx.getDescription(),
                tx.getCreatedAt()
        ));
    }

    /**
     * Monthly spending summary by category for the given account and month.
     * Cached in Redis with key pattern "monthlySummary::{accountId}:{month}".
     * TTL of 1 hour (configured in RedisConfig).
     */
    @Cacheable(value = "monthlySummary", key = "#accountId + ':' + #month")
    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(UUID accountId, YearMonth month) {
        LocalDateTime fromDate = month.atDay(1).atStartOfDay();
        LocalDateTime toDate = month.plusMonths(1).atDay(1).atStartOfDay();

        List<Object[]> results = transactionRepository
                .findMonthlyCategorySpend(accountId, fromDate, toDate);

        Map<String, BigDecimal> summaryByCategory = new LinkedHashMap<>();
        for (Object[] row : results) {
            TransactionCategory category = (TransactionCategory) row[0];
            BigDecimal total = (BigDecimal) row[1];
            summaryByCategory.put(category.name(), total);
        }

        return new MonthlySummaryResponse(month.toString(), summaryByCategory);
    }

    /**
     * Evict the monthly summary cache for the given account.
     * Called after every new transaction to ensure cache consistency.
     */
    @CacheEvict(value = "monthlySummary", allEntries = true)
    public void evictSummaryCache(UUID accountId) {
        // Cache eviction handled by Spring annotation
    }
}
