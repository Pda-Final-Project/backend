package finpago.gateway.filter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)  // CSRF 비활성화
                .cors(cors -> cors.disable()) // ❌ corsConfigurationSource() 대신 직접 비활성화
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/v1/api/auth/**", "/v1/api/fillings/**").permitAll()  // 🔥 인증 없이 허용할 경로
                        .anyExchange().authenticated()  // 나머지 요청은 인증 필요
                );

        return http.build();
    }

    // ✅ CORS 필터를 Bean으로 등록 (WebFlux에서 권장)
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("*"));  // ✅ 프론트엔드 도메인으로 변경하는 것이 좋음
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter((CorsConfigurationSource) source);  // ✅ `CorsWebFilter`로 감싸서 반환 (WebFlux 지원)
    }
}
