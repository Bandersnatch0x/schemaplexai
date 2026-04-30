---
topic: tech-research
stage: plan
version: v1.0
status: 已批准
supersedes: ""
---

# 技术预研计划

> **主题**: `tech-research`
> **阶段**: `plan`
> **版本**: v1.0
> **状态**: 已批准
> **日期**: 2026-04-30
> **目标**: 在正式开发前验证关键技术方案的可行性，降低架构风险

---

## 1. 预研项清单

| 优先级 | 预研项 | 阻塞模块 | 工期 | 负责人 |
|--------|--------|----------|------|--------|
| **P0** | LangChain4j 技术验证 | agent-engine | 2 周 | Agent 引擎组 |
| **P0** | OpenSandbox Java SDK 集成 | agent-engine | 2 周 | Agent 引擎组 |
| **P0** | Embedding 服务选型 | context | 1 周 | 数据/RAG 组 |
| **P1** | LangGraph4j Team Agent 评估 | agent-engine (Phase 9) | 2 周 | Agent 引擎组 |
| **P1** | ClickHouse JDBC 性能测试 | ops | 1 周 | 集成/运营组 |
| **P2** | Milvus 并发检索压测 | context | 1 周 | 数据/RAG 组 |

---

## 2. LangChain4j 技术验证

### 2.1 目标

验证 LangChain4j 0.31.0 是否满足以下核心能力：

- [ ] Streaming 响应（SSE 兼容）
- [ ] Tool Calling（函数调用）
- [ ] Chat Memory 管理
- [ ] Token 计数估算
- [ ] 多 Provider 切换（OpenAI / Anthropic）

### 2.2 预研任务

- [ ] **Task 1**: 搭建 LangChain4j 最小可运行项目
  - 创建独立 Maven 模块 `langchain4j-research`
  - 引入依赖 `langchain4j-spring-boot-starter`
  - 配置 OpenAI API Key
  - 预计: 0.5d

- [ ] **Task 2**: Streaming 验证
  - 实现 `generateStream()` 方法
  - 验证 Token 逐字返回
  - 验证 SSE 格式兼容性
  - 预计: 0.5d

- [ ] **Task 3**: Tool Calling 验证
  - 定义 2-3 个工具（计算器、天气查询）
  - 验证 LLM 自动选择工具
  - 验证工具结果回传后 LLM 继续推理
  - 预计: 1d

- [ ] **Task 4**: Memory 管理验证
  - 实现 `ChatMemoryStore` 接口
  - 验证历史消息注入 Prompt
  - 验证 MessageWindowChatMemory 行为
  - 预计: 0.5d

- [ ] **Task 5**: 多 Provider 切换
  - 同时配置 OpenAiChatModel 和 AnthropicChatModel
  - 验证运行时切换
  - 验证异常降级
  - 预计: 0.5d

### 2.3 验收标准

| 能力 | 通过标准 |
|------|----------|
| Streaming | 延迟 < 1s 首 Token，流式输出无乱码 |
| Tool Calling | 工具识别准确率 > 90%，执行后正确汇总 |
| Memory | 历史消息正确注入，超出窗口自动截断 |
| Token 计数 | 估算误差 < 10% |
| Provider 切换 | 切换延迟 < 100ms，异常时自动降级 |

### 2.4 风险与回退

| 风险 | 影响 | 回退方案 |
|------|------|----------|
| Tool Calling 不满足 | Phase 2 阻塞 | 直接调用 OpenAI/Anthropic SDK，自行封装 Tool Calling |
| Streaming API 变更 | 未来升级不兼容 | 防腐层隔离，仅影响适配器 |
| Token 计数不准 | 预算超支 | 使用 OpenAI tiktoken 库直接计算 |

---

## 3. OpenSandbox Java SDK 集成预研

### 3.1 目标

验证 OpenSandbox 作为 Agent 执行沙箱的可行性：

- [ ] Java SDK 可用性
- [ ] 文件系统沙箱隔离
- [ ] 终端命令执行
- [ ] 浏览器自动化
- [ ] 与现有 Agent 状态机集成

### 3.2 预研任务

- [ ] **Task 1**: OpenSandbox 部署
  - Docker 部署 OpenSandbox Daemon
  - 验证 gRPC/REST 接口可用
  - 预计: 0.5d

- [ ] **Task 2**: Java SDK 集成
  - 引入 OpenSandbox Java Client
  - 实现连接、断开、健康检查
  - 预计: 0.5d

- [ ] **Task 3**: 文件系统操作验证
  - 创建/读取/写入/删除文件
  - 验证租户隔离
  - 预计: 0.5d

- [ ] **Task 4**: 终端命令执行验证
  - 执行 shell 命令（ls, cat, grep）
  - 验证超时、输出捕获
  - 预计: 0.5d

- [ ] **Task 5**: 浏览器自动化验证
  - 打开网页、点击元素、提取内容
  - 验证与 Playwright/Selenium 兼容性
  - 预计: 1d

- [ ] **Task 6**: Agent 状态机集成原型
  - 修改 `ToolCallingStateHandler` 调用 OpenSandbox
  - 验证 Thinking → ToolCalling → Observation 循环
  - 预计: 1d

### 3.3 验收标准

| 能力 | 通过标准 |
|------|----------|
| 文件系统 | 单命令延迟 < 500ms，租户隔离生效 |
| 终端执行 | 命令超时可控，输出正确返回 |
| 浏览器 | 页面加载 < 5s，元素操作准确 |
| 集成 | 状态机循环完整运行无异常 |

### 3.4 风险与回退

| 风险 | 影响 | 回退方案 |
|------|------|----------|
| OpenSandbox 不成熟 | +2 周工期 | 使用 Docker-in-Docker 自建沙箱 |
| Java SDK 缺失 | 无法集成 | 使用 gRPC 直接调用，自行封装客户端 |
| 性能不达标 | Agent 执行慢 | 本地 JVM 执行 + 结果校验（降级方案） |

---

## 4. Embedding 服务选型预研

### 4.1 候选方案

| 方案 | 模型 | 维度 | 成本 | 延迟 | 数据隐私 |
|------|------|------|------|------|----------|
| A | OpenAI text-embedding-3-small | 1536 | $0.02/1M | ~100ms | 数据出境 |
| B | OpenAI text-embedding-3-large | 3072 | $0.13/1M | ~200ms | 数据出境 |
| C | 本地 BGE-large-zh | 1024 | 0 | ~500ms | 本地 |
| D | 本地 m3e-base | 768 | 0 | ~300ms | 本地 |

### 4.2 预研任务

- [ ] **Task 1**: 精度对比测试
  - 准备 100 组中文查询-文档对
  - 分别用 4 种方案生成 Embedding
  - 计算 Top5 准确率、MRR
  - 预计: 2d

- [ ] **Task 2**: 性能基准测试
  - 测试单文档 Embedding 延迟
  - 测试批量 Embedding 吞吐
  - 测试内存占用
  - 预计: 1d

- [ ] **Task 3**: 部署方案评估
  - 本地模型：ONNX / TensorRT 加速可行性
  - 云 API：网络稳定性、配额限制
  - 预计: 1d

### 4.3 推荐策略

```
默认: OpenAI text-embedding-3-small（精度高、延迟低）
敏感数据场景: 本地 BGE-large-zh（数据不出境）
混合策略: 先本地粗排 → Top100 云 API 精排
```

---

## 5. 预研产出物

每完成一项预研，必须产出：

1. **技术验证报告** (`docs/research/<topic>-report.md`)
   - 测试环境、测试数据、测试方法
   - 量化结果（表格 + 图表）
   - 通过/不通过结论
   - 风险与回退方案验证

2. **原型代码**
   - 提交到 `research/<topic>/` 分支
   - 包含 README 说明如何运行

3. **决策建议**
   - 是否采纳该技术方案
   - 如果采纳，集成注意事项
   - 如果不采纳，替代方案

---

## 6. 时间线

```
第 1 周:
  ├─ LangChain4j Task 1-3 (Streaming + Tool Calling)
  ├─ OpenSandbox Task 1-3 (部署 + 文件系统 + 终端)
  └─ Embedding Task 1 (精度对比)

第 2 周:
  ├─ LangChain4j Task 4-5 (Memory + Provider 切换)
  ├─ OpenSandbox Task 4-6 (浏览器 + 集成)
  └─ Embedding Task 2-3 (性能 + 部署)

第 3 周:
  ├─ LangGraph4j 评估（可选，P1）
  ├─ ClickHouse 性能测试（P1）
  └─ 报告汇总 + 评审
```

---

## 7. 相关文档

- `docs/plans/project-plan.md` v1.1（关键风险表）
- `docs/decisions/ADR-003-langchain4j-selection.md`
- `docs/decisions/ADR-002-cursor-sdk-to-opensandbox.md`
