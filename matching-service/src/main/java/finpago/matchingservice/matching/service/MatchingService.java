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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final MatchingProducer matchingProducer;

    // 매수 주문 (시간순 정렬)
    private final PriorityQueue<OrderCreateReqEvent> buyOrders = new PriorityQueue<>(
            Comparator.comparing(OrderCreateReqEvent::getCreatedAt) // 먼저 들어온 주문이 먼저 매칭
    );

    // 매도 주문 (시간순 정렬)
    private final PriorityQueue<OrderCreateReqEvent> sellOrders = new PriorityQueue<>(
            Comparator.comparing(OrderCreateReqEvent::getCreatedAt) //먼저 들어온 주문이 먼저 매칭
    );


    public void processOrder(OrderCreateReqEvent order) {
        System.out.println("Processing order: " + order);
        if (order.getOfferType() == OrderType.BUY) {
            buyOrders.offer(order);
            System.out.println("Buy order: " + order);
        } else {
            sellOrders.offer(order);
            System.out.println("Sell order: " + order);
        }
        processMatching();
    }

    private void processMatching() {
        System.out.println("매칭시작");
        System.out.println("buyOrders: " + buyOrders);
        System.out.println("sellOrders: " + sellOrders);

        while (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
            OrderCreateReqEvent buyOrder = buyOrders.poll();
            OrderCreateReqEvent sellOrder = sellOrders.poll();
            System.out.println("매칭조건확인시작");
            // 매칭 조건 확인: 매수 가격 >= 매도 가격
            if (buyOrder.getOfferPrice() >= sellOrder.getOfferPrice()) {
                long matchedQuantity = Math.min(buyOrder.getOfferQuantity(), sellOrder.getOfferQuantity());

                // 부분 체결 발생
                buyOrder.setOfferQuantity(buyOrder.getOfferQuantity() - matchedQuantity);
                sellOrder.setOfferQuantity(sellOrder.getOfferQuantity() - matchedQuantity);

                System.out.println("체결모듈로 전달한 객체 생성시작");
                // 체결된 주문을 Execution 모듈로 전달
                TradeMatchingEvent tradeEvent = new TradeMatchingEvent(
                        UUID.randomUUID(),               // 새로운 체결 ID
                        buyOrder.getOfferNumber(),       // 매수 주문 ID
                        sellOrder.getOfferNumber(),      // 매도 주문 ID
                        buyOrder.getUserId(),            // 매수자 ID
                        sellOrder.getUserId(),           // 매도자 ID
                        buyOrder.getStockTicker(),       // 주식 티커
                        matchedQuantity,                 // 체결 수량
                        buyOrder.getOfferPrice(),        // 체결 가격
                        LocalDateTime.now()              // 체결 시각
                );

                // Kafka Producer를 통해 Execution 모듈로 전송
                matchingProducer.sendTradeToExecution(tradeEvent);

                // 부분 체결된 주문을 다시 정렬하여 삽입
                if (buyOrder.getOfferQuantity() > 0) {
                    buyOrders.offer(buyOrder);
                }
                if (sellOrder.getOfferQuantity() > 0) {
                    sellOrders.offer(sellOrder);
                }
            } else {
                // 매칭이 안 된 주문은 다시 큐에 삽입
                buyOrders.offer(buyOrder);
                sellOrders.offer(sellOrder);
            }
        }
    }
}
