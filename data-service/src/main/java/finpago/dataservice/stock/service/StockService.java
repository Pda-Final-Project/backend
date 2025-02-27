package finpago.dataservice.stock.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final StringRedisTemplate redisTemplate;

    public StockService(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    public List<Map<String, String>> getStocks(String sortBy, String searchParam){
        Set<String> keys = redisTemplate.keys("stock:*");
        if (keys==null || keys.isEmpty()){
            return Collections.emptyList();
        }
        List<Map<String, String>> stocks = new ArrayList<>();

        for(String key: keys){
            if (key.matches("stock:[A-Z]+")) {  // 예: stock:TSLA, stock:AAPL
                Map<Object, Object> stockData = redisTemplate.opsForHash().entries(key);
                Map<String, String> stock = new HashMap<>();

                stockData.forEach((k, v) -> stock.put(k.toString(), v.toString()));
                stock.put("ticker", key.split(":")[1]);  // ✅ ticker 값 추가
                stocks.add(stock);
            }
        }

        //검색
        if (searchParam != null && !searchParam.isEmpty()) {
            stocks = stocks.stream()
                    .filter(stock -> stock.get("ticker").equalsIgnoreCase(searchParam) ||
                            stock.get("name").toLowerCase().contains(searchParam.toLowerCase()))
                    .collect(Collectors.toList());
        }

        //정렬 조건
        if ("vol".equals(sortBy)) {
            stocks.sort(Comparator.comparing(s -> Integer.parseInt(s.getOrDefault("volume", "0")), Comparator.reverseOrder()));
        } else if ("rate".equals(sortBy)) {
            stocks.sort(Comparator.comparing(s -> Double.parseDouble(s.getOrDefault("change_rate", "0")), Comparator.reverseOrder()));
        }

        return stocks;
    }
}
