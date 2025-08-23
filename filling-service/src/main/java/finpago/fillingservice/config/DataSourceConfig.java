package finpago.fillingservice.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {
    @Bean
    @ConfigurationProperties("spring.datasource.write")
    public DataSource writeDs() { return DataSourceBuilder.create().build(); }

    @Bean @ConfigurationProperties("spring.datasource.read")
    public DataSource readDs() { return DataSourceBuilder.create().build(); }

    @Primary
    @Bean
    public DataSource dataSource(@Qualifier("writeDs") DataSource write,
                                 @Qualifier("readDs") DataSource read) {
        Map<Object,Object> map = new HashMap<>();
        map.put("WRITE", write);
        map.put("READ",  read);

        AbstractRoutingDataSource routing = new AbstractRoutingDataSource() {
            @Override protected Object determineCurrentLookupKey() {
                return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? "READ" : "WRITE";
            }
        };
        routing.setTargetDataSources(map);
        routing.setDefaultTargetDataSource(write);
        return new com.zaxxer.hikari.HikariDataSource(new com.zaxxer.hikari.HikariConfig() {{ setDataSource(routing); }});
    }
}
