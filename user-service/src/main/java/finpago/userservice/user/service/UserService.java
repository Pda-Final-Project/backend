package finpago.userservice.user.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.common.global.common.ApiResponse;
import finpago.common.global.exception.error.DuplicateUserPhoneException;
import finpago.common.global.messaging.FillingNoticeEvent;
import finpago.common.global.messaging.NoticeEvent;
import finpago.userservice.account.entity.Account;
import finpago.userservice.account.repository.AccountRepository;
import finpago.userservice.user.dto.JoinReqDto;
import finpago.userservice.user.dto.LoginReqDto;
import finpago.userservice.user.entity.User;
import finpago.userservice.user.repository.UserRepository;
import finpago.userservice.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    //private final StringRedisTemplate redisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    private static final String NOTIFICATION_KEY_PREFIX = "user:%s:notifications";
    private static final String FILLING_NOTICE_KEY_PREFIX = "user:filling-notices:%s";// Redis 키 패턴
    private static final long EXPIRATION_DAYS = 7; // 알림 보관 기간 (7일)
    private static final String BANK_NAME = "[CMA 종합 계좌]";
    private static final long DEFAULT_WITHHOLDING = 10_000_000L;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String NOTIFICATION_SETTING_KEY = "user:%d:notification-settings";


    public void join(JoinReqDto joinReqDto) {
        Optional<User> existingUser = userRepository.findByUserPhone(joinReqDto.getUserPhone());
        if (existingUser.isPresent()) {
            throw new DuplicateUserPhoneException("이미 등록된 전화번호입니다.");
        }

        String encodedPassword = passwordEncoder.encode(joinReqDto.getUserPassword());
        String encodedPin = passwordEncoder.encode(joinReqDto.getAccountPassword());

        User user = User.builder()
                .userPhone(joinReqDto.getUserPhone())
                .userName(joinReqDto.getUserName())
                .userPassword(encodedPassword)
                .userNotificationSwitch(true)
                .build();

        userRepository.save(user);

        // 계좌 생성
        Account account = Account.builder()
                .userId(user.getUserId())
                .accountName(BANK_NAME)
                .accountNumber(generateRandomAccountNumber())
                .accountPassword(encodedPin)
                .accountWithholding(DEFAULT_WITHHOLDING)
                .build();

        accountRepository.save(account);
    }

    private String generateRandomAccountNumber() {
        StringBuilder accountNumber = new StringBuilder("270"); // 계좌번호 앞자리 "270" 고정
        for (int i = 0; i < 9; i++) { // 나머지 9자리 랜덤 생성
            accountNumber.append(RANDOM.nextInt(10));
        }
        return accountNumber.toString();
    }

    public String login(LoginReqDto loginReqDto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginReqDto.getUserPhone(), loginReqDto.getUserPassword()));

        User user = userRepository.findByUserPhone(loginReqDto.getUserPhone())
                .orElseThrow(() -> new UsernameNotFoundException("등록되지 않은 전화번호입니다."));

        return jwtUtil.generateToken(user.getUserId());
    }

    //알림 메시지를 Redis에 저장
    public void saveNotification(NoticeEvent event) {
        try {
            String userKey = String.format(NOTIFICATION_KEY_PREFIX, event.getUserId());
            String timestampKey = userKey + ":timestamps";
            String notificationJson = objectMapper.writeValueAsString(event);

            // Redis List에 알림 추가 (FIFO 구조)
            redisTemplate.opsForList().rightPush(userKey, notificationJson);
            redisTemplate.opsForList().rightPush(timestampKey, String.valueOf(System.currentTimeMillis()));

            redisTemplate.expire(userKey, EXPIRATION_DAYS, TimeUnit.DAYS);
            redisTemplate.expire(timestampKey, EXPIRATION_DAYS, TimeUnit.DAYS);

            log.info("[UserService] 알림 저장 - 사용자 {}: {}", event.getUserId(), notificationJson);
        } catch (Exception e) {
            log.error("[UserService] 알림 저장 실패: {}", e.getMessage());
        }
    }
    public void saveFillingNotice(FillingNoticeEvent event) {
        try {
            String tickerKey = String.format(FILLING_NOTICE_KEY_PREFIX, event.getTicker());
            String timestampKey = tickerKey + ":timestamps";
            String fillingNoticeJson = objectMapper.writeValueAsString(event);

            redisTemplate.opsForList().rightPush(tickerKey, fillingNoticeJson);
            redisTemplate.opsForList().rightPush(timestampKey, String.valueOf(System.currentTimeMillis()));

            redisTemplate.expire(tickerKey, EXPIRATION_DAYS, TimeUnit.DAYS);
            redisTemplate.expire(timestampKey, EXPIRATION_DAYS, TimeUnit.DAYS);

            log.info("[UserService] 공시 알림 저장 - 티커 {}: {}", event.getTicker(), fillingNoticeJson);
        } catch (Exception e) {
            log.error("[UserService] 공시 알림 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 사용자 알림 리스트 조회
     */
    public List<ApiResponse<Map<String, Object>>> getNotifications(String userId) {
        String userKey = String.format(NOTIFICATION_KEY_PREFIX, userId);
        String timestampKey = userKey + ":timestamps";

        List<String> notifications = Optional.ofNullable(redisTemplate.opsForList().range(userKey, 0, -1))
                .orElse(List.of())  // null 방지
                .stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        List<String> timestamps = Optional.ofNullable(redisTemplate.opsForList().range(timestampKey, 0, -1))
                .orElse(List.of())
                .stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        if (notifications.isEmpty()) {
            return List.of(ApiResponse.fail(HttpStatus.NO_CONTENT, "알림이 없습니다."));
        }

        return IntStream.range(0, Math.min(notifications.size(), timestamps.size()))
                .mapToObj(i -> {
                    try {
                        NoticeEvent event = objectMapper.readValue(notifications.get(i), NoticeEvent.class);
                        long timestamp = Long.parseLong(timestamps.get(i));

                        return ApiResponse.success(HttpStatus.OK, "알림 조회 성공",
                                Map.of("event", event, "timestamp", timestamp));
                    } catch (Exception e) {
                        log.error("알림 데이터 파싱 오류: {}", e.getMessage());
                        return null;  // 변환 실패한 경우 무시
                    }
                })
                .filter(Objects::nonNull) // 변환 실패한 항목 제거
                .collect(Collectors.toList());
    }


    /**
     * 사용자 알림 설정 변경
     */
    public void updateNotificationSetting(Long userId, Boolean enabled) {
        String key = String.format(NOTIFICATION_SETTING_KEY, userId);
        redisTemplate.opsForValue().set(key, enabled, Duration.ofDays(30)); // 30일 유지
        log.info("알림 설정 변경 저장: {} = {}", key, enabled);
    }

    /**
     * 사용자 알림 설정 조회
     */
    public boolean isNotificationEnabled(Long userId) {
        String key = String.format(NOTIFICATION_SETTING_KEY, userId);
        Boolean enabled = (Boolean) redisTemplate.opsForValue().get(key);
        return enabled != null ? enabled : true; // 기본값
    }
}