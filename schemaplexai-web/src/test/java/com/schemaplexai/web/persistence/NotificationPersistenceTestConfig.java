package com.schemaplexai.web.persistence;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.schemaplexai.web.config.MyBatisPlusConfig;
import com.schemaplexai.web.service.notification.NotificationServiceImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Issue 926: minimal persistence context used to prove the production
 * {@link MyBatisPlusConfig} actually activates pagination and tenant-line
 * interception against a real (H2) database. It deliberately imports the real
 * web {@code MyBatisPlusConfig} and {@code NotificationServiceImpl} rather than
 * re-declaring them, so the test exercises the exact wiring shipped in main.
 */
@SpringBootConfiguration
@ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class
})
@MapperScan("com.schemaplexai.dao.mapper.notification")
@Import({MyBatisPlusConfig.class, NotificationServiceImpl.class})
class NotificationPersistenceTestConfig {
}
