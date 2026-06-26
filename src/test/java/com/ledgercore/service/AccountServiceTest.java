package com.ledgercore.service;

import com.ledgercore.dto.request.DepositRequest;
import com.ledgercore.dto.request.WithdrawRequest;
import com.ledgercore.dto.response.TransactionResponse;
import com.ledgercore.entity.Account;
import com.ledgercore.entity.User;
import com.ledgercore.enums.AccountType;
import com.ledgercore.enums.TransactionCategory;
import com.ledgercore.enums.TransactionType;
import com.ledgercore.exception.InsufficientBalanceException;
import com.ledgercore.repository.AccountRepository;
import com.ledgercore.repository.TransactionRepository;
import com.ledgercore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private BudgetService budgetService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private AccountService accountService;

    private User testUser;
    private Account testAccount;
    private UUID userId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testUser = new User("test@example.com", "hashedPassword", "Test User");
        testUser.setId(userId);

        testAccount = new Account(testUser, AccountType.SAVINGS);
        testAccount.setId(accountId);
        testAccount.setBalance(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Deposit should increase balance and log audit entry")
    void deposit_shouldIncreaseBalance() {
        // Arrange
        DepositRequest request = new DepositRequest(
                new BigDecimal("500.00"), TransactionCategory.OTHER, "Salary", null);

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            var tx = invocation.getArgument(0, com.ledgercore.entity.Transaction.class);
            return tx;
        });
        when(budgetService.checkBudgetAlerts(any(), any())).thenReturn(Collections.emptyList());

        // Act
        TransactionResponse response = accountService.deposit(accountId, request, userId);

        // Assert
        assertThat(testAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(response.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("500.00"));

        // Verify audit log was written
        verify(auditLogService).logTransaction(
                any(UUID.class), any(TransactionType.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), any()
        );
    }

    @Test
    @DisplayName("Withdrawal should decrease balance")
    void withdraw_shouldDecreaseBalance() {
        // Arrange
        WithdrawRequest request = new WithdrawRequest(
                new BigDecimal("300.00"), TransactionCategory.GROCERIES, "ATM withdrawal");

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            var tx = invocation.getArgument(0, com.ledgercore.entity.Transaction.class);
            return tx;
        });
        when(budgetService.checkBudgetAlerts(any(), any())).thenReturn(Collections.emptyList());

        // Act
        TransactionResponse response = accountService.withdraw(accountId, request, userId);

        // Assert
        assertThat(testAccount.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(response.type()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    @Test
    @DisplayName("Withdrawal with insufficient balance should throw InsufficientBalanceException")
    void withdraw_insufficientBalance_shouldThrow() {
        // Arrange
        WithdrawRequest request = new WithdrawRequest(
                new BigDecimal("2000.00"), TransactionCategory.BILLS, "Rent");

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThatThrownBy(() -> accountService.withdraw(accountId, request, userId))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    @DisplayName("Deposit on account belonging to different user should throw AccessDeniedException")
    void deposit_wrongUser_shouldThrow() {
        // Arrange
        UUID differentUserId = UUID.randomUUID();
        DepositRequest request = new DepositRequest(
                new BigDecimal("100.00"), TransactionCategory.OTHER, "Test", null);

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThatThrownBy(() -> accountService.deposit(accountId, request, differentUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("do not own");
    }
}
