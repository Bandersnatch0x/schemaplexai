package com.schemaplexai.dao;

import com.schemaplexai.dao.config.MyBatisPlusTestConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.dao"})
@MapperScan("com.schemaplexai.dao.**.mapper")
@Import(MyBatisPlusTestConfig.class)
public class TestApplication {
}
