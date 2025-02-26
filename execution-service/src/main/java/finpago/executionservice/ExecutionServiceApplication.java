package finpago.executionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
public class ExecutionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExecutionServiceApplication.class, args);
    }

}
