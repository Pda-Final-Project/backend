package finpago.userservice.user.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.common.global.common.ApiResponse;
import finpago.common.global.exception.error.DuplicateUserPhoneException;
import finpago.common.global.messaging.NoticeEvent;
import finpago.userservice.user.dto.JoinReqDto;
import finpago.userservice.user.dto.LoginReqDto;
import finpago.userservice.user.entity.User;
import finpago.userservice.user.repository.UserRepository;
import finpago.userservice.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    private static final String NOTIFICATION_KEY_PREFIX = "user:%s:notifications"; // Redis 키 패턴
    private static final long EXPIRATION_DAYS = 7; // 알림 보관 기간 (7일)

    public void join(JoinReqDto joinReqDto) {

        if (userRepository.findByUserPhone(joinReqDto.getUserPhone()).isPresent()) {
            throw new DuplicateUserPhoneException("이미 등록된 전화번호입니다.");
        }
        String encodedPassword = passwordEncoder.encode(joinReqDto.getUserPassword());

        User user = User.builder()
                .userPhone(joinReqDto.getUserPhone())
                .userName(joinReqDto.getUserName())
                .userPassword(encodedPassword)
                .userNotificationSwitch(true)
                .build();

        userRepository.save(user);
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
            String notificationJson = objectMapper.writeValueAsString(event);

            // Redis List에 알림 추가 (FIFO 구조)
            redisTemplate.opsForList().rightPush(userKey, notificationJson);
            redisTemplate.expire(userKey, EXPIRATION_DAYS, TimeUnit.DAYS);

            log.info("[UserService] 알림 저장 - 사용자 {}: {}", event.getUserId(), notificationJson);
        } catch (Exception e) {
            log.error("[UserService] 알림 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 사용자 알림 리스트 조회
     */
    public List<ApiResponse<String>> getNotifications(String userId) {
        String userKey = String.format(NOTIFICATION_KEY_PREFIX, userId);
        List<String> notifications = redisTemplate.opsForList().range(userKey, 0, -1);

        if (notifications == null || notifications.isEmpty()) {
            return List.of(ApiResponse.fail(HttpStatus.NO_CONTENT, "알림이 없습니다."));

        }

        return notifications.stream()
                .map(notification -> ApiResponse.success(HttpStatus.OK, "알림 조회 성공", notification))
                .collect(Collectors.toList());

    }

    /**
     * 사용자 알림 설정 변경
     */
    @Transactional
    public void updateNotificationSetting(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자 ID"));

        user.setUserNotificationSwitch(enabled);
        userRepository.save(user);
    }

}