# SchemaPlexAI v1.0 — Spec-to-Code 合规复核报告

- **日期**: 2026-08-29
- **方法**: 逐需求规格核查(每份规格一个独立上下文)+ 独立反驳复核(非原作者重读代码与规格,尝试推翻每条 Critical/High 分歧)
- **范围**: `docs/specs/` 下 13 份规格文档(12 份已批准/已实现口径 + 1 份草案),覆盖全部后端业务模块与前端
- **反驳结果**: 65 条 Critical/High 分歧全部经独立复核,**0 条被推翻,全部维持**(2 条仅调整表述口径)
- **逐需求明细**: 见 `requirements/` 目录(13 个文件,每份末尾附「## 反驳复核」段)

---

## 1. 总体结论

**项目处于"框架齐备、链路大量未接通"的状态。** 13 份规格共提取 311 条可核查需求:

| 裁决 | 条数 | 占比 | 含义 |
|---|---|---|---|
| implemented | 117 | 37.6% | 需求成立(含部分文档漂移注记) |
| partial | 103 | 33.1% | 常规路径成立、某可达路径缺失 |
| contradicted | 47 | 15.1% | 代码与规格直接矛盾 |
| absent | 37 | 11.9% | 完全缺失(均附搜索记录) |
| undecidable | 5 | 1.6% | 需运行时验证(压测/基准) |
| notChecked | 2 | 0.6% | 未核查(规格明示非目标口径) |

**最严重的问题不是缺功能,而是"宣称与实态背离"**:多个规格标 `status: implemented/completed`,CHANGELOG 宣告 1.0.0 发布,但核查证实多条关键执行链路在生产环境是死端(见 §3)。同时存在大量**文档自身失真**(见 §6),它们会持续误导后续开发与发布决策。

分域完成度排序(按 implemented 占比):
**前端(80%)** ≫ 网关(56%) > 通知(50%) > 发布就绪清单(52%,但门禁项全红) > Agent 引擎(28–65%) > 集成(39%) > 成本/流程(13%) > RAG(25%) > Spec 管理(10%) > 工作流(13%) > 质量门禁(24%,核心全旁路)

---

## 2. Critical 发现(独立复核全部维持)

| # | 模块 | 发现 | 后果 | 证据 |
|---|---|---|---|---|
| C1 | agent-engine | `Map.of` 不可变载荷 + 发布器首行 `put` 必抛异常 | **所有** GATE_BLOCKED(护栏/循环/预算/准入/拦截)退化为 FAILED,MQ 阻塞事件从不发出,暂停-恢复链路不可达 | GateBlockedStateHandler.java:56,70; AgentExecutionEventPublisher.java:23 |
| C2 | agent-engine | 工具结构化解析唯一调用点恒传 `provider=null` → 路由到不存在的解析器 → 恒返回空 | 状态机路径下工具永远无法执行 | ToolCallingStateHandler.java:95; ToolRegistry.java:85-91 |
| C3 | agent-engine | `cancel()` 置单例全局 `volatile` 标志,无 executionId | 取消任一执行连带取消同实例所有并发执行 | AgentRuntimeOrchestrator.java:53,129-133 |
| C4 | quality | `evaluate()` 零生产调用方;三个规格触发点无一接入 | 门禁全路径旁路;叠加 DDL `status` 类型冲突(VARCHAR vs Integer),即使接入查询也永不命中 | QualityOrchestrator.java:43; 03-init-schema-others.sql:139 |
| C5 | workflow | DRAFT 门禁不存在,任意状态模板可直接触发;Flowable 桥接注册不存在的节点类型 "AI_AGENT" | 未发布流程可执行;BPMN 执行必失败 | WorkflowInstanceServiceImpl.java:45-48; AiAgentExecutionDelegate.java:68 |
| C6 | integration | Git 凭据明文内存存储并拼入 clone URL(spec 要求 AES-256-GCM);无租户键 | 凭据暴露 + 跨租户共享存储 | GitIntegrationService.java:56,321-329 |
| C7 | notification | 两份冲突 DDL:03 先建表(无 `read` 列),04 裸 `CREATE TABLE` 在 `ON_ERROR_STOP=1` 下中止初始化 | 通知三端点全部引用不存在的列;全新部署数据库初始化失败 | 03:281-292 vs 04-notification.sql:4 |
| C8 | cost | Token 用量采集源头不存在(全仓 11 处消息发布无一处发成本事件);非 gpt-4/3.5 模型计费恒 0 | 成本管线空转,报表/预算恒空,计费失真按调用累积 | LlmProviderAdapter.java:81-85; CostService.java:153-154 |
| C9 | rag | 检索服务存在空租户旁路分支(当前无调用方,其余路径 fail-closed) | 潜在跨租户读;一旦被接线即成实害 | RagSearchServiceImpl.java:66-68 |
| C10 | gateway | `lb://` 无任何服务发现源;过滤器链倒置(认证先于限流短路) | 所有路由运行期预期 503;匿名流量永不受限流 | api-gateway.md REQ-02/03 |

---

## 3. High 发现摘要(独立复核全部维持)

**执行链路死端**(宣称已实现但不可达):
- RAG 检索:规格端点 `/rag/search` 不存在,实际服务无任何注入点,异步消费者是恒抛异常的桩(上传失败致文档"蒸发")
- MCP 工具发现:`McpClient.listTools()` 恒返回空的桩;`status=1` 整型查询与库中 `VARCHAR 'ACTIVE'` 互斥 → 生产发现不了任何工具(`completed` 仅在"全 mock 测试"口径成立)
- 成本:3 条消费链路齐备但无生产者;ClickHouse 同步对象错位(同步元数据而非成本)
- 工作流:节点间无变量传递、无超时、无重试、无取消入口;SCRIPT 节点为永败存根;人工审批表/状态不存在
- agent-engine:重试时 `failedToolName` 无人写入 → 重放 0 个调用;RESUMING 快照主键错绑 → resume 恒失败;PLANNING 为无进入路径的死状态;RAG 注入双重断路(无 Bean + @Async 无租户上下文)

**控制项缺失**:
- gateway:免认证面扩大至 `/system/tenants/**`;change-password 经网关恒 401;租户存在性校验缺失;访问日志零脱敏(`?token=` 明文落日志)
- spec-management:已发布规格可被任意改写;版本回滚缺失;权限矩阵不存在(任意合法 JWT 可发布/删除);乐观锁未装配,并发写静默覆盖
- web 层:通知分页退化为无 LIMIT 全量查询(`total`/`pages` 恒 0);web 模块无租户拦截器
- integration:OAuth 回调仅打日志(无 token 交换);外部调用零超时(`process.waitFor()` 无限阻塞)
- cost:价格列不存在、精度 4 位(规格 6 位)致小额调用系统性归零、预算阈值双单位(0.8 与 80.00)并存致告警在 0.8% 触发
- release-readiness:发布门禁"0 failures"不成立(最近运行 3 失败);覆盖率门禁 6 模块 2 模块不达标

**前端**:构建红色(`tsc` 阶段 2 处未用导入,已实测复现);Cockpit 统计为永不发请求的桩,离线通知分支为死代码;1 个测试断言过期刷新契约。

---

## 4. 系统性模式(跨模块共因)

1. **桩与死端**:多个子系统交付了"壳"(消费者/调度/控制器)但处理实现是恒抛异常或恒空桩——Milvus 同步、质量事件、MCP 客户端、工作流 SCRIPT、RAG 检索。
2. **发布/消费断链**:消费侧齐备但生产侧不存在(成本 `sf.cost`、质量 `sf.quality`),或生产侧存在但载荷构造必抛异常(C1)。
3. **实体-DDL 类型漂移**:至少 4 处 `Integer status` 实体对 `VARCHAR` DDL(quality_gate、quality_issue、mcp_server、spec 状态词表三方漂移),部分导致查询永不命中或写入必失败。
4. **微服务边界漂移**:task/web/engine 直接 Maven 依赖 ops 模块;多套平行实现(通知在 web 与 ops 各一套,两套 DDL 下均无法工作;MCP 双栈互不相通)。
5. **声明式宣称未经验证**:`status: implemented/completed` 与 CHANGELOG 发布叙事领先于实际接线状态。

---

## 5. 文档漂移清单(修文档,不修代码)

| 文档 | 问题 |
|---|---|
| `docs/specs/README.md` | 多份规格标"已批准",实际 frontmatter 为 `status: 草稿`(quality-gate、rag-pipeline、workflow-engine) |
| `CHANGELOG.md` | 后端测试数宣称 2,601,实测产物 ≈4,129;分项偏差 30–180%;两处数字自相矛盾;已知限制一节与实态相反 |
| `AGENTS.md`(仓根) | 严重过期:声称"零测试"(实有 4,000+)、MySQL 数据源(实为 PostgreSQL)、admin 空桩(实有 25 主文件+111 测试)、自动 ACK 等 |
| `docs/COVERAGE.md` | 记录 2026-05-25 全绿,与 2026-07-02 最近运行 3 失败冲突;当前工作树恰有对应源码的未提交修改(疑似修复中) |
| MASTER.md | Day-0 判 NOT-READY 与同日 CHANGELOG 发布 1.0.0 叙事冲突 |

---

## 6. 未核查与不可判定

- **undecidable(5)**:全部为非功能指标(压测类),需运行时设施;其中发布就绪两项(无扫描产物/无基线)需 `trivy`/dependency-check 与前后两次测试运行。
- **notChecked**:agent-engine 规格 §6 非功能指标、通知规格 QPS 指标、性能基准类——均需压测/集测设施,非静态可核查。
- 本次核查基于**当前工作树**(含 3 个未提交修改文件);该修改(暂停键租户化)经复核方向正确,但与 agent-engine 现存 3 个失败测试无交互,失败仍在。

---

## 7. 建议优先级

**P0 — 发布阻断/数据完整性**:
1. 修 C1(GATE_BLOCKED 载荷)与 C2(工具解析接线)——否则引擎核心链路名存实亡
2. 修 C7(DDL 冲突)与 §4-3 的实体/DDL 类型漂移——全新部署即失败或数据写入失败
3. 修 C10(gateway 服务发现 + 过滤器顺序)——否则所有路由 503
4. 跑通并修复 agent-engine 当前 3 个失败测试(工作树已有修复中修改),恢复发布门禁诚实性

**P1 — 控制项补齐**:C6 凭据加密、C4 门禁接线(或正式宣布降级为后续版本)、C5 工作流 DRAFT 门禁、spec-management 编辑守卫+权限、web 分页/租户拦截器

**P2 — 死端清理**:决定每个桩子系统(质量事件、Milvus 同步、MCP 客户端、RAG 检索、成本生产者)是接线、降级还是从规格中撤下——当前"宣称完成但不可达"是比"未做"更大的风险源

**P3 — 文档修复**:§5 全部五份文档,其中 AGENTS.md 与 CHANGELOG 优先(它们直接误导代理与后续开发者)
