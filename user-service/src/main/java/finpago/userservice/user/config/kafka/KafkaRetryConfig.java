package finpago.userservice.user.config.kafka;

import finpago.common.global.messaging.NoticeEvent;
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

    private static final String DLT_TOPIC="default-dlt-topic";
    private static final String DLT_TOPIC1 = "notice-dlt-topic";
    private static final String DLT_TOPIC2= "settlement-dlt-topic";// DLT 토픽
    private static final long RETRY_INTERVAL = 1000L; // 재시도 간격 (1초)
    private static final int RETRY_COUNT = 3; // 최대 재시도 횟수

    @Bean(name = "noticeKafkaRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, NoticeEvent> noticeKafkaRetryListenerContainerFactory(
            ConsumerFactory<String, NoticeEvent> consumerFactory,
            KafkaTemplate<String, NoticeEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, NoticeEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // 실패한 메시지를 DLT(Dead Letter Topic)으로 이동하는 Recoverer 설정
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception e) -> new TopicPartition(DLT_TOPIC1, record.partition())
        );

        // 3번 재시도 후 DLT로 이동하는 ErrorHandler
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL, RETRY_COUNT));

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean(name = "tradeMatchingKafkaRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, TradeMatchingEvent> tradeMatchingKafkaRetryListenerContainerFactory(
            ConsumerFactory<String, TradeMatchingEvent> consumerFactory,
            KafkaTemplate<String, TradeMatchingEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, TradeMatchingEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // 실패한 메시지를 DLT(Dead Letter Topic)으로 이동하는 Recoverer 설정
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception e) -> new TopicPartition(DLT_TOPIC2, record.partition())
        );

        // 3번 재시도 후 DLT로 이동하는 ErrorHandler
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL, RETRY_COUNT));

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean(name = "kafkaRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaRetryListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // DLT 설정
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception e) ->
                        new TopicPartition(DLT_TOPIC, record.partition())
        );

        // 3번 재시도 후 DLT로 이동
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL, RETRY_COUNT));

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
