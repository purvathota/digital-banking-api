package com.ledgercore.service;

import com.ledgercore.entity.AuditLog;
import com.ledgercore.enums.TransactionType;
import com.ledgercore.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service responsible for writing immutable audit log entries.
 * <p>
 * This service ONLY inserts new records — it never updates or deletes.
 * The underlying PostgreSQL table has a trigger that raises an exception
 * on any UPDATE or DELETE attempt, enforcing append-only semantics.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Record an immutable audit log entry for a financial transaction.
     *
     * @param accountId     the account involved
     * @param type          the type of transaction (DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT)
     * @param amount        the transaction amount
     * @param balanceBefore the account balance before the operation
     * @param balanceAfter  the account balance after the operation
     * @param description   optional human-readable description
     */
    public void logTransaction(UUID accountId, TransactionType type, BigDecimal amount,
                               BigDecimal balanceBefore, BigDecimal balanceAfter,
                               String description) {
        AuditLog entry = new AuditLog(
                accountId,
                type.name(),
                amount,
                balanceBefore,
                balanceAfter,
                description
        );
        auditLogRepository.save(entry);
    }
}
