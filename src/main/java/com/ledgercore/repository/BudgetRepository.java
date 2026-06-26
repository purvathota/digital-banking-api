package com.ledgercore.repository;

import com.ledgercore.entity.Budget;
import com.ledgercore.enums.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Optional<Budget> findByAccountIdAndCategory(UUID accountId, TransactionCategory category);

    List<Budget> findAllByAccountId(UUID accountId);
}
