//package finpago.gateway.filter;
//
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//@Component
//public class CustomGlobalFilter implements GlobalFilter, Ordered {
//
//    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomGlobalFilter.class);
//
//    @Override
//    public int getOrder() {
//        return 0; // GlobalFilter 중 높은 우선순위를 가짐
//    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        ServerHttpRequest request = exchange.getRequest();
//
//        log.info("Incoming Request: {} {}", request.getMethod(), request.getURI());
//
//
//        // 로그인/회원가입 요청인지 확인
//        if (request.getURI().getPath().contains("/auth/login") || request.getURI().getPath().contains("/auth/join")) {
//            log.info("Login/Join 요청 Gateway에서 감지됨 - 요청 정상 전달");
//        }
//
//        // Authorization 헤더가 존재하면 로그 출력
//        if (request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
//            log.info("Authorization Header Found: {}", request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
//        } else {
//            log.warn("Authorization Header is Missing!");
//        }
//
//        // Authorization 헤더가 마이크로서비스로 전달되도록 수정
//        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
//                .headers(httpHeaders -> httpHeaders.addAll(request.getHeaders())) // 기존 헤더 유지
//                .build();
//
//        return chain.filter(exchange.mutate().request(modifiedRequest).build());
//    }
//}
//
