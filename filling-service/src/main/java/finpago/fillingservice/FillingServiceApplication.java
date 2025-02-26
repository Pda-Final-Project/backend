package finpago.fillingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"finpago.common.global.exception", "finpago.fillingservice"})
public class FillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FillingServiceApplication.class, args);
    }

}
