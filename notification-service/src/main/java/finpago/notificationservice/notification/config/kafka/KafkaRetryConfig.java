package finpago.notificationservice.notification.config.kafka;

import finpago.common.global.messaging.TradeMatchingEvent;
import finpago.common.global.messaging.NoticeEvent;
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

    private static final String TRADE_DLT_TOPIC = "trade-settlement-dlt-topic"; // TradeMatchingEvent DLT
    private static final String NOTICE_DLT_TOPIC = "notice-dlt-topic"; // NoticeEvent DLT
    private static final long RETRY_INTERVAL = 1000L; // 재시도 간격 (1초)
    private static final int RETRY_COUNT = 3; // 최대 재시도 횟수

    /**
     * 기본 리스너 팩토리 (기본적으로 @KafkaListener에서 사용됨)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaRetryListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception e) -> {
                    if (record.value() instanceof TradeMatchingEvent) {
                        return new TopicPartition(TRADE_DLT_TOPIC, record.partition());
                    } else if (record.value() instanceof NoticeEvent) {
                        return new TopicPartition(NOTICE_DLT_TOPIC, record.partition());
                    }
                    return new TopicPartition("generic-dlt-topic", record.partition()); // 예외 처리
                }
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL, RETRY_COUNT));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /**
     * TradeMatchingEvent 전용 리스너 팩토리
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeMatchingEvent> tradeRetryListenerContainerFactory(
            ConsumerFactory<String, TradeMatchingEvent> consumerFactory,
            KafkaTemplate<String, TradeMatchingEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, TradeMatchingEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception e) -> new TopicPartition(TRADE_DLT_TOPIC, record.partition())
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL, RETRY_COUNT));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /**
     * NoticeEvent 전용 리스너 팩토리
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NoticeEvent> noticeRetryListenerContainerFactory(
            ConsumerFactory<String, NoticeEvent> consumerFactory,
            KafkaTemplate<String, NoticeEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, NoticeEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception e) -> new TopicPartition(NOTICE_DLT_TOPIC, record.partition())
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL, RETRY_COUNT));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
