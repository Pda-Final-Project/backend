//package finpago.matchingservice.matching.service;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Random;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class RedisTradeScheduler {
//
//    private final StringRedisTemplate redisTemplate;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    private final Random random = new Random();
//
//    private static final String[] STOCK_TICKERS = {"TSLA", "GOOGLE", "AAPL", "AMZN", "MSFT"};
//
//    /**
//     * 1초마다 Redis 체결 데이터 갱신
//     */
//    @Scheduled(fixedRate = 2000)  // 1초마다 실행
//    public void updateDummyData() {
//        for (String stock : STOCK_TICKERS) {
//            String redisKey = "stock:" + stock + ":purchase";
//
//            // 최신 데이터 삽입
//            Map<String, Object> tradeData = new HashMap<>();
//            tradeData.put("current_price", generateRandomPrice(stock));
//            tradeData.put("volume", random.nextInt(100) + 1); // 1 ~ 100 랜덤 수량
//            tradeData.put("timestamp", System.currentTimeMillis());
//
//            try {
//                String tradeJson = objectMapper.writeValueAsString(tradeData);
//                redisTemplate.opsForList().leftPush(redisKey, tradeJson); // 최신 데이터가 앞에 오도록 저장
//                redisTemplate.opsForList().trim(redisKey, 0, 19); // 최신 20개만 유지
//            } catch (JsonProcessingException e) {
//                log.error("Redis 체결 데이터 갱신 오류: {}", e.getMessage());
//            }
//        }
//        log.info("🔄 Redis 체결 데이터 갱신 완료");
//    }
//
//    /**
//     * 랜덤 가격 생성
//     */
//    private double generateRandomPrice(String stock) {
//        return switch (stock) {
//            case "TSLA" -> 600 + random.nextDouble() * 50;  // 600~650
//            case "GOOGLE" -> 2500 + random.nextDouble() * 100; // 2500~2600
//            case "AAPL" -> 140 + random.nextDouble() * 10; // 140~150
//            case "AMZN" -> 3200 + random.nextDouble() * 200; // 3200~3400
//            case "MSFT" -> 280 + random.nextDouble() * 20; // 280~300
//            default -> 100 + random.nextDouble() * 50; // 기본값
//        };
//    }
//}