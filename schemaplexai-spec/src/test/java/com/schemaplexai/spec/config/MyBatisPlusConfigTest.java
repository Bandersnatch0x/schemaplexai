package com.schemaplexai.spec.config;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.schemaplexai.dao.config.TenantLineInterceptor;
import com.schemaplexai.spec.entity.SfSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the optimistic-lock wiring (REQ-21): the interceptor must be
 * registered and the SfSpec.version column must carry @Version, otherwise
 * concurrent writes silently overwrite each other again.
 */
class MyBatisPlusConfigTest {

    @Test
    void interceptorChain_containsOptimisticLocker() {
        MyBatisPlusConfig config = new MyBatisPlusConfig(new TenantLineInterceptor());

        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();

        List<Object> inners = readInterceptors(interceptor);
        assertThat(inners)
                .as("OptimisticLockerInnerInterceptor must be registered")
                .anyMatch(OptimisticLockerInnerInterceptor.class::isInstance);
    }

    @Test
    void sfSpec_versionColumn_isMappedWithVersionAnnotation() throws Exception {
        Field versionField = SfSpec.class.getDeclaredField("version");

        assertThat(versionField.isAnnotationPresent(Version.class))
                .as("sf_spec.version must carry @Version for the optimistic lock")
                .isTrue();
        assertThat(versionField.getType()).isEqualTo(Integer.class);
    }

    @Test
    void sfSpec_mapsToSfSpecTable() {
        assertThat(SfSpec.class.getAnnotation(TableName.class).value()).isEqualTo("sf_spec");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> readInterceptors(MybatisPlusInterceptor interceptor) {
        try {
            Field field = MybatisPlusInterceptor.class.getDeclaredField("interceptors");
            field.setAccessible(true);
            return (List<Object>) field.get(interceptor);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not inspect MybatisPlusInterceptor", e);
        }
    }
}
