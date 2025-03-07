package finpago.matchingservice.matching.messaging.producer;

import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.OrderCreateReqEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingProducer {

    private final KafkaTemplate<String, BuyTradeMatchEvent> buyTradeKafkaTemplate;
    private final KafkaTemplate<String, SellTradeMatchEvent> sellTradeKafkaTemplate;
    private final KafkaTemplate<String, OrderCreateReqEvent> orderKafkaTemplate;

    private static final String SELL_TRADE_MATCHING_TOPIC = "sell-trade-matching";
    private static final String BUY_TRADE_MATCHING_TOPIC = "buy-trade-matching";
    private static final String UNMATCHED_ORDER_TOPIC = "unmatched-order-topic"; // 미체결 주문을 Order 모듈로 전송


    public void sendBuyTradeToExecution(BuyTradeMatchEvent event) {
        System.out.println("들어옵니다");
        try {
            buyTradeKafkaTemplate.send(BUY_TRADE_MATCHING_TOPIC, event).get();
            log.info("매수 체결 이벤트 Execution 모듈로 전송: {}", event);
        } catch (Exception e) {
            log.error("매수 체결 이벤트 전송 실패: {}", e.getMessage());
            throw new RuntimeException("Kafka 전송 실패로 롤백 발생");
        }
    }

    public void sendSellTradeToExecution(SellTradeMatchEvent event) {
        try {
            sellTradeKafkaTemplate.send(SELL_TRADE_MATCHING_TOPIC, event).get();
            log.info("매도 체결 이벤트 Execution 모듈로 전송: {}", event);
        } catch (Exception e) {
            log.error("매도 체결 이벤트 전송 실패: {}", e.getMessage());
            throw new RuntimeException("Kafka 전송 실패로 롤백 발생");
        }
    }

    public void sendUnmatchedOrderToOrderService(OrderCreateReqEvent orderEvent) {
        orderKafkaTemplate.send(UNMATCHED_ORDER_TOPIC, orderEvent);
        log.warn("5분 동안 매칭되지 않은 주문을 Order 모듈로 재전송: {}", orderEvent);
    }
}
