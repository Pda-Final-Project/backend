package finpago.orderservice.order.controller;

import finpago.common.global.common.ApiResponse;
import finpago.orderservice.order.dto.OrderCreateReqDto;
import finpago.orderservice.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;



@RestController
@RequestMapping("/v1/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 🔹매수/매도 주문 요청
     *
     * @param orderCreateReqDto 주문 정보 DTO
     * @return 주문 생성 결과
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<String>> createOrder(
            @RequestBody OrderCreateReqDto orderCreateReqDto) {

        System.out.println("주문 생성 요청 수신됨");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == "anonymousUser") {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자"));
        }

        Long userId = Long.parseLong(authentication.getName());
        System.out.println("인증된 사용자 ID: " + userId);

        UUID offerNumber = orderService.createOrder(userId, orderCreateReqDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "주문 생성 완료", offerNumber.toString()));
    }

}
//@RestController
//@RequestMapping("/v1/api/order")
//@RequiredArgsConstructor
//public class OrderController {
//
//    private final OrderService orderService;
//
//    /**
//     * 매수/매도 주문 요청
//     *
//     * @param token             Authorization 헤더 (Bearer 포함)
//     * @param orderCreateReqDto 주문 정보 DTO
//     * @return 주문 생성 결과
//     */
//    @PostMapping("/create")
//    public ResponseEntity<ApiResponse<String>> createOrder(
//            @RequestHeader("Authorization") String token,
//            @RequestBody OrderCreateReqDto orderCreateReqDto) {
//        System.out.println("start");
//        String jwtToken = token.substring(7);
//        UUID offerNumber = orderService.createOrder(jwtToken, orderCreateReqDto);
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ApiResponse.success(HttpStatus.CREATED, "주문 생성 완료", offerNumber.toString()));
//    }
//
//}
