package com.schemaplexai.system.entity;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live-defect regression (browser verification round 2): sf_tenant is the
 * tenant ROOT table — it has no tenant_id / created_by / updated_by columns,
 * but SfTenant extends BaseEntity which maps them. Before the fix the
 * startup tenant-cache backfill failed with
 * {@code column "tenant_id" does not exist} and the gateway rejected every
 * tenant. The entity must exclude those inherited columns.
 */
class SfTenantTableMappingTest {

    @Test
    void sfTenantExcludesRootTableAbsentColumns() {
        TableInfo info = TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SfTenant.class);

        List<String> columns = info.getFieldList().stream()
                .map(f -> f.getColumn())
                .toList();

        assertThat(columns)
                .as("sf_tenant DDL has no tenant_id/created_by/updated_by columns")
                .doesNotContain("tenant_id", "created_by", "updated_by");
        assertThat(columns).contains("name", "code", "status", "config_json");
    }
}
