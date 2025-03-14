
package finpago.userservice.user.controller;

import finpago.common.global.common.ApiResponse;
import finpago.userservice.user.service.UserService;
import finpago.userservice.user.dto.JoinReqDto;
import finpago.userservice.user.dto.LoginReqDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    /**
     * 회원가입
     * @param joinReqDto 사용자 회원가입 정보
     * @return 성공 메시지 반환
     */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<String>> join(@RequestBody JoinReqDto joinReqDto) {
        userService.join(joinReqDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "회원가입 완료", "환영합니다!"));
    }

    /**
     * 로그인
     * @param loginReqDto 로그인 정보 (전화번호, 비밀번호)
     * @return JWT 토큰 반환
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginReqDto loginReqDto) {
        System.out.println("요청들옹ㅁ");
        String token = userService.login(loginReqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "로그인 성공", token));
    }

    /**
     * 사용자 정보 조회 (인증 필요)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<String>> getUserInfo(@RequestHeader(value = "X-Authenticated-User", required = false) String userIdHeader) {
        Long userId = getUserIdFromHeader(userIdHeader);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "사용자 정보 조회 성공", "User ID: " + userId));
    }

    /**
     * HTTP 요청 헤더에서 userId 가져오기
     */
    private Long getUserIdFromHeader(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            throw new IllegalStateException("인증되지 않은 사용자");
        }
        return Long.parseLong(userIdHeader);
    }


}
