package com.ledgercore.repository;

import com.ledgercore.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Paginated transaction history with optional date range filters.
     */
    Page<Transaction> findByAccountIdAndCreatedAtBetween(
            UUID accountId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    /**
     * Aggregates spending by category for a given account within a date range.
     * Used for monthly summary reports.
     * Returns rows of [category (String), totalAmount (BigDecimal)].
     */
    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
           "WHERE t.account.id = :accountId " +
           "AND t.createdAt >= :fromDate AND t.createdAt < :toDate " +
           "AND t.type IN (com.ledgercore.enums.TransactionType.WITHDRAWAL, " +
           "com.ledgercore.enums.TransactionType.TRANSFER_OUT) " +
           "GROUP BY t.category")
    List<Object[]> findMonthlyCategorySpend(
            @Param("accountId") UUID accountId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    /**
     * Sum of spend in a specific category for the current month.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.account.id = :accountId " +
           "AND t.category = :category " +
           "AND t.createdAt >= :fromDate AND t.createdAt < :toDate " +
           "AND t.type IN (com.ledgercore.enums.TransactionType.WITHDRAWAL, " +
           "com.ledgercore.enums.TransactionType.TRANSFER_OUT)")
    BigDecimal sumSpendByCategory(
            @Param("accountId") UUID accountId,
            @Param("category") com.ledgercore.enums.TransactionCategory category,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
