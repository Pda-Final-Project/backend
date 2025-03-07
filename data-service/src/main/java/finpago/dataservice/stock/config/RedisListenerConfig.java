package finpago.dataservice.stock.config;

import finpago.dataservice.stock.service.StockSseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisListenerConfig {

    @Bean
    public MessageListenerAdapter stockUpdatesListenerAdapter(StockSseService sseService) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(sseService, "onMessage");
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }

    @Bean
    public RedisMessageListenerContainer configureStockUpdatesListener(
            RedisMessageListenerContainer redisContainer,
            MessageListenerAdapter stockUpdatesListenerAdapter) {

        redisContainer.addMessageListener(stockUpdatesListenerAdapter, new ChannelTopic("stock_updates"));
        return redisContainer;
    }

}
