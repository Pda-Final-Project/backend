package finpago.dataservice.stock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import finpago.dataservice.stock.service.StockSseService;

@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory, StockSseService sseService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 문자열 직렬화 설정
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(sseService, "onMessage");
        listenerAdapter.setSerializer(new StringRedisSerializer());

        container.addMessageListener(listenerAdapter, new ChannelTopic("stock_updates"));
        return container;
    }
}
