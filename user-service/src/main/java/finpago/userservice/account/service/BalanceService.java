package finpago.userservice.account.service;

import finpago.userservice.account.dto.BalanceDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceService {

    private final StringRedisTemplate redisTemplate;


    private static final String BALANCE_KEY = "user:%d:balance";
    private static final String AVAILABLE_BALANCE_KEY = "user:%d:available_balance";  // 주문 가능 금액
    private static final String BATCH_BALANCE_KEY = "user:%d:batch_balance";  // 배치 예수금
    private static final String PENDING_UPDATE_KEY = "pending_update:%s:balance:%d"; // D+1, D+2 업데이트 예약 키

    private static final long DEFAULT_BALANCE = 10000000; // 기본 예수금 (1,000만원)
    private static final long EXPIRATION_DAYS = 30; // Redis 데이터 보관 기간 (30일)

    /**
     * 서비스 초기화 시 유저 1~50명에 대해 어제(D-1)와 2일 전(D-2) 날짜의 batch_balance 값을 기본값(10,000,000)으로 설정
     */
    @PostConstruct
    public void initializeBatchBalances() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);

        // 유저 ID 1~50번까지 반복하여 Redis에 값 설정
        IntStream.rangeClosed(1, 50).forEach(userId -> {
            setDefaultBatchBalance(yesterday, (long) userId);
            setDefaultBatchBalance(twoDaysAgo, (long) userId);
        });
    }

    /**
     * Redis에 특정 날짜의 batch_balance가 없으면 기본값(10,000,000)으로 설정
     * @param date 날짜 (D-1 또는 D-2)
     * @param userId 사용자 ID
     */
    private void setDefaultBatchBalance(LocalDate date, Long userId) {
        String key = String.format(PENDING_UPDATE_KEY, date, userId);
        if (redisTemplate.opsForValue().get(key) == null) {
            redisTemplate.opsForValue().set(key, String.valueOf(DEFAULT_BALANCE), EXPIRATION_DAYS, TimeUnit.DAYS);
            log.info("Redis 초기화 완료: {} = {}", key, DEFAULT_BALANCE);
        }
    }

    /**
     * 사용자의 예수금 상세 정보 조회
     * @param userId 사용자 ID
     * @return 예수금 정보 DTO
     */
    public BalanceDto getUserBalance(Long userId) {
        //현재 주문 가능 금액 조회
        Long availableBalance = getCachedAvailableBalance(userId);

        //출금가능금액
        Long balance=getCachedBalance(userId);
        //D+1 예수금 조회 (없으면 가장 최근 데이터 유지)
        String d1Key = String.format(PENDING_UPDATE_KEY, LocalDate.now().plusDays(1), userId);
        Long d1Balance = getLatestBalance(d1Key, DEFAULT_BALANCE);

        //오늘 배치 예수금 조회 (D+2 → 현재 배치 진행 중인 예수금)
        Long batchBalance = getCachedBatchBalance(userId);

        return BalanceDto.builder()
                .availableBalance(availableBalance)  // 사용 가능 예수금
                .d1Balance(d1Balance)                // D+1일 예수금 (없으면 최근 값 유지)// D+2일 예수금 (없으면 최근 값 유지)
                .batchBalance(batchBalance)          // D+2일 예수금 (없으면 최근 값 유지)
                .balance(balance)                   // 출금 가능 금액
                .build();
    }

    /**
     * 사용 가능 예수금 조회
     * @param userId 사용자 ID
     * @return 현재 사용 가능한 예수금
     */
    private Long getCachedAvailableBalance(Long userId) {
        String balanceKey = String.format(AVAILABLE_BALANCE_KEY, userId);
        String balanceStr = redisTemplate.opsForValue().get(balanceKey);
        return balanceStr != null ? Long.parseLong(balanceStr) : DEFAULT_BALANCE;
    }

    /**
     * 계좌 예수금 조회
     * @param userId 사용자 ID
     * @return 계좌 예수금
     */
    private Long getCachedBalance(Long userId) {
        String balanceKey = String.format(BALANCE_KEY, userId);
        String balanceStr = redisTemplate.opsForValue().get(balanceKey);
        return balanceStr != null ? Long.parseLong(balanceStr) : DEFAULT_BALANCE;
    }

    /**
     * 배치 예수금 조회 (D+0 데이터)
     * @param userId 사용자 ID
     * @return 현재 배치 예수금 (없으면 출금 가능 금액 유지)
     */
    private Long getCachedBatchBalance(Long userId) {
        String batchBalanceKey = String.format(BATCH_BALANCE_KEY, userId);
        return getLatestBalance(batchBalanceKey, getCachedAvailableBalance(userId));
    }

    /**
     * Redis에서 최신 예수금 데이터를 가져옴
     * - 키에 해당하는 값이 없으면 최근 존재하는 값을 반환
     * - 초기값이 필요할 경우 defaultValue를 사용
     * @param key Redis Key
     * @param defaultValue 기본 반환 값 (이전 데이터가 없을 경우)
     * @return Redis에서 찾은 값 또는 이전 값 유지
     */
    private Long getLatestBalance(String key, Long defaultValue) {
        String balanceStr = redisTemplate.opsForValue().get(key);

        // 데이터가 없으면 기본값 유지
        if (balanceStr == null) {
            return defaultValue;
        }

        return Long.parseLong(balanceStr);
    }
}
