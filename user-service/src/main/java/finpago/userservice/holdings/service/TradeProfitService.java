package finpago.userservice.holdings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeProfitService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 실현 손익(KRW) 계산 메서드
     * @param userId 사용자 ID
     * @param sellOfferNumber 매도 주문 번호
     * @return 실현 손익 (KRW)
     */
    public double calculateRealizedProfit(Long userId, String sellOfferNumber) {
        // Redis Key 정의
        String tradeKey = "user:" + userId + ":trades";

        // Redis에서 체결 내역 가져오기
        List<String> tradeList = redisTemplate.opsForList().range(tradeKey, 0, -1);
        if (tradeList == null || tradeList.isEmpty()) {
            return 0.0; // 데이터가 없으면 0 반환
        }

        double totalSellAmount = 0.0;
        double totalBuyAmount = 0.0;

        // 매도 주문 번호가 일치하는 체결 내역 필터링
        List<String> filteredTrades = tradeList.stream()
                .filter(tradeJson -> tradeJson.contains("\"sellOfferNumber\":\"" + sellOfferNumber + "\""))
                .collect(Collectors.toList());

        for (String tradeJson : filteredTrades) {
            try {
                Trade trade = objectMapper.readValue(tradeJson, Trade.class);

                // 매도자인 경우
                if (trade.getSellerUserId().equals(userId)) {
                    totalSellAmount += trade.getTradeQuantity() * trade.getTradePrice();
                }

                // 매수자인 경우
                if (trade.getBuyerUserId().equals(userId)) {
                    totalBuyAmount += trade.getTradeQuantity() * trade.getTradePrice();
                }

            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        // 실현 손익(KRW) 계산
        return totalSellAmount - totalBuyAmount;
    }

    /**
     * Trade 객체 (Redis JSON 변환용)
     */
    private static class Trade {
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
        public String getSellOfferNumber() { return sellOfferNumber; }
        public Long getBuyerUserId() { return buyerUserId; }
        public Long getSellerUserId() { return sellerUserId; }
        public int getTradeQuantity() { return tradeQuantity; }
        public double getTradePrice() { return tradePrice; }
        public double getTradeExchangeRate() { return tradeExchangeRate; }
    }
}
