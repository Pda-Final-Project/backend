package finpago.notificationservice.notification.config.kafka;

import finpago.common.global.messaging.*;
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

    private static final String BUY_DLT_TOPIC = "buy-settlement-dlt-topic";
    private static final String SELL_DLT_TOPIC = "sell-settlement-dlt-topic";
    private static final String FILLING_NOTICE_DLT_TOPIC = "filling-notice-dlt-topic";
    private static final long RETRY_INTERVAL = 10000L; // 재시도 간격 (10초)
    private static final int RETRY_COUNT = 1000; // ✅ 1000번 재시도

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

    // FillingNoticeEvent Retry Listener Factory
    @Bean(name = "fillingNoticeRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, FillingNoticeEvent> fillingNoticeRetryListenerContainerFactory(
            ConsumerFactory<String, FillingNoticeEvent> consumerFactory,
            KafkaTemplate<String, FillingNoticeEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, FillingNoticeEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
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

    private DefaultErrorHandler createErrorHandler(KafkaTemplate<?, ?> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception e) -> {
                    String topic;
                    if (record.topic().contains("buy")) {
                        topic = BUY_DLT_TOPIC;
                    } else if (record.topic().contains("sell")) {
                        topic = SELL_DLT_TOPIC;
                    } else {
                        topic = FILLING_NOTICE_DLT_TOPIC;
                    }
                    return new TopicPartition(topic, record.partition());
                }
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL, RETRY_COUNT));
    }
}
