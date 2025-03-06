package finpago.matchingservice.matching.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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

    // 매수 주문 (시간순 정렬)
    private final PriorityQueue<OrderCreateReqEvent> buyOrders = new PriorityQueue<>(
            Comparator.comparing(OrderCreateReqEvent::getCreatedAt)
    );

    // 매도 주문 (시간순 정렬)
    private final PriorityQueue<OrderCreateReqEvent> sellOrders = new PriorityQueue<>(
            Comparator.comparing(OrderCreateReqEvent::getCreatedAt)
    );

    // 미체결 주문 저장
    private final Queue<OrderCreateReqEvent> unmatchedOrders = new PriorityQueue<>(
            Comparator.comparing(OrderCreateReqEvent::getCreatedAt)
    );

    private static final long MAX_WAIT_TIME = 5 * 60 * 1000; // 5분 (밀리초 단위)

    public void processOrder(OrderCreateReqEvent order) {
        log.info("주문 접수: {}", order);

        if (order.getOfferType() == OrderType.BUY) {
            buyOrders.offer(order);
            log.info("매수 주문 추가: {}", order);
        } else {
            sellOrders.offer(order);
            log.info("매도 주문 추가: {}", order);
        }

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
        return tradeDataList;
    }

    private void processMatching() {
        log.info("매칭 시작");
        long startTime = System.currentTimeMillis();

        while (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
            if (System.currentTimeMillis() - startTime > MAX_WAIT_TIME) {
                log.warn("5분 초과 - 미체결 주문을 Order 모듈로 전송");
                moveUnmatchedOrdersToQueue();
                sendUnmatchedOrdersToOrderService();
                return;
            }

            OrderCreateReqEvent buyOrder = buyOrders.poll();
            OrderCreateReqEvent sellOrder = sellOrders.poll();

            // Redis에서 최신 체결 데이터 조회
            List<Map<String, Object>> recentTrades = getRecentTradesFromRedis(buyOrder.getStockTicker());

            boolean matchedExternally = false;
            for (Map<String, Object> trade : recentTrades) {
                long tradePrice = (long) trade.get("price");
                long tradeVolume = (long) trade.get("volume");

                // 우리 서비스의 주문과 실제 시장 체결 가격이 동일하면 즉시 체결
                if (buyOrder.getOfferPrice() == tradePrice) {
                    matchedExternally = true;
                    long matchedQuantity = Math.min(buyOrder.getOfferQuantity(), tradeVolume);

                    // 체결 실행
                    handleTradeExecution(buyOrder, sellOrder, matchedQuantity, tradePrice);

                    // 남은 주문 다시 큐에 삽입 (부분 체결 처리)
                    if (buyOrder.getOfferQuantity() > matchedQuantity) {
                        buyOrder.setOfferQuantity(buyOrder.getOfferQuantity() - matchedQuantity);
                        buyOrders.offer(buyOrder);
                    }
                    if (sellOrder.getOfferQuantity() > matchedQuantity) {
                        sellOrder.setOfferQuantity(sellOrder.getOfferQuantity() - matchedQuantity);
                        sellOrders.offer(sellOrder);
                    }
                    break; // 외부 체결 완료 시 내부 매칭 패스
                }
            }

            // 외부 체결이 없으면 기존 내부 매칭 수행
            if (!matchedExternally) {
                if (buyOrder.getOfferPrice() >= sellOrder.getOfferPrice()) {
                    long matchedQuantity = Math.min(buyOrder.getOfferQuantity(), sellOrder.getOfferQuantity());

                    // 체결 실행
                    handleTradeExecution(buyOrder, sellOrder, matchedQuantity, buyOrder.getOfferPrice());

                    // 부분 체결된 주문 다시 큐에 삽입
                    if (buyOrder.getOfferQuantity() > matchedQuantity) {
                        buyOrder.setOfferQuantity(buyOrder.getOfferQuantity() - matchedQuantity);
                        buyOrders.offer(buyOrder);
                    }
                    if (sellOrder.getOfferQuantity() > matchedQuantity) {
                        sellOrder.setOfferQuantity(sellOrder.getOfferQuantity() - matchedQuantity);
                        sellOrders.offer(sellOrder);
                    }
                } else {
                    // 매칭되지 않으면 다시 큐에 삽입
                    buyOrders.offer(buyOrder);
                    sellOrders.offer(sellOrder);
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


    /**
     * 체결완료 주문 처리
     */
    private void handleTradeExecution(OrderCreateReqEvent buyOrder, OrderCreateReqEvent sellOrder, long matchedQuantity, long matchedPrice) {
        log.info("주문 체결 실행: 매수 주문 ID={}, 매도 주문 ID={}, 체결량={}, 체결가={}",
                buyOrder.getOfferNumber(), sellOrder.getOfferNumber(), matchedQuantity, matchedPrice);

        sendTradeToExecution(buyOrder, sellOrder, matchedQuantity, matchedPrice);
    }

    /**
     * Kafka 통해 Execution 모듈로 체결된 주문을 전송
     */
    private void sendTradeToExecution(OrderCreateReqEvent buyOrder, OrderCreateReqEvent sellOrder, long matchedQuantity, long matchedPrice) {
        TradeMatchingEvent tradeEvent = new TradeMatchingEvent(
                UUID.randomUUID(),
                buyOrder.getOfferNumber(),
                sellOrder.getOfferNumber(),
                buyOrder.getUserId(),
                sellOrder.getUserId(),
                buyOrder.getStockTicker(),
                matchedQuantity,
                matchedPrice,
                LocalDateTime.now(),
                buyOrder.getOfferQuantity(), // 원래 매수주문수량
                sellOrder.getOfferQuantity(), // 원래 매도주문수량
                getExchangeRateFromRedis(buyOrder.getStockTicker()) // 환율정보
        );

        matchingProducer.sendTradeToExecution(tradeEvent);
        log.info("체결된 주문 Execution 모듈로 전송: {}", tradeEvent);
    }

    /**
     * 매칭되지 않은 주문을 unmatchedOrders 큐로 이동
     */
    private void moveUnmatchedOrdersToQueue() {
        while (!buyOrders.isEmpty()) {
            unmatchedOrders.offer(buyOrders.poll());
        }
        while (!sellOrders.isEmpty()) {
            unmatchedOrders.offer(sellOrders.poll());
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
