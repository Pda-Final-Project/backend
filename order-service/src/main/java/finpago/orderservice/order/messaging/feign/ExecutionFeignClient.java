package finpago.orderservice.order.messaging.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "execution-service", url = "http://execution-service:19095")
public interface ExecutionFeignClient {
    @GetMapping("/v1/api/execution/stocks")
    Long getUserStocks(@RequestParam Long userId, @RequestParam String stockTicker);
}
