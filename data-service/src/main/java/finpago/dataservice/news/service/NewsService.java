package finpago.dataservice.news.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.dataservice.news.entity.News;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String NEWS_KEY = "investing:news"; // Redis에서 사용할 Key 이름

    // 최신 뉴스 데이터 조회
    public List<News> getNews() {
        List<String> newsJsonList = redisTemplate.opsForList().range(NEWS_KEY, 0, -1); // ✅ 리스트에서 가져오기
        List<News> newsList = new ArrayList<>();

        if (newsJsonList != null) {
            for (String newsJson : newsJsonList) {
                try {
                    newsList.add(objectMapper.readValue(newsJson, News.class));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("JSON 변환 오류", e);
                }
            }
        }
        return newsList;
    }
}
