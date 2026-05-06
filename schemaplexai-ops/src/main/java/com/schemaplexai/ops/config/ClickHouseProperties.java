package com.schemaplexai.ops.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "clickhouse")
public class ClickHouseProperties {

    private String host = "localhost";
    private int port = 8123;
    private String database = "schemaplexai";
    private String username = "default";
    private String password = "";
    private int socketTimeout = 30000;
    private int connectTimeout = 10000;
    private boolean compress = true;
}
