package finpago.userservice.holdings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeFetchService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 사용자의 매수(Buyer) 체결 내역 조회
     * @param userId 사용자 ID
     * @return 매수 내역 리스트 (List<BuyTrade>)
     */
    public List<BuyTrade> getUserBuyTrades(Long userId) {
        String tradeKey = "user:" + userId + ":buy-trades";
        return fetchTradesFromRedis(tradeKey, BuyTrade.class);
    }

    /**
     * 사용자의 매도(Seller) 체결 내역 조회
     * @param userId 사용자 ID
     * @return 매도 내역 리스트 (List<SellTrade>)
     */
    public List<SellTrade> getUserSellTrades(Long userId) {
        String tradeKey = "user:" + userId + ":sell-trades";
        return fetchTradesFromRedis(tradeKey, SellTrade.class);
    }

    /**
     * Redis에서 특정 타입의 체결 내역을 가져오는 공통 메서드
     */
    private <T> List<T> fetchTradesFromRedis(String tradeKey, Class<T> tradeClass) {
        List<String> tradeListJson = redisTemplate.opsForList().range(tradeKey, 0, -1);
        if (tradeListJson == null || tradeListJson.isEmpty()) {
            return new ArrayList<>();
        }
        return tradeListJson.stream().map(tradeJson -> {
            try {
                return objectMapper.readValue(tradeJson, tradeClass);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(trade -> trade != null).collect(Collectors.toList());
    }

    /**
     * BuyTrade 객체 (Redis JSON 변환용)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BuyTrade {
        private UUID buyTradeNumber;
        private UUID buyOfferNumber;
        private String tradeTicker;
        private Long buyerUserId;
        private String tradeDate;
        private Long tradeQuantity;
        private Long unfilledQuantity;
        private Long tradePrice;
        private Float tradeExchangeRate;
        private String tradeStatus;
        private Long buyerOfferPrice;
        private Long buyerOrderQuantity;
    }

    /**
     * SellTrade 객체 (Redis JSON 변환용)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SellTrade {
        private UUID sellTradeNumber;
        private UUID sellOfferNumber;
        private String tradeTicker;
        private Long sellerUserId;
        private String tradeDate;
        private Long tradeQuantity;
        private Long unfilledQuantity;
        private Long tradePrice;
        private Float tradeExchangeRate;
        private String tradeStatus;
        private Long sellerOfferPrice;
        private Long sellerOrderQuantity;
    }
}

