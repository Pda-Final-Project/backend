package finpago.userservice.holdings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeFetchService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 사용자의 전체 체결 내역 중 매도(Seller) 내역 필터링
     * @param userId 사용자 ID
     * @return 매도 내역 리스트 (List<Trade>)
     */
    public List<Trade> getUserSellTrades(Long userId) {
        // Redis Key 정의
        String tradeKey = "user:" + userId + ":trades";

        // Redis에서 체결 내역 가져오기
        List<String> tradeListJson = redisTemplate.opsForList().range(tradeKey, 0, -1);
        if (tradeListJson == null || tradeListJson.isEmpty()) {
            return new ArrayList<>(); // 데이터가 없으면 빈 리스트 반환
        }

        List<Trade> tradeList = new ArrayList<>();

        // JSON을 Trade 객체로 변환
        for (String tradeJson : tradeListJson) {
            try {
                Trade trade = objectMapper.readValue(tradeJson, Trade.class);
                tradeList.add(trade);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        // 사용자가 Seller인 거래만 필터링
        return tradeList.stream()
                .filter(trade -> trade.getSellerUserId().equals(userId))
                .collect(Collectors.toList());
    }

    /**
     * Trade 객체 (Redis JSON 변환용)
     */
    public static class Trade {
        private String buyOfferNumber;
        private String sellOfferNumber;
        private String tradeTicker;
        private Long buyerUserId;
        private Long sellerUserId;
        private String tradeDate;
        private int tradeQuantity;
        private double tradePrice;
        private double tradeExchangeRate;
        private String tradeStatus;

        // 기본 생성자
        public Trade() {}

        // Getter & Setter
        public String getTradeTicker() { return tradeTicker; }
        public Long getSellerUserId() { return sellerUserId; }
        public int getTradeQuantity() { return tradeQuantity; }
        public double getTradePrice() { return tradePrice; }
        public double getTradeExchangeRate() { return tradeExchangeRate; }
        public String getTradeDate() { return tradeDate; }
    }
}
