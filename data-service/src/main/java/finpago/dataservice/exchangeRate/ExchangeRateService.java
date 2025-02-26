package finpago.dataservice.exchangeRate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.redis.core.RedisTemplate;

@Service
public class ExchangeRateService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExchangeRateService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Double getExchangeRate(String[] tickers) {
        try {
            String URL = "https://m.search.naver.com/p/csearch/content/qapirender.nhn"
                    + "?key=calculator&pkid=141&q=환율&where=m&u1=keb"
                    + "&u6=standardUnit&u7=0&u3=USD&u4=KRW&u8=down&u2=1";
            ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String exchangeRateStr = jsonNode.get("country").get(1).get("value").asText();
                Double exchangeRate = Double.parseDouble(exchangeRateStr.replace(",", ""));

                for (String ticker: tickers){
                    String redisKey = "stock:" + ticker + ":exchange_rate";
                    redisTemplate.opsForValue().set(redisKey, exchangeRate.toString());
                }

                return exchangeRate;
            }
        } catch (Exception e) {
            System.out.println("환율 정보를 가져오는 데 실패했습니다: " + e.getMessage());
        }
        return null;
    }
}
