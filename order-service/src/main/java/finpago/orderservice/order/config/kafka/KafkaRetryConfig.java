package finpago.orderservice.order.config.kafka;

import finpago.common.global.messaging.OrderCreateReqEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

@Configuration
public class KafkaRetryConfig {

    private static final String ORDER_DLT_TOPIC = "order-dlt-topic";
    private static final String UNMATCHED_ORDER_DLT_TOPIC = "unmatched-order-dlt-topic";//DLT 토픽
    private static final long RETRY_INTERVAL = 10000L; // 재시도 간격 (10초)
    private static final int RETRY_COUNT = 5; // ✅ 5번 재시도

    @Bean(name = "kafkaRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreateReqEvent> kafkaRetryListenerContainerFactory(
            ConsumerFactory<String, OrderCreateReqEvent> consumerFactory,
            KafkaTemplate<String, OrderCreateReqEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, OrderCreateReqEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // DLT로 이동하는 ErrorHandler 설정
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception e) -> {
                    String originalTopic = record.topic();
                    if ("unmatched-order-topic".equals(originalTopic)) {
                        return new TopicPartition(UNMATCHED_ORDER_DLT_TOPIC, record.partition());
                    } else {
                        return new TopicPartition(ORDER_DLT_TOPIC, record.partition());
                    }
                }
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL, RETRY_COUNT));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
