//package finpago.common.global.config;
//
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.annotation.EnableKafka;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.core.*;
//import org.springframework.kafka.support.serializer.JsonDeserializer;
//import org.springframework.kafka.support.serializer.JsonSerializer;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//@EnableKafka
//@ConditionalOnProperty(prefix = "common.kafka", name = "enabled", havingValue = "true")
//public class KafkaConfig {
//
//    // 환경별 Kafka 서버 주소를 상수로 관리
//    private static final String DEV_BOOTSTRAP_SERVERS = "localhost:9092";
//    private static final String PROD_BOOTSTRAP_SERVERS = "kafka.kafka.svc.cluster.local:9092";
//
//    // 서비스별 Kafka Group ID 관리 (Map 활용)
//    private static final Map<String, String> GROUP_IDS = new HashMap<>() {{
//        put("user-service", "user-service-group");
//        put("holding-service", "holding-service-group");
//        put("order-service", "order-service-group");
//        put("notification-service", "notification-service-group");
//        put("matching-service", "matching-service-group");
//        put("execution-service", "execution-service-group");
//    }};
//
//    /**
//     * 현재 환경이 prod인지 확인하는 메서드
//     */
//    private boolean isProdProfile() {
//        return "prod".equalsIgnoreCase(System.getProperty("spring.profiles.active", "dev"));
//    }
//
//    /**
//     * 환경에 맞는 Kafka 서버 주소 반환
//     */
//    private String getBootstrapServers() {
//        return isProdProfile() ? PROD_BOOTSTRAP_SERVERS : DEV_BOOTSTRAP_SERVERS;
//    }
//
//    /**
//     * Producer Factory (모든 이벤트 타입을 지원)
//     */
//    @Bean
//    public ProducerFactory<String, Object> producerFactory() {
//        Map<String, Object> props = new HashMap<>();
//        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
//        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
//        return new DefaultKafkaProducerFactory<>(props);
//    }
//
//    @Bean
//    public KafkaTemplate<String, Object> kafkaTemplate() {
//        return new KafkaTemplate<>(producerFactory());
//    }
//
//    /**
//     * 서비스별 Consumer Factory를 자동 생성
//     */
//    private ConsumerFactory<String, Object> createConsumerFactory(String groupId) {
//        Map<String, Object> props = new HashMap<>();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
//        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
//        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
//        return new DefaultKafkaConsumerFactory<>(props);
//    }
//
//    /**
//     * 서비스별 Kafka Listener Factory 자동 등록
//     */
//    @Bean
//    public Map<String, ConcurrentKafkaListenerContainerFactory<String, Object>> kafkaListenerContainerFactories() {
//        Map<String, ConcurrentKafkaListenerContainerFactory<String, Object>> factoryMap = new HashMap<>();
//
//        for (Map.Entry<String, String> entry : GROUP_IDS.entrySet()) {
//            ConcurrentKafkaListenerContainerFactory<String, Object> factory =
//                    new ConcurrentKafkaListenerContainerFactory<>();
//            factory.setConsumerFactory(createConsumerFactory(entry.getValue()));
//            factoryMap.put(entry.getKey(), factory);
//        }
//
//        return factoryMap;
//    }
//}
