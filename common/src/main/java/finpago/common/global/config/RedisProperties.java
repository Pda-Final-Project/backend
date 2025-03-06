package finpago.common.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Getter
@Setter
@Profile("prod")
@ConfigurationProperties(prefix = "common.redis")
public class RedisProperties {
    private boolean enabled;
    private String password;
    private int maxRedirects;
    private String clientType;
    private String readFrom;
    private LettuceConfig lettuce;
    private ClusterConfig cluster;  // ✅ "cluster" 속성 추가

    @Getter
    @Setter
    public static class ClusterConfig {
        private List<String> nodes;
        private int maxRedirects;
    }

    @Getter
    @Setter
    public static class LettuceConfig {
        private ClusterRefreshConfig cluster;

        @Getter
        @Setter
        public static class ClusterRefreshConfig {
            private boolean adaptive;
            private String period;
        }
    }
}
