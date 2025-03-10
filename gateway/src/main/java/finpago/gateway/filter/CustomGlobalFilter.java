package finpago.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomGlobalFilter.class);

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        log.info(" Incoming Request: {} {}", request.getMethod(), request.getURI());
        return chain.filter(exchange);
    }
}
