package finpago.gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> openApiEndpoints = List.of(
            "/v1/api/auth/.*",   // 🔥 "*" → ".*" 로 변환 (정규식 사용)
            "/v1/api/fillings/.*"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints.stream()
                    .noneMatch(uri -> request.getURI().getPath().matches(uri)); // 🔥 matches() 사용
}
