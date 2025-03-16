package finpago.matchingservice.matching.config;

import finpago.common.global.messaging.BuyTradeMatchEvent;
import finpago.common.global.messaging.OrderCreateReqEvent;
import finpago.common.global.messaging.SellTradeMatchEvent;
import finpago.common.global.messaging.TradeMatchingEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String BOOTSTRAP_SERVERS;
    private static final String GROUP_ID = "matching-service-group";

    // OrderCreateReqEvent ProducerFactory
    @Bean
    public ProducerFactory<String, OrderCreateReqEvent> orderProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); // 헤더 제거

        props.put(ProducerConfig.ACKS_CONFIG, "all"); // 모든 리플리카가 데이터 저장 확인 후 응답
        props.put(ProducerConfig.RETRIES_CONFIG, 10); // 최대 10번 재시도
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000); // 재시도 간격 (1초)
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 요청 타임아웃 (30초)
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10); // 배치 전송을 위해 대기 시간 설정 (10ms)

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, OrderCreateReqEvent> orderKafkaTemplate() {
        return new KafkaTemplate<>(orderProducerFactory());
    }

    // SellTradeMatchEvent ProducerFactory
    @Bean
    public ProducerFactory<String, SellTradeMatchEvent> sellTradeProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        props.put(ProducerConfig.ACKS_CONFIG, "all"); // 모든 리플리카가 데이터 저장 확인 후 응답
        props.put(ProducerConfig.RETRIES_CONFIG, 10); // 최대 10번 재시도
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000); // 재시도 간격 (1초)
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 요청 타임아웃 (30초)
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10); // 배치 전송을 위해 대기 시간 설정 (10ms)

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, SellTradeMatchEvent> sellTradeKafkaTemplate() {
        return new KafkaTemplate<>(sellTradeProducerFactory());
    }

    // BuyTradeMatchEvent ProducerFactory
    @Bean
    public ProducerFactory<String, BuyTradeMatchEvent> buyTradeProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        props.put(ProducerConfig.ACKS_CONFIG, "all"); // 모든 리플리카가 데이터 저장 확인 후 응답
        props.put(ProducerConfig.RETRIES_CONFIG, 10); // 최대 10번 재시도
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000); // 재시도 간격 (1초)
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 요청 타임아웃 (30초)
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10); // 배치 전송을 위해 대기 시간 설정 (10ms)

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, BuyTradeMatchEvent> buyTradeKafkaTemplate() {
        return new KafkaTemplate<>(buyTradeProducerFactory());
    }

    // ConsumerFactory (OrderCreateReqEvent 수신)
    @Bean
    public ConsumerFactory<String, OrderCreateReqEvent> orderConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "finpago.common.global.messaging.OrderCreateReqEvent");

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 수동 커밋 활성화
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // 장애 발생 후 가장 오래된 데이터부터 재처리
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000); // 재연결 시도 간격 (1초)
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10000); // 최대 재연결 시도 간격 (10초)
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 45000); // 세션 타임아웃 (기본값 10초 -> 45초)
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 15000); // 하트비트 주기 (기본값 3초 -> 15초)
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 최대 폴링 간격 (5분)
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100); // 한 번에 가져올 메시지 개수 조정

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreateReqEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreateReqEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderConsumerFactory());
        return factory;
    }
}
