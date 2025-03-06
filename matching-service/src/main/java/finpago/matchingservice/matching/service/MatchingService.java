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

    private static final long MAX_WAIT_TIME = 5 * 60 * 1000; // 5분 (밀리초 단위)

    public void processOrder(OrderCreateReqEvent order) {
        log.info("주문 접수: {}", order);

        orders.offer(order);
        processMatching();

    }

    // Redis에서 최신 20개 체결가 가져오기
    private List<Map<String, Object>> getRecentTradesFromRedis(String stockTicker) {
        System.out.println("너가 문제냐?");
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
        return tradeDataList;
    }

    @Transactional
    protected void processMatching() {
        log.info("매칭 시작");
        long startTime = System.currentTimeMillis();

        while (!orders.isEmpty()) {
            System.out.println("들어옵니다");
            if (System.currentTimeMillis() - startTime > MAX_WAIT_TIME) {
                log.warn("5분 초과 - 미체결 주문을 Order 모듈로 전송");
                moveUnmatchedOrdersToQueue();
                sendUnmatchedOrdersToOrderService();
                return;
            }

            OrderCreateReqEvent order = orders.poll();
            System.out.println("레디스 조회전 들어옵니다");
            List<Map<String, Object>> recentTrades = getRecentTradesFromRedis(order.getStockTicker());
            System.out.println("조회된거: " + recentTrades);

            if (!recentTrades.isEmpty()) {
                System.out.println("드렁오나??");
                long maxTradePrice = recentTrades.stream()
                        .mapToLong(trade -> (long) trade.get("price"))
                        .max().orElse(Long.MIN_VALUE);
                long minTradePrice = recentTrades.stream()
                        .mapToLong(trade -> (long) trade.get("price"))
                        .min().orElse(Long.MAX_VALUE);

                if (order.getOfferType() == OrderType.BUY) {
                    if (order.getOfferPrice() > maxTradePrice) {
                        System.out.println("들어옵니다22");
                        handleTradeExecution(order, order.getOfferQuantity(), 0L, order.getOfferPrice(), true);
                    } else {
                        for (Map<String, Object> trade : recentTrades) {
                            System.out.println("들어옵니다 333");
                            long tradePrice = (long) trade.get("price");
                            long tradeVolume = (long) trade.get("volume");

                            if (order.getOfferPrice() == tradePrice) {
                                System.out.println("여기는???");
                                long matchedQuantity = Math.min(order.getOfferQuantity(), tradeVolume);
                                long unfilledQuantity = order.getOfferQuantity() - matchedQuantity;
                                handleTradeExecution(order, matchedQuantity, unfilledQuantity, tradePrice, true);
                                order.setOfferQuantity(unfilledQuantity);

                                if (unfilledQuantity > 0)
                                    orders.offer(order);
                                break;
                            }
                        }
                        System.out.println("아무것도 체결안됨");
                        handleTradeExecution(order, 0L, order.getOfferQuantity(), order.getOfferPrice(), true);

                    }
                }else {
                    if (order.getOfferPrice() < minTradePrice) {
                        handleTradeExecution(order, order.getOfferQuantity(), 0L, order.getOfferPrice(), false);
                    } else {
                        for (Map<String, Object> trade : recentTrades) {
                            long tradePrice = (long) trade.get("price");
                            long tradeVolume = (long) trade.get("volume");

                            if (order.getOfferPrice() == tradePrice) {
                                long matchedQuantity = Math.min(order.getOfferQuantity(), tradeVolume);
                                long unfilledQuantity = order.getOfferQuantity() - matchedQuantity;
                                handleTradeExecution(order, matchedQuantity, unfilledQuantity, tradePrice, false);
                                order.setOfferQuantity(unfilledQuantity);

                                if (unfilledQuantity > 0)
                                    orders.offer(order);
                                break;
                            }
                        }
                    }
                }
            }

            // 5분 초과 시 미체결 주문을 Order 모듈로 전송
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
                    UUID.randomUUID(),
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
                    UUID.randomUUID(),
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
