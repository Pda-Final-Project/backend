package finpago.orderservice.order.messaging.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "settlement-service", url = "http://settlement-service:19096")
public interface SettlementFeignClient {
    @GetMapping("/v1/api/settlement/balance")
    Long getUserBalance(@RequestParam Long userId);
}
