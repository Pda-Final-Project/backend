
package finpago.userservice.user.controller;

import finpago.common.global.common.ApiResponse;
import finpago.userservice.user.service.UserService;
import finpago.userservice.user.dto.JoinReqDto;
import finpago.userservice.user.dto.LoginReqDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/api/auth")
@RequiredArgsConstructor
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
        System.out.println("dd");
        String token = userService.login(loginReqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "로그인 성공", token));
    }
}
