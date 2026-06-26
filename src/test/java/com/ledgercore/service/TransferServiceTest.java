package com.ledgercore.service;

import com.ledgercore.dto.request.TransferRequest;
import com.ledgercore.dto.response.TransactionResponse;
import com.ledgercore.entity.Account;
import com.ledgercore.entity.User;
import com.ledgercore.enums.AccountType;
import com.ledgercore.enums.TransactionCategory;
import com.ledgercore.enums.TransactionType;
import com.ledgercore.exception.InsufficientBalanceException;
import com.ledgercore.repository.AccountRepository;
import com.ledgercore.repository.TransactionRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private BudgetService budgetService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransferService transferService;

    private User testUser;
    private Account sourceAccount;
    private Account destAccount;
    private UUID userId;
    private UUID sourceId;
    private UUID destId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testUser = new User("test@example.com", "hashedPassword", "Test User");
        testUser.setId(userId);

        sourceId = UUID.randomUUID();
        destId = UUID.randomUUID();

        sourceAccount = new Account(testUser, AccountType.CURRENT);
        sourceAccount.setId(sourceId);
        sourceAccount.setBalance(new BigDecimal("5000.00"));

        destAccount = new Account(testUser, AccountType.SAVINGS);
        destAccount.setId(destId);
        destAccount.setBalance(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Successful transfer debits source and credits destination")
    void transfer_success() {
        // Arrange
        TransferRequest request = new TransferRequest(
                sourceId, destId, new BigDecimal("2000.00"),
                TransactionCategory.OTHER, "Moving savings", null);

        // Determine lock order (ascending UUID)
        UUID firstLock = sourceId.compareTo(destId) < 0 ? sourceId : destId;
        UUID secondLock = sourceId.compareTo(destId) < 0 ? destId : sourceId;

        Account firstAccount = firstLock.equals(sourceId) ? sourceAccount : destAccount;
        Account secondAccount = firstLock.equals(sourceId) ? destAccount : sourceAccount;

        when(accountRepository.findByIdForUpdate(firstLock)).thenReturn(Optional.of(firstAccount));
        when(accountRepository.findByIdForUpdate(secondLock)).thenReturn(Optional.of(secondAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(budgetService.checkBudgetAlerts(any(), any())).thenReturn(Collections.emptyList());

        // Act
        TransactionResponse response = transferService.transfer(request, userId);

        // Assert
        assertThat(sourceAccount.getBalance()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(destAccount.getBalance()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(response.type()).isEqualTo(TransactionType.TRANSFER_OUT);

        // Verify two audit entries were written
        verify(auditLogService, times(2)).logTransaction(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Transfer to same account should throw IllegalArgumentException")
    void transfer_sameAccount_shouldThrow() {
        TransferRequest request = new TransferRequest(
                sourceId, sourceId, new BigDecimal("100.00"),
                TransactionCategory.OTHER, "Self transfer", null);

        assertThatThrownBy(() -> transferService.transfer(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");
    }

    @Test
    @DisplayName("Transfer from account not owned by user should throw AccessDeniedException")
    void transfer_notOwned_shouldThrow() {
        UUID otherUserId = UUID.randomUUID();

        TransferRequest request = new TransferRequest(
                sourceId, destId, new BigDecimal("100.00"),
                TransactionCategory.OTHER, "Transfer", null);

        UUID firstLock = sourceId.compareTo(destId) < 0 ? sourceId : destId;
        UUID secondLock = sourceId.compareTo(destId) < 0 ? destId : sourceId;

        Account firstAccount = firstLock.equals(sourceId) ? sourceAccount : destAccount;
        Account secondAccount = firstLock.equals(sourceId) ? destAccount : sourceAccount;

        when(accountRepository.findByIdForUpdate(firstLock)).thenReturn(Optional.of(firstAccount));
        when(accountRepository.findByIdForUpdate(secondLock)).thenReturn(Optional.of(secondAccount));

        assertThatThrownBy(() -> transferService.transfer(request, otherUserId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Transfer with insufficient source balance should throw InsufficientBalanceException")
    void transfer_insufficientBalance_shouldThrow() {
        TransferRequest request = new TransferRequest(
                sourceId, destId, new BigDecimal("99999.00"),
                TransactionCategory.OTHER, "Too much", null);

        UUID firstLock = sourceId.compareTo(destId) < 0 ? sourceId : destId;
        UUID secondLock = sourceId.compareTo(destId) < 0 ? destId : sourceId;

        Account firstAccount = firstLock.equals(sourceId) ? sourceAccount : destAccount;
        Account secondAccount = firstLock.equals(sourceId) ? destAccount : sourceAccount;

        when(accountRepository.findByIdForUpdate(firstLock)).thenReturn(Optional.of(firstAccount));
        when(accountRepository.findByIdForUpdate(secondLock)).thenReturn(Optional.of(secondAccount));

        assertThatThrownBy(() -> transferService.transfer(request, userId))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");
    }
}
