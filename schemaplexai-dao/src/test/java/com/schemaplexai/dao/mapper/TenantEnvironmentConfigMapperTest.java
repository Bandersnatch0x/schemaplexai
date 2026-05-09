package com.schemaplexai.dao.mapper;

import com.schemaplexai.model.entity.config.TenantEnvironmentConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.schemaplexai.dao.TestApplication.class)
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
class TenantEnvironmentConfigMapperTest {

    @Autowired
    private TenantEnvironmentConfigMapper tenantEnvironmentConfigMapper;

    @Test
    void insert_selectById() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setTenantId("tenant-001");
        config.setEnvironment("dev");
        config.setAllowedTools("[\"tool1\"]");
        config.setSecurityLevel("LOW");
        config.setAllowHttpCalls(true);
        config.setAllowFileRead(false);
        config.setAllowIrreversibleOps(false);
        config.setMaxConcurrentToolCalls(3);
        config.setExtraConfig("{}");
        config.setDeleted(0);

        int rows = tenantEnvironmentConfigMapper.insert(config);
        assertThat(rows).isEqualTo(1);
        assertThat(config.getId()).isNotNull();

        TenantEnvironmentConfig found = tenantEnvironmentConfigMapper.selectById(config.getId());
        assertThat(found).isNotNull();
        assertThat(found.getTenantId()).isEqualTo("tenant-001");
        assertThat(found.getEnvironment()).isEqualTo("dev");
    }

    @Test
    void updateById() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setTenantId("tenant-002");
        config.setEnvironment("staging");
        config.setDeleted(0);
        tenantEnvironmentConfigMapper.insert(config);

        config.setEnvironment("prod");
        int rows = tenantEnvironmentConfigMapper.updateById(config);
        assertThat(rows).isEqualTo(1);

        TenantEnvironmentConfig found = tenantEnvironmentConfigMapper.selectById(config.getId());
        assertThat(found.getEnvironment()).isEqualTo("prod");
    }

    @Test
    void deleteById() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setTenantId("tenant-003");
        config.setEnvironment("dev");
        config.setDeleted(0);
        tenantEnvironmentConfigMapper.insert(config);

        int rows = tenantEnvironmentConfigMapper.deleteById(config.getId());
        assertThat(rows).isEqualTo(1);

        TenantEnvironmentConfig found = tenantEnvironmentConfigMapper.selectById(config.getId());
        assertThat(found).isNull();
    }

    @Test
    void selectCount() {
        long count = tenantEnvironmentConfigMapper.selectCount(null);
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
}
