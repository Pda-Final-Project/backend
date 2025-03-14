package finpago.gateway.filter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // CSRF 비활성화
                .securityContextRepository(new StatelessWebSessionSecurityContextRepository()) // Stateless 인증 유지
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/login", "/auth/join").permitAll() // 로그인 및 회원가입 경로 허용
                        .anyExchange().permitAll() // 모든 요청 허용
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable) // 기본 HTTP 인증 비활성화
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable); // 폼 로그인 비활성화

        return http.build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true); // 인증 정보 포함 가능
        config.addAllowedOrigin("http://172.16.1.70:5173");
        config.setAllowedOrigins(List.of(
                "http://finpago-bucket.s3-website.ap-northeast-2.amazonaws.com", // S3 정적 호스팅 URL
                "https://uvsp59ev1f.execute-api.ap-northeast-2.amazonaws.com/finpago/api", // API Gateway Invoke URL, // S3 정적 호스팅 URL
                "https://uvsp59ev1f.execute-api.ap-northeast-2.amazonaws.com/finpago/**", // API Gateway Invoke URL
                "https://finpago-nlb-15e4af6205f92f61.elb.ap-northeast-2.amazonaws.com/**",
                "https://finpago-nlb-15e4af6205f92f61.elb.ap-northeast-2.amazonaws.com"
        ));
        config.setAllowedOriginPatterns(List.of("*")); // 특정 Origin 허용
        config.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // 허용할 HTTP 메서드

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    private static class StatelessWebSessionSecurityContextRepository implements ServerSecurityContextRepository {
        private static final Mono<SecurityContext> EMPTY_CONTEXT = Mono.empty();

        @Override
        public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
            return Mono.empty();
        }

        @Override
        public Mono<SecurityContext> load(ServerWebExchange exchange) {
            return EMPTY_CONTEXT;
        }
    }
}
