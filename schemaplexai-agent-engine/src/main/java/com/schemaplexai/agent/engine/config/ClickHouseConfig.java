package com.schemaplexai.agent.engine.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ClickHouse DataSource configuration for agent-engine timeline persistence.
 * Mirrors the ops module config but is scoped to agent-engine to avoid cross-module coupling.
 */
@Slf4j
@Configuration
public class ClickHouseConfig {

    @Bean
    @ConfigurationProperties(prefix = "clickhouse")
    public ClickHouseProperties clickHouseProperties() {
        return new ClickHouseProperties();
    }

    @Bean
    @ConditionalOnProperty(name = "clickhouse.enabled", havingValue = "true", matchIfMissing = false)
    public DataSource clickHouseDataSource(ClickHouseProperties props) {
        String url = String.format("jdbc:clickhouse://%s:%d/%s",
                props.getHost(), props.getPort(), props.getDatabase());

        Properties connProps = new Properties();
        connProps.setProperty("user", props.getUsername());
        connProps.setProperty("password", props.getPassword());
        connProps.setProperty("socket_timeout", String.valueOf(props.getSocketTimeout()));
        connProps.setProperty("connect_timeout", String.valueOf(props.getConnectTimeout()));
        connProps.setProperty("compress", String.valueOf(props.isCompress()));

        try {
            ClickHouseDataSource ds = new ClickHouseDataSource(url, connProps);
            log.info("AgentEngine ClickHouse DataSource initialized: {}", url);
            return ds;
        } catch (SQLException e) {
            log.error("Failed to create ClickHouse DataSource", e);
            throw new RuntimeException("Failed to create ClickHouse DataSource", e);
        }
    }
}
