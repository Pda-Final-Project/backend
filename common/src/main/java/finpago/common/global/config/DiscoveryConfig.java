package finpago.common.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@Configuration
@EnableDiscoveryClient
@ConditionalOnClass(name = "org.springframework.cloud.client.discovery.EnableDiscoveryClient")
public class DiscoveryConfig {
}