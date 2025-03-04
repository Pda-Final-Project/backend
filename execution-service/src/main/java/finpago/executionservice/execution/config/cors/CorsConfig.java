package finpago.executionservice.execution.config.cors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 API 엔드포인트에 대해 CORS 허용
                        .allowedOriginPatterns("*") // 모든 도메인 허용 (필요 시 특정 도메인으로 변경 가능)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                        .allowCredentials(false) // 인증 정보 포함 여부 (SSE에서는 false 가능)
                        .maxAge(3600); // 캐시 시간 (1시간)
            }
        };
    }
}
