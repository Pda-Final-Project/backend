package finpago.notificationservice.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSettingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String NOTIFICATION_SETTING_KEY = "user:%d:notification-settings";

    public void updateNotificationSetting(Long userId, Boolean enabled) {
        String key = String.format(NOTIFICATION_SETTING_KEY, userId);
        redisTemplate.opsForValue().set(key, enabled, Duration.ofDays(30)); // 30일 유지
        log.info("알림 설정 변경 저장: {} = {}", key, enabled);
    }

    public boolean isNotificationEnabled(Long userId) {
        String key = String.format(NOTIFICATION_SETTING_KEY, userId);
        Boolean enabled = (Boolean) redisTemplate.opsForValue().get(key);
        return enabled != null ? enabled : true; // 기본값 ON
    }
}

