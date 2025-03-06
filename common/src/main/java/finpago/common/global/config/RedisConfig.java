package finpago.common.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ReadFrom;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Profile("prod")
@Configuration
@ConditionalOnClass(RedisClusterConfiguration.class)
@ConditionalOnProperty(prefix = "common.redis", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfig {

    @Bean
    public RedisClusterConfiguration redisClusterConfiguration(RedisProperties redisProperties) {
        if (redisProperties.getCluster() == null || redisProperties.getCluster().getNodes().isEmpty()) {
            throw new IllegalArgumentException("Redis cluster nodes must not be null or empty.");
        }

        RedisClusterConfiguration config = new RedisClusterConfiguration(redisProperties.getCluster().getNodes());
        config.setPassword(RedisPassword.of(redisProperties.getPassword()));
        config.setMaxRedirects(redisProperties.getCluster().getMaxRedirects());
        return config;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisClusterConfiguration redisClusterConfiguration, RedisProperties redisProperties) {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();

        // 기본 타임아웃 설정
        builder.commandTimeout(Duration.ofSeconds(10));

        // Lettuce Cluster Refresh 설정
        if (redisProperties.getLettuce() != null && redisProperties.getLettuce().getCluster() != null) {
            if (redisProperties.getLettuce().getCluster().isAdaptive()) {
                // Adaptive refresh는 사용 가능하지만, autoReconnect는 제거
                builder.shutdownTimeout(Duration.ofSeconds(2));
            }

            // refresh.period 설정 (Spring Boot 3.x에서 Duration으로 변환)
            if (redisProperties.getLettuce().getCluster().getPeriod() != null) {
                builder.shutdownTimeout(Duration.parse("PT" + redisProperties.getLettuce().getCluster().getPeriod().replace("s", "") + "S"));
            }
        }

        // ReadFrom 설정 적용 (replica 읽기 우선 적용)
        if ("replica".equalsIgnoreCase(redisProperties.getReadFrom())) {
            builder.readFrom(ReadFrom.REPLICA_PREFERRED);
        }

        return new LettuceConnectionFactory(redisClusterConfiguration, builder.build());
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // LocalDateTime 같은 타입을 위한 모듈 자동 등록

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        return template;
    }
}

