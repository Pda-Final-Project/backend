package finpago.userservice.common.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {

    SUCCESS(200, "요청이 성공적으로 처리되었습니다."),
    INVALID_TOKEN(401, "토큰이 필요합니다."),
    PINNED_STOCK_ADDED(201, "관심 종목이 추가되었습니다."),
    PINNED_STOCK_ALREADY_EXISTS(409, "이미 추가된 관심 종목입니다.");

    private final int code;
    private final String message;
}
