//package finpago.gateway.filter;
//
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
//import org.springframework.stereotype.Component;
//
//@Component
//public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {
//
//    public AuthenticationFilter() {
//        super(Config.class);
//    }
//
//    @Override
//    public GatewayFilter apply(Config config) {
//        return (exchange, chain) -> {
//            System.out.println("Gateway Filter - 요청 통과");
//            System.out.println("Request URI: " + exchange.getRequest().getURI());
//            System.out.println("Request Headers: " + exchange.getRequest().getHeaders());
//
//            return chain.filter(exchange); // 모든 요청을 그대로 통과시킴
//        };
//    }
//
//    public static class Config {
//    }
//}
