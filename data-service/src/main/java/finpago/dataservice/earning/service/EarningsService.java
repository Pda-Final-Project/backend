package finpago.dataservice.earning.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.dataservice.earning.entity.Earnings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EarningsService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PREFIX = "stock:%s:earnings"; // Redis Key 패턴

    /**
     * 특정 ticker의 earnings 데이터를 조회하는 메서드
     * @param ticker 조회할 종목 코드 (예: "TSLA")
     * @return List<Earnings> 조회된 실적 데이터 목록
     */
    public List<Earnings> getEarningsByTicker(String ticker) {
        String redisKey = String.format(REDIS_KEY_PREFIX, ticker);
        List<Object> earningsJsonList = redisTemplate.opsForList().range(redisKey, 0, -1);

        if (earningsJsonList == null || earningsJsonList.isEmpty()) {
            return List.of(); // 데이터가 없으면 빈 리스트 반환
        }

        return earningsJsonList.stream()
                .map(json -> convertJsonToEarnings(json.toString()))
                .collect(Collectors.toList());
    }

    /**
     * JSON 데이터를 Earnings 객체로 변환
     * @param json JSON 형식의 문자열
     * @return Earnings 객체
     */
    private Earnings convertJsonToEarnings(String json) {
        try {
            return objectMapper.readValue(json, Earnings.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing earnings JSON: " + json, e);
        }
    }
}