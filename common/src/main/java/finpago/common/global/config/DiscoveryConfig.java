package finpago.common.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@Configuration
@ConditionalOnClass(EnableDiscoveryClient.class)
@ConditionalOnProperty(name = "common.discovery.enabled", havingValue = "true", matchIfMissing = false)
@EnableDiscoveryClient
public class DiscoveryConfig {
}