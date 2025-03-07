package finpago.notificationservice.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "http://user-service:19092")
public interface UserClient {

    @GetMapping("/v1/api/notification/status/{userId}")
    Boolean getNotificationSetting(@PathVariable("userId") Long userId);

}
