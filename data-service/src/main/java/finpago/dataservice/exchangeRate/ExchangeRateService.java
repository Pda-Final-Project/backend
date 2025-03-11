package finpago.dataservice.exchangeRate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();


    // 기본 환율 배율 값 (조회 실패 시 사용) - 1000원 대비 몇 배인지
    private static final Map<String, Double> DEFAULT_EXCHANGE_RATES = new HashMap<>();

    static {
        DEFAULT_EXCHANGE_RATES.put("AAPL", 1320.0);  // 미국 (USD → KRW)
        DEFAULT_EXCHANGE_RATES.put("MSFT", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("NVDA", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("GOOGL", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("AMZN", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("META", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("TSLA", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("LLY", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("AVGO", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("JPM", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("IBM", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("MA", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("ORCL", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("COST", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("UNH", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("NFLX", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("AMD", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("TSM", 42.0);   // 대만 (TWD → KRW)
        DEFAULT_EXCHANGE_RATES.put("ADBE", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("CRM", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("NOW", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("INTC", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("PLTR", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("ASML", 1440.0);  // 네덜란드 (EUR → KRW)
        DEFAULT_EXCHANGE_RATES.put("V", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("XOM", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("KO", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("WMT", 1320.0);
        DEFAULT_EXCHANGE_RATES.put("LIN", 1440.0);  // 독일 (EUR → KRW)
        DEFAULT_EXCHANGE_RATES.put("MS", 1320.0);
    }

    public void  getExchangeRate(String[] tickers) {
        try {
            String URL = "https://m.search.naver.com/p/csearch/content/qapirender.nhn"
                    + "?key=calculator&pkid=141&q=환율&where=m&u1=keb"
                    + "&u6=standardUnit&u7=0&u3=USD&u4=KRW&u8=down&u2=1";
            ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String exchangeRateStr = jsonNode.get("country").get(1).get("value").asText();
                Double exchangeRate = Double.parseDouble(exchangeRateStr.replace(",", ""));

                Double exchangeRateRatio = exchangeRate ;

                for (String ticker : tickers) {
                    saveToRedis(ticker, exchangeRateRatio);
                }

            }
            else {
                System.out.println("환율 API 응답 실패, 기본 환율을 사용합니다.");
                setDefaultExchangeRates(tickers);
            }
        }  catch (Exception e) {
            System.out.println("환율 정보를 가져오는 데 실패했습니다: " + e.getMessage());
            setDefaultExchangeRates(tickers);
        }
    }

    private void saveToRedis(String ticker, Double exchangeRate) {
        String redisKey = "stock:" + ticker + ":exchange_rate";
        redisTemplate.opsForValue().set(redisKey, exchangeRate.toString(), 5, TimeUnit.MINUTES);
        System.out.println(ticker + ": 환율 " + exchangeRate + " (KRW) 저장 완료.");
    }

    private void setDefaultExchangeRates(String[] tickers) {
        for (String ticker : tickers) {
            Double defaultRate = DEFAULT_EXCHANGE_RATES.getOrDefault(ticker, 1.32); // 기본값: USD-KRW
            saveToRedis(ticker, defaultRate);
        }
    }
}