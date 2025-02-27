package finpago.matchingservice.matching.messaging.producer;

import finpago.common.global.messaging.OrderCreateReqEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingProducer {

    private final KafkaTemplate<String, TradeMatchingEvent> kafkaTemplate;
    private final KafkaTemplate<String, OrderCreateReqEvent> orderKafkaTemplate;

    private static final String TRADE_MATCHING_TOPIC = "trade-matching-topic";
    private static final String UNMATCHED_ORDER_TOPIC = "unmatched-order-topic"; // 미체결 주문을 Order 모듈로 전송

    public void sendTradeToExecution(TradeMatchingEvent tradeEvent) {
        kafkaTemplate.send(TRADE_MATCHING_TOPIC, tradeEvent);
        log.info("매칭된 주문을 Execution 모듈로 전송: {}", tradeEvent);
    }

    public void sendUnmatchedOrderToOrderService(OrderCreateReqEvent orderEvent) {
        orderKafkaTemplate.send(UNMATCHED_ORDER_TOPIC, orderEvent);
        log.warn("5분 동안 매칭되지 않은 주문을 Order 모듈로 재전송: {}", orderEvent);
    }
}
