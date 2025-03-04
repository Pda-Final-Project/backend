package finpago.matchingservice.matching.service;

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

            // Redis에서 해당 주식 티커의 체결 가능 데이터를 가져옴
            Map<String, Object> redisTrade = getMatchingTradeFromRedis(buyOrder.getStockTicker());
            if (redisTrade != null) {
                long redisPrice = (long) redisTrade.get("price");
                long redisVolume = (long) redisTrade.get("volume");

                log.info("Redis 체결 데이터 발견 - 가격: {}, 수량: {}", redisPrice, redisVolume);

                // 현재 주문과 Redis 체결 데이터를 비교하여 매칭 수행
                if (buyOrder.getOfferPrice() >= redisPrice && sellOrder.getOfferPrice() <= redisPrice) {
                    long matchedQuantity = Math.min(Math.min(buyOrder.getOfferQuantity(), sellOrder.getOfferQuantity()), redisVolume);

                    // 체결 처리
                    handleTradeExecution(buyOrder, sellOrder, matchedQuantity, redisPrice);
                } else {
                    log.info(" Redis 데이터와 매칭 불가 - 기존 매칭 로직 수행");
                    matchOrders(buyOrder, sellOrder);
                }
            } else {
                log.info("Redis 체결 데이터 없음 - 기존 매칭 로직 수행");
                matchOrders(buyOrder, sellOrder);
            }
        }

        if (System.currentTimeMillis() - startTime > MAX_WAIT_TIME) {
            log.warn("5분 초과 - 미체결 주문을 Order 모듈로 전송");
            moveUnmatchedOrdersToQueue();
            sendUnmatchedOrdersToOrderService();
        }
    }

    /**
     * 기존 매칭 로직
     */
    private void matchOrders(OrderCreateReqEvent buyOrder, OrderCreateReqEvent sellOrder) {
        if (buyOrder.getOfferPrice() >= sellOrder.getOfferPrice()) {
            long matchedQuantity = Math.min(buyOrder.getOfferQuantity(), sellOrder.getOfferQuantity());

            handleTradeExecution(buyOrder, sellOrder, matchedQuantity, buyOrder.getOfferPrice());
        } else {
            buyOrders.offer(buyOrder);
            sellOrders.offer(sellOrder);
        }
    }

    /**
     * 체결된 주문을 Execution 모듈로 전달
     */
    private void handleTradeExecution(OrderCreateReqEvent buyOrder, OrderCreateReqEvent sellOrder, long matchedQuantity, long matchedPrice) {
        Long buyerOrderQuantity = buyOrder.getOfferQuantity();
        Long sellerOrderQuantity = sellOrder.getOfferQuantity();

        buyOrder.setOfferQuantity(buyOrder.getOfferQuantity() - matchedQuantity);
        sellOrder.setOfferQuantity(sellOrder.getOfferQuantity() - matchedQuantity);

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
                buyerOrderQuantity,
                sellerOrderQuantity
        );

        matchingProducer.sendTradeToExecution(tradeEvent);
        log.info("체결 완료 - Execution 모듈 전송: {}", tradeEvent);

        if (buyOrder.getOfferQuantity() > 0) {
            buyOrders.offer(buyOrder);
        }
        if (sellOrder.getOfferQuantity() > 0) {
            sellOrders.offer(sellOrder);
        }
    }

    /**
     * Redis에서 주어진 주식 티커의 최신 체결 데이터 가져오기
     */
    private Map<String, Object> getMatchingTradeFromRedis(String stockTicker) {
        String redisKey = "stock:" + stockTicker + ":purchase";
        List<String> tradeList = redisTemplate.opsForList().range(redisKey, 0, 1);

        if (tradeList != null && !tradeList.isEmpty()) {
            String tradeJson = tradeList.get(0);
            try {
                return Map.of(
                        "price", Long.parseLong(tradeJson.split(",")[0].split(":")[1].trim()), // 체결가
                        "volume", Long.parseLong(tradeJson.split(",")[1].split(":")[1].trim()) // 체결량
                );
            } catch (Exception e) {
                log.error("Redis 체결 데이터 파싱 오류: {}", e.getMessage());
            }
        }
        return null;
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
}
