package finpago.executionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@SpringBootApplication
@ComponentScan(basePackages = {"finpago.common.global.exception", "finpago.executionservice"})
public class ExecutionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExecutionServiceApplication.class, args);
    }

}
