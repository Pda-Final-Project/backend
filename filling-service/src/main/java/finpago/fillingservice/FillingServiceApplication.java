package finpago.fillingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"finpago.common.global", "finpago.fillingservice"})
public class FillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FillingServiceApplication.class, args);
    }

}
