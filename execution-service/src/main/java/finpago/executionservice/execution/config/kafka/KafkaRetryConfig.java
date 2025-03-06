package finpago.executionservice.execution.config.kafka;

import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
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

    private static final String DLT_TOPIC = "execution-dlt-topic"; // DLT 토픽
    private static final long RETRY_INTERVAL = 1000L; // 재시도 간격 (1초)
    private static final int RETRY_COUNT = 3;
    // 최대 재시도 횟수
    // Default Retry Listener Factory (Generic)
    @Bean(name = "kafkaRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaRetryListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(createErrorHandler(kafkaTemplate));

        return factory;
    }

    // SellTradeMatchEvent Retry Listener Factory
    @Bean(name = "sellTradeRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, SellTradeMatchEvent> sellTradeRetryListenerContainerFactory(
            ConsumerFactory<String, SellTradeMatchEvent> consumerFactory,
            KafkaTemplate<String, SellTradeMatchEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, SellTradeMatchEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(createErrorHandler(kafkaTemplate));

        return factory;
    }

    // BuyTradeMatchEvent Retry Listener Factory
    @Bean(name = "buyTradeRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, BuyTradeMatchEvent> buyTradeRetryListenerContainerFactory(
            ConsumerFactory<String, BuyTradeMatchEvent> consumerFactory,
            KafkaTemplate<String, BuyTradeMatchEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, BuyTradeMatchEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(createErrorHandler(kafkaTemplate));

        return factory;
    }

    private DefaultErrorHandler createErrorHandler(KafkaTemplate<?, ?> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception e) ->
                        new TopicPartition(DLT_TOPIC, record.partition())
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL, RETRY_COUNT));
    }
}
