package finpago.dataservice.earning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.dataservice.earning.entity.Earnings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EarningsService {

    private final RedisTemplate<String, String> redisTemplate; // Redis에서 JSON 데이터를 문자열로 저장
    private final ObjectMapper objectMapper; // JSON 변환기
    private static final String REDIS_KEY_FORMAT = "stock:%s:earnings"; // Redis Key 패턴

    public List<Earnings> getEarningsByTicker(String ticker) {
        String redisKey = String.format(REDIS_KEY_FORMAT, ticker);
        List<String> earningsJsonList = redisTemplate.opsForList().range(redisKey, 0, -1);
        List<Earnings> earningsList = new ArrayList<>();

        if (earningsJsonList != null) {
            for (String earningsJson : earningsJsonList) {
                try {
                    earningsList.add(objectMapper.readValue(earningsJson, Earnings.class));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("JSON 변환 오류", e);
                }
            }
        }
        return earningsList;
    }
}
