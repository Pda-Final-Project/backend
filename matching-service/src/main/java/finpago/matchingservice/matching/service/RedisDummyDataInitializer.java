package finpago.matchingservice.matching.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDummyDataInitializer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    private static final String[] STOCK_TICKERS = {"TSLA", "GOOGLE", "AAPL", "AMZN", "MSFT"};

    @PostConstruct
    public void initDummyData() {
        for (String stock : STOCK_TICKERS) {
            String redisKey = "stock:" + stock + ":purchase";

            // 최신 20개 체결가 데이터 삽입
            for (int i = 0; i < 20; i++) {
                Map<String, Object> tradeData = new HashMap<>();
                tradeData.put("price", generateRandomPrice(stock));
                tradeData.put("volume", random.nextInt(100) + 1); // 1 ~ 100 랜덤 수량
                tradeData.put("timestamp", System.currentTimeMillis() - (i * 1000L)); // 과거 타임스탬프

                try {
                    String tradeJson = objectMapper.writeValueAsString(tradeData);
                    redisTemplate.opsForList().leftPush(redisKey, tradeJson); // 최신 데이터가 앞에 오도록 저장
                } catch (JsonProcessingException e) {
                    log.error("Redis 더미 데이터 저장 오류: {}", e.getMessage());
                }
            }
            log.info("{} 체결 데이터 Redis 저장 완료 (키: {})", stock, redisKey);
        }
    }

    private double generateRandomPrice(String stock) {
        return switch (stock) {
            case "TSLA" -> 600 + random.nextDouble() * 50;  // 600~650
            case "GOOGLE" -> 2500 + random.nextDouble() * 100; // 2500~2600
            case "AAPL" -> 140 + random.nextDouble() * 10; // 140~150
            case "AMZN" -> 3200 + random.nextDouble() * 200; // 3200~3400
            case "MSFT" -> 280 + random.nextDouble() * 20; // 280~300
            default -> 100 + random.nextDouble() * 50; // 기본값
        };
    }
}
