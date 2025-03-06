package finpago.userservice;

import finpago.userservice.account.repository.AccountRepository;
import finpago.userservice.user.service.TradingSettlementBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TradingSettlementBatchServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TradingSettlementBatchService tradingSettlementBatchService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private static final String TEST_USER_ID = "18";
    private static final String TEST_BALANCE = "50000";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testSchedulePendingUpdates() {
        LocalDate executionDate = LocalDate.now().plusDays(2);
        String pendingUpdateKey = "pending_update:" + executionDate;

        when(redisTemplate.keys("user:*:batch_balance")).thenReturn(Set.of("user:" + TEST_USER_ID + ":batch_balance"));
        when(valueOperations.get("user:" + TEST_USER_ID + ":batch_balance")).thenReturn(TEST_BALANCE);

        tradingSettlementBatchService.schedulePendingUpdates();

        verify(valueOperations).set(pendingUpdateKey + ":balance:" + TEST_USER_ID, TEST_BALANCE, 30, java.util.concurrent.TimeUnit.DAYS);
    }

    @Test
    void testApplyPendingUpdates() {
        LocalDate executionDate = LocalDate.of(2025, 3, 8);
        String pendingUpdateKey = "pending_update:" + executionDate;

        when(redisTemplate.keys(pendingUpdateKey + ":balance:*")).thenReturn(Set.of(pendingUpdateKey + ":balance:" + TEST_USER_ID));
        when(valueOperations.get(pendingUpdateKey + ":balance:" + TEST_USER_ID)).thenReturn(TEST_BALANCE);

        // 예수금 업데이트 전 값(Mock 데이터)
        long previousBalance = 30000L;
        when(accountRepository.getBalanceByUserId(Long.parseLong(TEST_USER_ID)))
                .thenReturn(previousBalance);

        tradingSettlementBatchService.applyPendingUpdates();

        // 예수금 업데이트 확인
        verify(accountRepository).updateAccountWithholding(Long.parseLong(TEST_USER_ID), Long.parseLong(TEST_BALANCE));
        verify(valueOperations).set("user:" + TEST_USER_ID + ":balance", TEST_BALANCE, 30, java.util.concurrent.TimeUnit.DAYS);

        // 예수금 변경 검증
        long updatedBalance = Long.parseLong(TEST_BALANCE);
        assertNotEquals(previousBalance, updatedBalance, "예수금이 변경되지 않았습니다.");
    }
}
