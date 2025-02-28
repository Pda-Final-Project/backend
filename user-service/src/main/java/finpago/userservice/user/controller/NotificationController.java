package finpago.userservice.user.controller;

import finpago.common.global.common.ApiResponse;
import finpago.userservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final UserService userService;

    /**
     * 사용자의 알림 리스트 조회
     */
    @GetMapping
    public ResponseEntity<List<ApiResponse<String>>> getNotifications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(List.of(ApiResponse.fail(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자")));
        }

        Long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of(ApiResponse.fail(HttpStatus.BAD_REQUEST, "잘못된 사용자 정보")));
        }

        List<ApiResponse<String>> notifications = userService.getNotifications(userId.toString());

        return ResponseEntity.ok(notifications);
    }
}
