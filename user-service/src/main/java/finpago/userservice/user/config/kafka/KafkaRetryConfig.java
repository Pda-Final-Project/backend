package finpago.userservice.user.config.kafka;

import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.NoticeEvent;
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

    private static final String NOTICE_DLT_TOPIC = "notice-dlt-topic";
    private static final String BUY_TRADE_DLT_TOPIC = "buy-trade-dlt-topic";
    private static final String SELL_TRADE_DLT_TOPIC = "sell-trade-dlt-topic";
    private static final String DEFAULT_DLT_TOPIC = "default-dlt-topic";
    private static final long RETRY_INTERVAL = 1000L; // 재시도 간격 (1초)
    private static final int RETRY_COUNT = 3; // 최대 재시도 횟수

    @Bean(name = "noticeKafkaRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, NoticeEvent> noticeKafkaRetryListenerContainerFactory(
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

    @Bean(name = "buyTradeRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, BuyTradeMatchEvent> buyTradeKafkaRetryListenerContainerFactory(
            ConsumerFactory<String, BuyTradeMatchEvent> consumerFactory,
            KafkaTemplate<String, BuyTradeMatchEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, BuyTradeMatchEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception e) -> new TopicPartition(BUY_TRADE_DLT_TOPIC, record.partition())
        );
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
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception e) -> new TopicPartition(DEFAULT_DLT_TOPIC, record.partition())
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL, RETRY_COUNT));
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }


    // SellTradeMatchEvent Retry Listener Factory
    @Bean(name = "sellTradeRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, SellTradeMatchEvent> sellTradeRetryListenerContainerFactory(
            ConsumerFactory<String, SellTradeMatchEvent> consumerFactory,
            KafkaTemplate<String, SellTradeMatchEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, SellTradeMatchEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception e) -> new TopicPartition(SELL_TRADE_DLT_TOPIC, record.partition())
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL, RETRY_COUNT));
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }


}
