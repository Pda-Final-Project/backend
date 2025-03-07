package finpago.common.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ReadFrom;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * 환경별 Redis 설정 (dev = 단일 인스턴스, prod = 클러스터)
 */
@Configuration
@ConditionalOnProperty(prefix = "common.redis", name = "enabled", havingValue = "true")
public class RedisConfig {

    // Dev 환경 Redis 설정 (Standalone)
    private static final String DEV_REDIS_HOST = "localhost";
    private static final int DEV_REDIS_PORT = 6379;

    // Prod 환경 Redis 클러스터 설정 (쿠버네티스 Headless Service)
    private static final List<String> PROD_CLUSTER_NODES = List.of(
            "redis-cluster-0.redis-cluster-headless.redis.svc.cluster.local:6379",
            "redis-cluster-1.redis-cluster-headless.redis.svc.cluster.local:6379",
            "redis-cluster-2.redis-cluster-headless.redis.svc.cluster.local:6379",
            "redis-cluster-3.redis-cluster-headless.redis.svc.cluster.local:6379",
            "redis-cluster-4.redis-cluster-headless.redis.svc.cluster.local:6379",
            "redis-cluster-5.redis-cluster-headless.redis.svc.cluster.local:6379"
    );
    private static final String PROD_REDIS_PASSWORD = "CTg0n49k0M";
    private static final int MAX_REDIRECTS = 3;
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Redis 연결 팩토리 설정 (dev: 단일 인스턴스, prod: 클러스터)
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder()
                .commandTimeout(COMMAND_TIMEOUT)
                .readFrom(ReadFrom.REPLICA_PREFERRED); // ✅ replica 읽기 우선 설정 (공통 적용)

        return isProdProfile()
                ? new LettuceConnectionFactory(createClusterConfig(), builder.build())
                : new LettuceConnectionFactory(createStandaloneConfig(), builder.build());
    }

    /**
     * 단일 인스턴스 Redis 설정 (dev 환경)
     */
    private RedisStandaloneConfiguration createStandaloneConfig() {
        return new RedisStandaloneConfiguration(DEV_REDIS_HOST, DEV_REDIS_PORT);
    }

    /**
     * Redis 클러스터 설정 (prod 환경)
     */
    private RedisClusterConfiguration createClusterConfig() {
        RedisClusterConfiguration config = new RedisClusterConfiguration(PROD_CLUSTER_NODES);
        config.setPassword(RedisPassword.of(PROD_REDIS_PASSWORD));
        config.setMaxRedirects(MAX_REDIRECTS);
        return config;
    }

    /**
     * RedisTemplate 설정 (모든 환경에서 공통 사용 가능)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        return template;
    }

    /**
     * 현재 환경이 prod인지 확인하는 메서드
     */
    private boolean isProdProfile() {
        String profile = System.getProperty("spring.profiles.active", System.getenv("SPRING_PROFILES_ACTIVE"));
        return "prod".equalsIgnoreCase(profile);
    }
}
