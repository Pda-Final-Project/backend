package finpago.matchingservice.matching.service;

import finpago.common.global.enums.OrderType;
import finpago.common.global.messaging.OrderCreateReqEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.matchingservice.matching.messaging.producer.MatchingProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Queue;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final MatchingProducer matchingProducer;

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
            // 5분이 지나면 미체결 주문을 Order 모듈로 전송
            if (System.currentTimeMillis() - startTime > MAX_WAIT_TIME) {
                log.warn("5분 초과 - 미체결 주문을 Order 모듈로 전송");

                moveUnmatchedOrdersToQueue();
                sendUnmatchedOrdersToOrderService();
                return;
            }

            OrderCreateReqEvent buyOrder = buyOrders.poll();
            OrderCreateReqEvent sellOrder = sellOrders.poll();

            // 매칭 조건 확인: 매수 가격 >= 매도 가격
            if (buyOrder.getOfferPrice() >= sellOrder.getOfferPrice()) {
                long matchedQuantity = Math.min(buyOrder.getOfferQuantity(), sellOrder.getOfferQuantity());

                // 부분 체결 발생
                buyOrder.setOfferQuantity(buyOrder.getOfferQuantity() - matchedQuantity);
                sellOrder.setOfferQuantity(sellOrder.getOfferQuantity() - matchedQuantity);

                // 체결된 주문을 Execution 모듈로 전달
                TradeMatchingEvent tradeEvent = new TradeMatchingEvent(
                        UUID.randomUUID(),
                        buyOrder.getOfferNumber(),
                        sellOrder.getOfferNumber(),
                        buyOrder.getUserId(),
                        sellOrder.getUserId(),
                        buyOrder.getStockTicker(),
                        matchedQuantity,
                        buyOrder.getOfferPrice(),
                        LocalDateTime.now()
                );

                matchingProducer.sendTradeToExecution(tradeEvent);
                log.info("체결 완료 - Execution 모듈 전송: {}", tradeEvent);

                // 부분 체결된 주문을 다시 삽입
                if (buyOrder.getOfferQuantity() > 0) {
                    buyOrders.offer(buyOrder);
                }
                if (sellOrder.getOfferQuantity() > 0) {
                    sellOrders.offer(sellOrder);
                }
            } else {
                // 매칭이 안 된 주문은 다시 큐에 삽입 (5분 후 unmatchedOrders로 이동 예정)
                buyOrders.offer(buyOrder);
                sellOrders.offer(sellOrder);
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
