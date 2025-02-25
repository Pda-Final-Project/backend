package finpago.settlementservice.settlement.config.kafka;

import finpago.common.global.messaging.TradeMatchingEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
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

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String GROUP_ID = "settlement-service-group";

    // TradeMatchingEvent ProducerFactory
    @Bean
    public ProducerFactory<String, TradeMatchingEvent> tradeProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, TradeMatchingEvent> tradeKafkaTemplate() {
        return new KafkaTemplate<>(tradeProducerFactory());
    }

    // ConsumerFactory: TradeMatchingEvent 지원
    @Bean
    public ConsumerFactory<String, TradeMatchingEvent> tradeConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "finpago.common.global.messaging.TradeMatchingEvent");

        return new DefaultKafkaConsumerFactory<>(props);
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeMatchingEvent> tradeKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TradeMatchingEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(tradeConsumerFactory());
        return factory;
    }
}
