package finpago.orderservice.order.config;

import finpago.orderservice.order.messaging.events.OrderCreateReqEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaRetryConfig {

    private static final String DLT_TOPIC = "order-dlt-topic";

    @Bean(name = "kafkaRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreateReqEvent> kafkaRetryListenerContainerFactory(
            ConsumerFactory<String, OrderCreateReqEvent> consumerFactory,
            KafkaTemplate<String, OrderCreateReqEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, OrderCreateReqEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // DLT 설정: 3번 재시도 후 Dead Letter Topic으로 메시지 전송
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), // 실패한 메시지를 DLT로 전송
                new FixedBackOff(1000L, 3) // 1초 간격으로 3번 재시도 후 DLT로 이동
        ));

        return factory;
    }
}
