package finpago.matchingservice.matching.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import finpago.common.global.enums.OrderType;
import finpago.common.global.messaging.OrderCreateReqEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.matchingservice.matching.messaging.producer.MatchingProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final MatchingProducer matchingProducer;
    private final StringRedisTemplate redisTemplate;

    //주문 시간순 정렬
    private final PriorityQueue<OrderCreateReqEvent> orders = new PriorityQueue<>(
            Comparator.comparing(OrderCreateReqEvent::getCreatedAt)
    );

    // 미체결 주문 저장
    private final Queue<OrderCreateReqEvent> unmatchedOrders = new PriorityQueue<>(
            Comparator.comparing(OrderCreateReqEvent::getCreatedAt)
    );

    private static final long MAX_WAIT_TIME = 5 *1000; // 5분 (밀리초 단위)

    public void processOrder(OrderCreateReqEvent order) {
        log.info("주문 접수: {}", order);

        orders.offer(order);
        processMatching();

    }

    // Redis에서 최신 20개 체결가 가져오기
    private List<Map<String, Object>> getRecentTradesFromRedis(String stockTicker) {
        String redisKey = "stock:" + stockTicker + ":purchase";
        List<String> tradeList = redisTemplate.opsForList().range(redisKey, 0, 19);

        List<Map<String, Object>> tradeDataList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        if (tradeList != null) {
            for (String tradeJson : tradeList) {
                try {
                    Map<String, Object> tradeData = objectMapper.readValue(tradeJson, new TypeReference<>() {});

                    // 체결가 반올림 + Long타입 변환
                    double originalPrice = (double) tradeData.get("price");
                    tradeData.put("price", Math.round(originalPrice));

                    tradeDataList.add(tradeData);
                } catch (Exception e) {
                    log.error("Redis 체결 데이터 JSON 파싱 오류: {}", e.getMessage());
                }
            }
        }
        // 가격(price) 기준 내림차순 정렬
        tradeDataList.sort((trade1, trade2) -> Long.compare(
                (long) trade2.get("price"),
                (long) trade1.get("price")
        ));

        return tradeDataList;
    }

    @Transactional
    protected void processMatching() {
        log.info("매칭 시작");
        long startTime = System.currentTimeMillis();

        while (!orders.isEmpty()) {
            System.out.println("들어와요11");
            if (System.currentTimeMillis() - startTime > MAX_WAIT_TIME) {
                log.warn("5분 초과 - 미체결 주문을 Order 모듈로 전송");
                moveUnmatchedOrdersToQueue();
                sendUnmatchedOrdersToOrderService();
                return;
            }

            OrderCreateReqEvent order = orders.poll();
            List<Map<String, Object>> recentTrades = getRecentTradesFromRedis(order.getStockTicker());

            if (!recentTrades.isEmpty()) {
                System.out.println("들어와요22");
                // 가격기준 정렬
                List<Map<String, Object>> sortedTrades = recentTrades.stream()
                        .sorted(Comparator.comparing(trade -> (long) trade.get("price")))
                        .toList();

                long maxTradePrice = (long) sortedTrades.get(sortedTrades.size() - 1).get("price");
                long minTradePrice = (long) sortedTrades.get(0).get("price");

                System.out.println("maxTradePrice: " + maxTradePrice);
                System.out.println("minTradePrice: " + minTradePrice);

                if (order.getOfferType() == OrderType.BUY) {
                    System.out.println("들어와요33");
                    if (order.getOfferPrice() > maxTradePrice) {
                        System.out.println("들어와요44");
                        handleTradeExecution(order, order.getOfferQuantity(), 0L, order.getOfferPrice(), true);
                    } else {
                        System.out.println("들어와요55");
                        int matchedIndex = linearSearch(sortedTrades, order.getOfferPrice());
                        System.out.println("matchedIndex: " + matchedIndex);

                        if (matchedIndex != -1) {
                            System.out.println("들어와요66");
                            Map<String, Object> matchedTrade = sortedTrades.get(matchedIndex);
                            System.out.println("matchedTrade은용: " + matchedTrade);

                            Number priceValue = (Number) matchedTrade.get("price");
                            long tradePrice = Math.round(priceValue.doubleValue());

                            Number volumeValue = (Number) matchedTrade.get("volume");
                            long tradeVolume = volumeValue.longValue();


                            System.out.println("tradePrice: " + tradePrice);
                            System.out.println("tradeVolume: " + tradeVolume);
                            Number orderQuantityValue = ((Number) order.getOfferQuantity());
                            long  orderQuantity = orderQuantityValue.longValue();

                            long matchedQuantity = Math.min(orderQuantity, tradeVolume);
                            long unfilledQuantity = orderQuantity - matchedQuantity;

                            System.out.println("matchedQuantity: " + matchedQuantity);
                            System.out.println("unfilledQuantity: " + unfilledQuantity);

                            handleTradeExecution(order, matchedQuantity, unfilledQuantity, tradePrice, true);
                            order.setOfferQuantity(unfilledQuantity);

                            if (unfilledQuantity > 0) {
                                System.out.println("들어와요77");
                                orders.offer(order);
                            }
                        } else {
                            System.out.println("들어와요88");
                            orders.offer(order);
                        }
                    }
                } else {
                    if (order.getOfferPrice() < minTradePrice) {
                        handleTradeExecution(order, order.getOfferQuantity(), 0L, order.getOfferPrice(), false);
                    } else {
                        int matchedIndex = linearSearch(sortedTrades, order.getOfferPrice());
                        if (matchedIndex != -1) {
                            Map<String, Object> matchedTrade = sortedTrades.get(matchedIndex);

                            // Number 타입 변환 후 long으로 변환
                            Number priceValue = (Number) matchedTrade.get("price");
                            long tradePrice = Math.round(priceValue.doubleValue());

                            Number volumeValue = (Number) matchedTrade.get("volume");
                            long tradeVolume = volumeValue.longValue();


                            Number orderQuantityValue = ((Number) order.getOfferQuantity());
                            long  orderQuantity = orderQuantityValue.longValue();

                            long matchedQuantity = Math.min(orderQuantity, tradeVolume);
                            long unfilledQuantity = order.getOfferQuantity() - matchedQuantity;

                            handleTradeExecution(order, matchedQuantity, unfilledQuantity, tradePrice, false);
                            order.setOfferQuantity(unfilledQuantity);

                            if (unfilledQuantity > 0) {
                                orders.offer(order);
                            }
                        } else {
                            orders.offer(order);
                        }
                    }
                }
            }

            if (System.currentTimeMillis() - startTime > MAX_WAIT_TIME) {
                log.warn("5분 초과 - 미체결 주문을 Order 모듈로 전송");
                moveUnmatchedOrdersToQueue();
                sendUnmatchedOrdersToOrderService();
            }
        }
    }

    /**
     * 체결완료 주문 처리
     */
    @Transactional
    protected void handleTradeExecution(OrderCreateReqEvent order, long matchedQuantity, long unfilledQuantity, long matchedPrice, boolean isBuy) {
        System.out.println("체결 처리시작");
        if (isBuy) {
            BuyTradeMatchEvent event = new BuyTradeMatchEvent(
                    generateRandomId(),
                    order.getOfferNumber(),
                    order.getUserId(),
                    order.getStockTicker(),
                    matchedQuantity,
                    unfilledQuantity,
                    matchedPrice,
                    LocalDateTime.now(),
                    order.getOfferQuantity(),
                    getExchangeRateFromRedis(order.getStockTicker()),
                    order.getOfferPrice()
            );
            sendBuyTradeToExecution(event);
        } else {
            SellTradeMatchEvent event = new SellTradeMatchEvent(
                    generateRandomId(),
                    order.getOfferNumber(),
                    order.getUserId(),
                    order.getStockTicker(),
                    matchedQuantity,
                    unfilledQuantity,
                    matchedPrice,
                    LocalDateTime.now(),
                    order.getOfferQuantity(),
                    getExchangeRateFromRedis(order.getStockTicker()),
                    order.getOfferPrice()
            );
            sendSellTradeToExecution(event);
        }
    }

    /**
     * Long 타입 랜덤 ID 생성
     */
    private Long generateRandomId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }

    /**
     * Kafka 통해 Execution 모듈로 체결된 주문을 전송
     */
    private void sendBuyTradeToExecution(BuyTradeMatchEvent event) {
        System.out.println("카프카 메시지 전송 직전");
        matchingProducer.sendBuyTradeToExecution(event);
    }

    private void sendSellTradeToExecution(SellTradeMatchEvent event) {
        matchingProducer.sendSellTradeToExecution(event);
    }

    /**
     * 매칭되지 않은 주문을 unmatchedOrders 큐로 이동
     */
    private void moveUnmatchedOrdersToQueue() {
        while (!orders.isEmpty()) {
            unmatchedOrders.offer(orders.poll());
        }

    }

    /**
     * 미체결 주문을 Order 모듈로 전송
     */
    private void sendUnmatchedOrdersToOrderService() {
        while (!unmatchedOrders.isEmpty()) {
            OrderCreateReqEvent unmatchedOrder = unmatchedOrders.poll();
            matchingProducer.sendUnmatchedOrderToOrderService(unmatchedOrder);
            log.info("미체결 주문 Order 모듈 전송: {}", unmatchedOrder);
        }
    }
    /**
     * 순차 탐색 - 체결가격과 주문가격 비교
     */
    private int linearSearch(List<Map<String, Object>> trades, long targetPrice) {
        System.out.println("순차 탐색 들어와용");
        System.out.println("targetPrice: " + targetPrice);

        for (int i = 0; i < trades.size(); i++) {
            long price = (long) trades.get(i).get("price");
            System.out.println("현재 탐색 중인 가격: " + price);

            if (price == targetPrice) {
                return i; // 일치하는 체결가 발견
            }
        }
        return -1; // 일치하는 체결가 없음
    }

    /**
     * Redis에서 실시간 환율 가져오기
     */
    private Float getExchangeRateFromRedis(String stockTicker) {
        String redisKey = "stock:" + stockTicker + ":exchange_rate";
        String exchangeRateStr = redisTemplate.opsForValue().get(redisKey);
        try {
            return exchangeRateStr != null ? Float.parseFloat(exchangeRateStr) : 1.0f;
        } catch (NumberFormatException e) {
            log.error("Redis 환율 데이터 변환 오류: {}", e.getMessage());
            return 1.0f;
        }
    }
}
