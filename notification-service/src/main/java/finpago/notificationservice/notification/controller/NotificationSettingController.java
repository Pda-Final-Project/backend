package finpago.notificationservice.notification.controller;

import finpago.notificationservice.notification.service.NotificationSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/api/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @PatchMapping("/switch")
    public ResponseEntity<?> switchNotification(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        Boolean enabled = Boolean.valueOf(request.get("enabled").toString());

        notificationSettingService.updateNotificationSetting(userId, enabled);
        log.info("사용자 {} 알림 설정 변경: {}", userId, enabled);

        return ResponseEntity.ok(Map.of("userId", userId, "notificationEnabled", enabled));
    }
}

