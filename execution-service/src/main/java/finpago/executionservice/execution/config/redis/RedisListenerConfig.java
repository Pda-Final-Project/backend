package finpago.executionservice.execution.config.redis;

import finpago.executionservice.execution.service.TradeSseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisListenerConfig {

    @Bean
    public MessageListenerAdapter tradeUpdatesListenerAdapter(TradeSseService tradeSseService) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(tradeSseService, "onMessage");
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }

    @Bean
    public void configureTradeUpdatesListener(RedisMessageListenerContainer redisContainer, MessageListenerAdapter tradeUpdatesListenerAdapter) {
        redisContainer.addMessageListener(tradeUpdatesListenerAdapter, new ChannelTopic("trade_updates"));
    }
}