package finpago.dataservice.stock.config;

import finpago.dataservice.stock.service.StockSseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisListenerConfig {

    @Autowired
    private RedisMessageListenerContainer container;

    @Autowired
    private StockSseService stockSseService;

    @Bean
    public MessageListenerAdapter stockUpdatesListenerAdapter(StockSseService sseService) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(sseService, "onMessage");
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }

    @Bean
    public ChannelTopic stockTopic() {
        return new ChannelTopic("stock_updates");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerListeners() {
        container.addMessageListener(stockUpdatesListenerAdapter(stockSseService), stockTopic());
    }

}
