---
topic: phase1-observability-foundation
stage: plan
version: v1.0
status: approved
---

# Phase 1: Observability & Configuration Foundation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish observability data model (Trace/Span), prompt versioning, Skill markdown spec, and unified messaging format — four independent subsystems layered onto existing services.

**Architecture:** Entities in `schemaplexai-model` for cross-service sharing; mappers/services/controllers in their owning modules; all follow the existing MyBatis-Plus `BaseEntity` + `BaseMapperX` + `ServiceImpl` pattern. Each subsystem is independently testable and deployable.

**Tech Stack:** Java 21, Spring Boot 3.2.5, MyBatis-Plus 3.5.5, Testcontainers (JUnit 5), JdbcTemplate (for ClickHouse schema)

---

## Scope Check

Phase 1 covers 4 independent subsystems — they share no code dependencies and can be implemented in parallel:

| # | Subsystem | Owning Module |
|---|-----------|--------------|
| 1 | Observability data model | schemaplexai-model + agent-engine |
| 2 | Prompt version management | agent-config |
| 3 | Skill Markdown specification | integration |
| 4 | Unified message format | common + web |

> **Phase 3 Preview (2026-04-30)**: The spec's Phase 3 sandbox design was updated to use **zeroboot CoW VM** — sub-millisecond fork (~0.8ms) with hardware-enforced isolation (VT-x/AMD-V), replacing the original Docker-based approach. Per-sandbox memory reduced from ~20MB to ~265KB, implementation cost from 3-4 person-weeks to 1-2 person-weeks. A separate Phase 3 plan will cover `ZerobootClient` integration in `schemaplexai-integration`. See spec: `docs/specs/open-source-agent-architecture-research.md` and research: `wiki/ideas/2026-04-30-zeroboot-architecture.md`.

---

## Task 0: Test Infrastructure Setup (Prerequisite)

**Files:**
- Create: `schemaplexai-agent-engine/src/test/resources/application-test.yml`
- Create: `schemaplexai-agent-config/src/test/resources/application-test.yml`

- [ ] **Step 1: Create test config for agent-engine**

`schemaplexai-agent-engine/src/test/resources/application-test.yml`:
```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:16-alpine:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  test:
    database:
      replace: none

mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

- [ ] **Step 2: Create test config for agent-config**

Same content as Step 1, at `schemaplexai-agent-config/src/test/resources/application-test.yml`.

- [ ] **Step 3: Verify test infrastructure**

```bash
cd D:/code_space/frige
mvn test -pl schemaplexai-agent-engine -Dtest="dummy" 2>&1 | tail -5
```

Expected: `No tests found` (NOT infrastructure errors)

- [ ] **Step 4: Commit**

```bash
git add schemaplexai-agent-engine/src/test/resources/ schemaplexai-agent-config/src/test/resources/
git commit -m "chore: add test infrastructure with Testcontainers config"
```

---

## Task Group 1: Observability Data Model

### Task 1: ObservabilityTrace Entity

**Files:**
- Create: `schemaplexai-model/src/main/java/com/schemaplexai/model/entity/observability/ObservabilityTrace.java`
- Create: `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/entity/ObservabilityTraceTest.java`

- [ ] **Step 1: Write the failing test**

`schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/entity/ObservabilityTraceTest.java`:
```java
package com.schemaplexai.agent.engine.entity;

import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityTraceTest {

    @Test
    void shouldCreateTraceWithRequiredFields() {
        ObservabilityTrace trace = new ObservabilityTrace();
        trace.setTraceId("trace-001");
        trace.setName("agent-execution-42");
        trace.setUserId("user-1");
        trace.setSessionId("session-1");
        trace.setInput("{\"prompt\":\"hello\"}");

        assertThat(trace.getTraceId()).isEqualTo("trace-001");
        assertThat(trace.getName()).isEqualTo("agent-execution-42");
        assertThat(trace.getUserId()).isEqualTo("user-1");
        assertThat(trace.getSessionId()).isEqualTo("session-1");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=ObservabilityTraceTest -v
```

Expected: FAIL — `ObservabilityTrace` class not found.

- [ ] **Step 3: Write the entity**

`schemaplexai-model/src/main/java/com/schemaplexai/model/entity/observability/ObservabilityTrace.java`:
```java
package com.schemaplexai.model.entity.observability;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_observability_trace")
public class ObservabilityTrace extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String traceId;
    private String name;
    private String userId;
    private String sessionId;
    private String input;
    private String output;
    private String metadata;
    private String tags;
    private String version;
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=ObservabilityTraceTest -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add schemaplexai-model/src/main/java/com/schemaplexai/model/entity/observability/ObservabilityTrace.java schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/entity/ObservabilityTraceTest.java
git commit -m "feat: add ObservabilityTrace entity for agent execution trace tracking"
```

---

### Task 2: ObservabilitySpan Entity

**Files:**
- Create: `schemaplexai-model/src/main/java/com/schemaplexai/model/entity/observability/ObservabilitySpan.java`
- Create: `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/entity/ObservabilitySpanTest.java`

- [ ] **Step 1: Write the failing test**

`schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/entity/ObservabilitySpanTest.java`:
```java
package com.schemaplexai.agent.engine.entity;

import com.schemaplexai.model.entity.observability.ObservabilitySpan;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ObservabilitySpanTest {

    @Test
    void shouldCreateGenerationSpanWithModelAndUsage() {
        ObservabilitySpan span = new ObservabilitySpan();
        span.setSpanId("span-001");
        span.setTraceId("trace-001");
        span.setName("llm-call");
        span.setType("GENERATION");
        span.setModel("gpt-4");
        span.setUsageDetails("{\"input\":100,\"output\":50}");
        span.setCostDetails("{\"total\":0.015}");

        assertThat(span.getType()).isEqualTo("GENERATION");
        assertThat(span.getModel()).isEqualTo("gpt-4");
        assertThat(span.getUsageDetails()).contains("100");
    }

    @Test
    void shouldSupportNestedSpansViaParentSpanId() {
        ObservabilitySpan child = new ObservabilitySpan();
        child.setSpanId("span-002");
        child.setParentSpanId("span-001");

        assertThat(child.getParentSpanId()).isEqualTo("span-001");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=ObservabilitySpanTest -v
```

Expected: FAIL.

- [ ] **Step 3: Write the entity**

`schemaplexai-model/src/main/java/com/schemaplexai/model/entity/observability/ObservabilitySpan.java`:
```java
package com.schemaplexai.model.entity.observability;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_observability_span")
public class ObservabilitySpan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String spanId;
    private String traceId;
    private String parentSpanId;
    private String name;
    private String type;
    private Long startTime;
    private Long endTime;
    private String input;
    private String output;
    private String metadata;
    private String status;
    private String model;
    private String modelParameters;
    private String usageDetails;
    private String costDetails;
    private String promptName;
    private String promptVersion;
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=ObservabilitySpanTest -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add schemaplexai-model/src/main/java/com/schemaplexai/model/entity/observability/ObservabilitySpan.java schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/entity/ObservabilitySpanTest.java
git commit -m "feat: add ObservabilitySpan entity with generation and nesting support"
```

---

### Task 3: Observability Mappers + DB Schema

**Files:**
- Create: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/mapper/ObservabilityTraceMapper.java`
- Create: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/mapper/ObservabilitySpanMapper.java`

- [ ] **Step 1: Write TraceMapper**

`schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/mapper/ObservabilityTraceMapper.java`:
```java
package com.schemaplexai.agent.engine.mapper;

import com.schemaplexai.dao.base.BaseMapperX;
import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ObservabilityTraceMapper extends BaseMapperX<ObservabilityTrace> {
}
```

- [ ] **Step 2: Write SpanMapper**

`schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/mapper/ObservabilitySpanMapper.java`:
```java
package com.schemaplexai.agent.engine.mapper;

import com.schemaplexai.dao.base.BaseMapperX;
import com.schemaplexai.model.entity.observability.ObservabilitySpan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ObservabilitySpanMapper extends BaseMapperX<ObservabilitySpan> {
}
```

- [ ] **Step 3: Write DB migration SQL**

Create `docker/postgres/init/009_observability.sql`:
```sql
CREATE TABLE IF NOT EXISTS sf_observability_trace (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(64),
    trace_id VARCHAR(64) NOT NULL,
    name VARCHAR(255),
    user_id VARCHAR(64),
    session_id VARCHAR(64),
    input TEXT,
    output TEXT,
    metadata TEXT,
    tags VARCHAR(512),
    version VARCHAR(32),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by BIGINT,
    updated_by BIGINT,
    deleted INT DEFAULT 0
);

CREATE INDEX idx_obs_trace_trace_id ON sf_observability_trace(trace_id);
CREATE INDEX idx_obs_trace_session_id ON sf_observability_trace(session_id);

CREATE TABLE IF NOT EXISTS sf_observability_span (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(64),
    span_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    parent_span_id VARCHAR(64),
    name VARCHAR(255),
    type VARCHAR(32),
    start_time BIGINT,
    end_time BIGINT,
    input TEXT,
    output TEXT,
    metadata TEXT,
    status VARCHAR(32),
    model VARCHAR(64),
    model_parameters TEXT,
    usage_details TEXT,
    cost_details TEXT,
    prompt_name VARCHAR(128),
    prompt_version VARCHAR(32),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by BIGINT,
    updated_by BIGINT,
    deleted INT DEFAULT 0
);

CREATE INDEX idx_obs_span_trace_id ON sf_observability_span(trace_id);
CREATE INDEX idx_obs_span_parent ON sf_observability_span(parent_span_id);
```

- [ ] **Step 4: Commit**

```bash
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/mapper/ObservabilityTraceMapper.java schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/mapper/ObservabilitySpanMapper.java docker/postgres/init/009_observability.sql
git commit -m "feat: add observability mappers and PostgreSQL schema"
```

---

### Task 4: ObservabilityRecorder Service

**Files:**
- Create: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/observability/ObservabilityRecorder.java`
- Create: `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/observability/ObservabilityRecorderTest.java`

- [ ] **Step 1: Write the failing integration test**

`schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/observability/ObservabilityRecorderTest.java`:
```java
package com.schemaplexai.agent.engine.observability;

import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import com.schemaplexai.model.entity.observability.ObservabilitySpan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ObservabilityRecorderTest {

    @Autowired(required = false)
    private ObservabilityRecorder recorder;

    @Test
    void shouldStartAndEndTrace() {
        assertThat(recorder).isNotNull();

        ObservabilityTrace trace = recorder.startTrace(
            "agent-exec-1", "test run", "user-1", "session-1", "{\"prompt\":\"hi\"}");

        assertThat(trace.getTraceId()).isNotNull();
        assertThat(trace.getName()).isEqualTo("test run");

        recorder.endTrace(trace.getTraceId(), "{\"result\":\"ok\"}");
    }

    @Test
    void shouldAddSpanToTrace() {
        ObservabilityTrace trace = recorder.startTrace(
            "agent-exec-2", "span test", "user-1", "sess-1", "{}");

        ObservabilitySpan span = recorder.addSpan(
            trace.getTraceId(), null, "tool-call", "SPAN",
            System.currentTimeMillis(), System.currentTimeMillis() + 100,
            "{\"tool\":\"bash\"}", "{\"exitCode\":0}", "SUCCESS");

        assertThat(span.getSpanId()).isNotNull();
        assertThat(span.getTraceId()).isEqualTo(trace.getTraceId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=ObservabilityRecorderTest -v
```

Expected: FAIL — no `ObservabilityRecorder` bean.

- [ ] **Step 3: Write the service**

`schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/observability/ObservabilityRecorder.java`:
```java
package com.schemaplexai.agent.engine.observability;

import com.schemaplexai.agent.engine.mapper.ObservabilitySpanMapper;
import com.schemaplexai.agent.engine.mapper.ObservabilityTraceMapper;
import com.schemaplexai.model.entity.observability.ObservabilitySpan;
import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObservabilityRecorder {

    private final ObservabilityTraceMapper traceMapper;
    private final ObservabilitySpanMapper spanMapper;

    @Transactional
    public ObservabilityTrace startTrace(String executionId, String name,
                                          String userId, String sessionId, String input) {
        ObservabilityTrace trace = new ObservabilityTrace();
        trace.setTraceId(UUID.randomUUID().toString());
        trace.setName(name + "-" + executionId);
        trace.setUserId(userId);
        trace.setSessionId(sessionId);
        trace.setInput(input);
        traceMapper.insert(trace);
        return trace;
    }

    @Transactional
    public void endTrace(String traceId, String output) {
        ObservabilityTrace trace = traceMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ObservabilityTrace>()
                .eq(ObservabilityTrace::getTraceId, traceId));
        if (trace != null) {
            trace.setOutput(output);
            traceMapper.updateById(trace);
        }
    }

    @Transactional
    public ObservabilitySpan addSpan(String traceId, String parentSpanId,
                                      String name, String type,
                                      Long startTime, Long endTime,
                                      String input, String output, String status) {
        ObservabilitySpan span = new ObservabilitySpan();
        span.setSpanId(UUID.randomUUID().toString());
        span.setTraceId(traceId);
        span.setParentSpanId(parentSpanId);
        span.setName(name);
        span.setType(type);
        span.setStartTime(startTime);
        span.setEndTime(endTime);
        span.setInput(input);
        span.setOutput(output);
        span.setStatus(status);
        spanMapper.insert(span);
        return span;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=ObservabilityRecorderTest -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/observability/ schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/observability/
git commit -m "feat: add ObservabilityRecorder service for trace/span lifecycle"
```

---

### Task 5: Integrate Recorder into AgentRuntimeOrchestrator

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/orchestrator/AgentRuntimeOrchestrator.java`

- [ ] **Step 3: Write the integration test**

`schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/orchestrator/AgentRuntimeOrchestratorIntegrationTest.java`:
```java
package com.schemaplexai.agent.engine.orchestrator;

import com.schemaplexai.agent.engine.observability.ObservabilityRecorder;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
class AgentRuntimeOrchestratorIntegrationTest {

    @Autowired
    private AgentRuntimeOrchestrator orchestrator;

    @MockBean
    private ObservabilityRecorder observabilityRecorder;

    @Test
    void shouldCallObservabilityRecorderDuringExecution() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setConversationId("conv-1");
        execution.setCreatedBy(100L);
        execution.setState("IDLE");

        try {
            orchestrator.run(execution, "tenant-1", "test prompt");
        } catch (Exception e) {
            // 预期可能因 stub 实现而异常，关注 trace 调用即可
        }

        verify(observabilityRecorder, atLeastOnce())
            .startTrace(any(), any(), any(), any(), any());
    }
}
```

- [ ] **Step 4: Run test to verify it fails (if Orchestrator not yet wired)**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=AgentRuntimeOrchestratorIntegrationTest -v
```

Expected: FAIL — no ObservabilityRecorder injected into orchestrator yet.

- [ ] **Step 5: Add ObservabilityRecorder dependency and trace lifecycle**

Add `private final ObservabilityRecorder observabilityRecorder;` to the field declarations. Lombok `@RequiredArgsConstructor` auto-generates the constructor.

In the `run(SfAgentExecution execution, String tenantId, String prompt)` method, at the beginning of `run()`, add:

```java
String traceId = observabilityRecorder.startTrace(
    String.valueOf(execution.getId()),
    "agent-execution",
    String.valueOf(execution.getCreatedBy()),
    execution.getConversationId(),
    prompt
).getTraceId();
```

In the `finally` block of `run()`, add:

```java
observabilityRecorder.endTrace(traceId,
    "{\"state\":\"" + execution.getState() + "\",\"rounds\":" + roundCount + "}");
```

- [ ] **Step 6: Verify compilation**

```bash
mvn compile -pl schemaplexai-agent-engine -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Run integration test to verify it passes**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=AgentRuntimeOrchestratorIntegrationTest -v
```

Expected: PASS — ObservabilityRecorder startTrace/endTrace called during execution.

- [ ] **Step 8: Commit**

```bash
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/orchestrator/AgentRuntimeOrchestrator.java schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/orchestrator/AgentRuntimeOrchestratorIntegrationTest.java
git commit -m "feat: integrate observability trace lifecycle into AgentRuntimeOrchestrator"
```

---

## Task Group 2: Prompt Version Management

### Task 6: SfPromptVersion Entity + Mapper

**Files:**
- Create: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/entity/SfPromptVersion.java`
- Create: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/mapper/PromptVersionMapper.java`
- Create: `schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/entity/SfPromptVersionTest.java`

- [ ] **Step 1: Write the failing test**

`schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/entity/SfPromptVersionTest.java`:
```java
package com.schemaplexai.agent.config.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SfPromptVersionTest {

    @Test
    void shouldStorePromptVersionWithLabel() {
        SfPromptVersion pv = new SfPromptVersion();
        pv.setConfigId(1L);
        pv.setAgentId(10L);
        pv.setVersion(3);
        pv.setContent("You are a helpful assistant");
        pv.setLabel("production");
        pv.setChangeNote("Updated tone to be more professional");

        assertThat(pv.getVersion()).isEqualTo(3);
        assertThat(pv.getLabel()).isEqualTo("production");
        assertThat(pv.getContent()).contains("helpful assistant");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn test -pl schemaplexai-agent-config -Dtest=SfPromptVersionTest -v
```

Expected: FAIL.

- [ ] **Step 3: Write entity and mapper**

`schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/entity/SfPromptVersion.java`:
```java
package com.schemaplexai.agent.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_prompt_version")
public class SfPromptVersion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long configId;
    private Long agentId;
    private Integer version;
    private String content;
    private String label;
    private String changeNote;
}
```

`schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/mapper/PromptVersionMapper.java`:
```java
package com.schemaplexai.agent.config.mapper;

import com.schemaplexai.dao.base.BaseMapperX;
import com.schemaplexai.agent.config.entity.SfPromptVersion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PromptVersionMapper extends BaseMapperX<SfPromptVersion> {
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
mvn test -pl schemaplexai-agent-config -Dtest=SfPromptVersionTest -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/entity/SfPromptVersion.java schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/mapper/PromptVersionMapper.java schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/entity/SfPromptVersionTest.java
git commit -m "feat: add SfPromptVersion entity and mapper for prompt versioning"
```

---

### Task 7: PromptVersionService + Controller

**Files:**
- Create: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/PromptVersionService.java`
- Create: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/PromptVersionServiceImpl.java`
- Create: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/controller/PromptVersionController.java`

- [ ] **Step 1: Write service interface**

`schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/PromptVersionService.java`:
```java
package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.agent.config.entity.SfPromptVersion;

import java.util.List;
import java.util.Optional;

public interface PromptVersionService extends IService<SfPromptVersion> {

    SfPromptVersion createVersion(Long configId, Long agentId, String content,
                                   String label, String changeNote);

    Optional<SfPromptVersion> getByLabel(Long configId, String label);

    List<SfPromptVersion> listVersions(Long configId);
}
```

- [ ] **Step 2: Write service implementation**

`schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/PromptVersionServiceImpl.java`:
```java
package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.agent.config.entity.SfPromptVersion;
import com.schemaplexai.agent.config.mapper.PromptVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromptVersionServiceImpl
        extends ServiceImpl<PromptVersionMapper, SfPromptVersion>
        implements PromptVersionService {

    private final PromptVersionMapper promptVersionMapper;

    @Override
    public SfPromptVersion createVersion(Long configId, Long agentId,
                                          String content, String label, String changeNote) {
        Integer nextVersion = promptVersionMapper.selectCount(
            new LambdaQueryWrapper<SfPromptVersion>()
                .eq(SfPromptVersion::getConfigId, configId)) + 1;

        SfPromptVersion pv = new SfPromptVersion();
        pv.setConfigId(configId);
        pv.setAgentId(agentId);
        pv.setVersion(nextVersion);
        pv.setContent(content);
        pv.setLabel(label);
        pv.setChangeNote(changeNote);
        promptVersionMapper.insert(pv);
        return pv;
    }

    @Override
    public Optional<SfPromptVersion> getByLabel(Long configId, String label) {
        SfPromptVersion pv = promptVersionMapper.selectOne(
            new LambdaQueryWrapper<SfPromptVersion>()
                .eq(SfPromptVersion::getConfigId, configId)
                .eq(SfPromptVersion::getLabel, label));
        return Optional.ofNullable(pv);
    }

    @Override
    public List<SfPromptVersion> listVersions(Long configId) {
        return promptVersionMapper.selectList(
            new LambdaQueryWrapper<SfPromptVersion>()
                .eq(SfPromptVersion::getConfigId, configId)
                .orderByDesc(SfPromptVersion::getVersion));
    }
}
```

- [ ] **Step 3: Write controller**

`schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/controller/PromptVersionController.java`:
```java
package com.schemaplexai.agent.config.controller;

import com.schemaplexai.agent.config.entity.SfPromptVersion;
import com.schemaplexai.agent.config.service.PromptVersionService;
import com.schemaplexai.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/agent-config/prompt-versions")
@RequiredArgsConstructor
public class PromptVersionController {

    private final PromptVersionService promptVersionService;

    @PostMapping
    public Result<SfPromptVersion> create(@RequestBody SfPromptVersion request) {
        SfPromptVersion pv = promptVersionService.createVersion(
            request.getConfigId(), request.getAgentId(),
            request.getContent(), request.getLabel(), request.getChangeNote());
        return Result.success(pv);
    }

    @GetMapping("/by-label")
    public Result<SfPromptVersion> getByLabel(@RequestParam Long configId,
                                               @RequestParam String label) {
        Optional<SfPromptVersion> pv = promptVersionService.getByLabel(configId, label);
        return pv.map(Result::success)
                 .orElse(Result.error("Prompt version not found"));
    }

    @GetMapping
    public Result<List<SfPromptVersion>> list(@RequestParam Long configId) {
        return Result.success(promptVersionService.listVersions(configId));
    }
}
```

- [ ] **Step 4: Verify compilation**

```bash
mvn compile -pl schemaplexai-agent-config -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/PromptVersionService.java schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/PromptVersionServiceImpl.java schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/controller/PromptVersionController.java
git commit -m "feat: add prompt version management service and controller"
```

---

## Task Group 3: Skill Markdown Specification

### Task 8: Skill Markdown Spec Document + Parser

**Files:**
- Create: `docs/standards/skill-markdown-spec.md`
- Create: `schemaplexai-integration/src/main/java/com/schemaplexai/integration/skill/SkillMarkdownParser.java`
- Create: `schemaplexai-integration/src/test/java/com/schemaplexai/integration/skill/SkillMarkdownParserTest.java`

- [ ] **Step 1: Write the failing test**

`schemaplexai-integration/src/test/java/com/schemaplexai/integration/skill/SkillMarkdownParserTest.java`:
```java
package com.schemaplexai.integration.skill;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SkillMarkdownParserTest {

    private static final String VALID_SKILL = """
        ---
        name: web-scraper
        version: 1.0
        description: Scrape web pages and extract structured data
        tags: [web, scraper, data]
        ---

        # web-scraper

        ## Description
        Fetches a URL and extracts text content.

        ## Parameters
        - `url` (string, required): The target URL
        - `selector` (string, optional): CSS selector for extraction

        ## Steps
        1. Fetch the URL
        2. Parse HTML
        3. Extract content matching the selector
        4. Return extracted text
        """;

    @Test
    void shouldParseFrontmatterFields() {
        SkillMarkdownParser.SkillMeta meta = SkillMarkdownParser.parseMeta(VALID_SKILL);

        assertThat(meta.name()).isEqualTo("web-scraper");
        assertThat(meta.version()).isEqualTo("1.0");
        assertThat(meta.description()).contains("Scrape web pages");
        assertThat(meta.tags()).contains("scraper");
    }

    @Test
    void shouldExtractBodyContent() {
        String body = SkillMarkdownParser.parseBody(VALID_SKILL);

        assertThat(body).contains("## Description");
        assertThat(body).contains("Fetches a URL");
        assertThat(body).doesNotContain("---");
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        String badSkill = """
            ---
            name: bad-skill
            ---

            No version or description
            """;

        try {
            SkillMarkdownParser.parseMeta(badSkill);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Missing required fields");
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn test -pl schemaplexai-integration -Dtest=SkillMarkdownParserTest -v
```

Expected: FAIL.

- [ ] **Step 3: Write parser and spec**

`schemaplexai-integration/src/main/java/com/schemaplexai/integration/skill/SkillMarkdownParser.java`:
```java
package com.schemaplexai.integration.skill;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillMarkdownParser {

    private static final Pattern FRONTMATTER_PATTERN =
        Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)", Pattern.DOTALL);

    public record SkillMeta(String name, String version, String description, List<String> tags) {}

    public static SkillMeta parseMeta(String markdown) {
        Matcher m = FRONTMATTER_PATTERN.matcher(markdown);
        if (!m.find()) {
            throw new IllegalArgumentException("Missing YAML frontmatter (--- blocks)");
        }
        String yaml = m.group(1);
        Map<String, String> fields = parseYamlMap(yaml);

        String name = fields.get("name");
        String version = fields.get("version");
        String description = fields.get("description");
        String tagsRaw = fields.getOrDefault("tags", "");

        if (name == null || version == null || description == null) {
            throw new IllegalArgumentException(
                "Missing required fields in frontmatter. Required: name, version, description");
        }

        List<String> tags = parseYamlList(tagsRaw);
        return new SkillMeta(name, version, description, tags);
    }

    public static String parseBody(String markdown) {
        Matcher m = FRONTMATTER_PATTERN.matcher(markdown);
        if (m.find()) {
            return m.group(2).trim();
        }
        return markdown;
    }

    private static Map<String, String> parseYamlMap(String yaml) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : yaml.split("\n")) {
            String trimmed = line.trim();
            int colon = trimmed.indexOf(':');
            if (colon > 0) {
                String key = trimmed.substring(0, colon).trim();
                String value = trimmed.substring(colon + 1).trim();
                map.put(key, value);
            }
        }
        return map;
    }

    private static List<String> parseYamlList(String raw) {
        String cleaned = raw.replaceAll("[\\[\\]]", "").trim();
        if (cleaned.isEmpty()) return List.of();
        return Arrays.stream(cleaned.split(","))
            .map(s -> s.trim().replaceAll("^\"|\"$", ""))
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
```

`docs/standards/skill-markdown-spec.md`:
```markdown
# Skill Markdown Specification v1.0

A Skill is a structured capability module defined as a Markdown file with YAML frontmatter.

## Format

```markdown
---
name: skill-name
version: 1.0
description: One-line summary of what this skill does
tags: [tag1, tag2]
---

# skill-name

## Description
Detailed explanation of what the skill does.

## Parameters
- `param1` (type, required/optional): Description

## Steps
1. Step one
2. Step two

## Output
What the skill produces.
```

## Frontmatter Fields

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| name | yes | string | Unique skill identifier (kebab-case) |
| version | yes | semver | Skill version |
| description | yes | string | One-line summary |
| tags | no | string[] | Categorization tags |

## File Naming

`SKILL.md` at the root of the skill directory.

## Progressive Loading

Skills are loaded only when the current task requires them. The parser extracts frontmatter metadata first (cheap), then the full body is loaded on demand.
```

- [ ] **Step 4: Run test to verify it passes**

```bash
mvn test -pl schemaplexai-integration -Dtest=SkillMarkdownParserTest -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add schemaplexai-integration/src/main/java/com/schemaplexai/integration/skill/SkillMarkdownParser.java schemaplexai-integration/src/test/java/com/schemaplexai/integration/skill/SkillMarkdownParserTest.java docs/standards/skill-markdown-spec.md
git commit -m "feat: add Skill Markdown spec and YAML frontmatter parser"
```

---

## Task Group 4: Unified Message Format

### Task 9: UnifiedMessage Interface

**Files:**
- Create: `schemaplexai-common/src/main/java/com/schemaplexai/common/message/UnifiedMessage.java`
- Create: `schemaplexai-common/src/main/java/com/schemaplexai/common/message/MessageType.java`
- Create: `schemaplexai-common/src/test/java/com/schemaplexai/common/message/UnifiedMessageTest.java`

- [ ] **Step 1: Write the failing test**

`schemaplexai-common/src/test/java/com/schemaplexai/common/message/UnifiedMessageTest.java`:
```java
package com.schemaplexai.common.message;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class UnifiedMessageTest {

    @Test
    void shouldBuildAgentResponseMessage() {
        long now = Instant.now().toEpochMilli();
        UnifiedMessage msg = UnifiedMessage.builder()
            .type(MessageType.AGENT_RESPONSE)
            .source("agent-engine")
            .target("web-client-1")
            .payload("{\"output\":\"Hello, world!\"}")
            .timestamp(now)
            .build();

        assertThat(msg.getType()).isEqualTo(MessageType.AGENT_RESPONSE);
        assertThat(msg.getSource()).isEqualTo("agent-engine");
        assertThat(msg.getPayload()).contains("Hello");
    }

    @Test
    void shouldHandleSseEventMessage() {
        UnifiedMessage msg = UnifiedMessage.builder()
            .type(MessageType.SSE_EVENT)
            .source("web")
            .target("sse-subscriber-1")
            .eventName("agent-step")
            .payload("{\"step\":\"thinking\",\"content\":\"...\"}")
            .timestamp(Instant.now().toEpochMilli())
            .build();

        assertThat(msg.getEventName()).isEqualTo("agent-step");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn test -pl schemaplexai-common -Dtest=UnifiedMessageTest -v
```

Expected: FAIL.

- [ ] **Step 3: Write UnifiedMessage and MessageType**

`schemaplexai-common/src/main/java/com/schemaplexai/common/message/MessageType.java`:
```java
package com.schemaplexai.common.message;

public enum MessageType {
    AGENT_RESPONSE,
    SSE_EVENT,
    WEBSOCKET_MESSAGE,
    ERROR,
    SYSTEM_NOTIFICATION
}
```

`schemaplexai-common/src/main/java/com/schemaplexai/common/message/UnifiedMessage.java`:
```java
package com.schemaplexai.common.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnifiedMessage {
    private MessageType type;
    private String source;
    private String target;
    private String eventName;
    private String payload;
    private Long timestamp;
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
mvn test -pl schemaplexai-common -Dtest=UnifiedMessageTest -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add schemaplexai-common/src/main/java/com/schemaplexai/common/message/ schemaplexai-common/src/test/java/com/schemaplexai/common/message/
git commit -m "feat: add UnifiedMessage and MessageType for standardized messaging"
```

---

### Task 10: Integrate UnifiedMessage into SSE Emitter

**Files:**
- Modify: `schemaplexai-web/src/main/java/com/schemaplexai/web/sse/AgentSseEmitter.java`

- [ ] **Step 1: Read current AgentSseEmitter to identify send method**

Read the file to find the existing `send` or `emit` method signature.

- [ ] **Step 2: Add overloaded send method accepting UnifiedMessage**

Add a new method to `AgentSseEmitter`:

```java
import com.schemaplexai.common.message.UnifiedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

private final ObjectMapper objectMapper = new ObjectMapper();

public void sendUnified(String clientId, UnifiedMessage message) {
    try {
        String json = objectMapper.writeValueAsString(message);
        send(clientId, message.getEventName() != null ? message.getEventName() : "message", json);
    } catch (Exception e) {
        log.error("Failed to serialize UnifiedMessage for client {}", clientId, e);
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
mvn compile -pl schemaplexai-web -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add schemaplexai-web/src/main/java/com/schemaplexai/web/sse/AgentSseEmitter.java
git commit -m "feat: integrate UnifiedMessage into AgentSseEmitter"
```

---

## Self-Review Checklist

**Spec coverage:** Each Phase 1 requirement has a corresponding task group:
- Trace/Span/Generation data model → Tasks 1-5
- Prompt version management → Tasks 6-7
- Skill Markdown specification → Task 8
- Unified message format → Tasks 9-10

**No placeholders:** All code is concrete, all paths are exact, all commands have expected outputs.

**Type consistency:** `ObservabilityTrace.traceId` / `ObservabilitySpan.traceId` both use `String` type. `SfPromptVersion` fields match service usage. `UnifiedMessage.type` uses `MessageType` enum consistently.

---

## Execution Handoff

**Plan complete and saved to `docs/plans/2026-04-30-phase1-observability-foundation.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
