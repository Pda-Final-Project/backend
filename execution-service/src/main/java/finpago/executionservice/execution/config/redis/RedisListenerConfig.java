package finpago.executionservice.execution.config.redis;

import finpago.executionservice.execution.service.TradeSseService;
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
    private TradeSseService tradeSseService;

    @Bean
    public MessageListenerAdapter tradeListenerAdapter(TradeSseService service) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(service, "onMessage");
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }

    @Bean
    public ChannelTopic tradeTopic() {
        return new ChannelTopic("trade_updates");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerListeners() {
        container.addMessageListener(tradeListenerAdapter(tradeSseService), tradeTopic());
    }
}