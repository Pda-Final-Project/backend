package finpago.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "prod")
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication(scanBasePackages = {"finpago.common.global", "finpago.userservice"})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
