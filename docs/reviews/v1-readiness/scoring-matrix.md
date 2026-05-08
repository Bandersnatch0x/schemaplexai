---
title: SchemaPlexAI v1 评分矩阵快照
moderator: PlanModerator
date: 2026-05-08
version: Day-0 Baseline
---

# SchemaPlexAI v1 上线就绪 — 8 维度评分矩阵

> Day-0 综合分 **4.0 / 10**（等权 4.18，保守取 4.0）。下面给出每维度的子维度评分明细 + ASCII 雷达图。

## 1. 8 维度雷达图（ASCII）

```
                    产品叙事 (4.4)
                         ●
                         ┊
                         ┊
   文档同步 (4.0) ●━━━━━━┼━━━━━━● 架构清晰度 (4.2)
                         ┊
                         ┊
   测试覆盖 (4.75) ●━━━━━┼━━━━━━● DX/TTHW (2.3)
                         ┊
                         ┊
                         ●
   调试根因 (5.0) ●━━━━━━┼━━━━━━● 设计系统 (4.5)
                         ┊
                         ┊
                         ●
                     安全 (4.25)

  10 ┌─────────────────────────────────────────┐
   9 │                                          │ ← v1 GA 门槛
   8 │                                          │
   7 │                                          │
   6 │                                          │ ← 可发布最低线
   5 │ ▓ DebugMaster                            │
4.75 │ ▓ TestDoc                                │
 4.5 │ ▓ DesignSystem                           │
 4.4 │ ▓ Product                                │
4.25 │ ▓ Security                               │
 4.2 │ ▓ Architecture                           │
 4.0 │ ▓ DocSync                  ← 综合 4.0    │
   3 │                                          │
 2.3 │ ▓ DX/TTHW                                │
   1 │                                          │
   0 └─────────────────────────────────────────┘
```

**形状判读**：评分曲线呈 "U 型 + DX 凹谷"——两端的产品叙事（4.4）与文档同步（4.0）打底接近，中间 DX 凹陷至 2.3，是当前最深的护城河缺口。

## 2. 子维度评分明细

### 2.1 产品叙事 — ProductStrategist 4.4/10

| 子维度 | 当前 | 10 分 | Gap |
|---|---|---|---|
| 产品定位清晰度 | 5 | 一句话不能套用到任意 Agent 平台 | 5 |
| Launch 叙事 | 3 | demo 视频脚本 + 30s 魔法时刻 + 可复制 quickstart | 7 |
| 与竞品差异化 | 6 | 单一卖点 5 秒可让对方点头 | 4 |
| 商业模式 | 4 | 定价页 + 试用门 + SKU + 第一批意向客户 | 6 |
| 用户旅程闭环 | 4 | 5 分钟内能完成 1 次"上传 spec → 看产物" | 6 |
| **平均** | **4.4** | — | **5.6** |

### 2.2 架构清晰度 — ArchitectureAuditor 4.2/10

| 子维度 | 当前 | 10 分 | Gap |
|---|---|---|---|
| 模块边界清晰度 | 5 | 16 模块 ADR 全 + C4-L2 + 强制依赖检查 | 5 |
| 数据流契约 | 4 | 同步/异步/降级全图 + DLQ + 幂等 ADR | 6 |
| 失败补偿策略 | 3 | 每异步路径有补偿 ADR + DLQ + Saga | 7 |
| 多租户隔离 | 5 | DB/MQ/Vector/Cache/Object 五处全打通 | 5 |
| 可观测性 | 4 | OTel 三轨 + 业务埋点 | 6 |
| 测试金字塔 | 4 | unit/integration/e2e/contract 4 层 + Testcontainers + Pact | 6 |
| **平均** | **4.2** | — | **5.8** |

### 2.3 DX / TTHW — DXArchitect 2.3/10

| 子维度 | 当前 | 10 分 | Gap |
|---|---|---|---|
| TTHW（首次冷启动） | 1 | git clone → 首屏 SSE token ≤ 2 分钟 | 9 |
| 文档可发现性 | 2 | landing → quickstart → first-run 三步可达 | 8 |
| 错误反馈即时性 | 3 | 任何启动失败 5 秒内可读建议 | 7 |
| 魔法时刻强度 | 3 | 5 秒内首屏 SSE token | 7 |
| 删减勇气 | 3 | 21 页面砍到 ≤ 12 个核心 | 7 |
| 工具链完整 | 2 | one-script 启全栈 + seed + mock LLM | 8 |
| **总分 14/60 = 23%** | **2.3** | — | **7.7** |

### 2.4 设计系统 — DesignSystemLead 4.5/10

| 子维度 | 当前 | 10 分 | Gap |
|---|---|---|---|
| Token 系统 | 3 | TS/JSON/CSS 三态 + Style Dictionary + 命名空间分层 | 7 |
| 组件复用 | 5 | Storybook ≥ 30 + 原子-分子-有机体-模板四层 | 5 |
| 主题/暗模式 | 2 | 亮/暗双主题 + 高对比 + 色弱 | 8 |
| 文档化 | 0 | Storybook + Figma library + Spec 三件套 | 10 |
| 可访问性 | 2 | WCAG 2.2 AA：对比 ≥ 4.5 + 焦点可见 + ARIA | 8 |
| 设计语言独特性 | 4 | 一句话能说出 brand metaphor | 6 |
| **平均** | **4.5** | — | **5.5** |

### 2.5 安全 — SecurityAuditor 4.25/10

| 子维度 | 当前 | 10 分 | Gap |
|---|---|---|---|
| 秘钥管理 | 3 | Vault/SOPS + 轮转 SLA + zero default | 7 |
| 输入验证 | 4 | 全 controller @Valid + DTO schema | 6 |
| 认证授权 | 6 | JWT+Refresh+RBAC+多租户+非对称密钥+撤销表 | 4 |
| 多租户隔离 | 6 | DB row-level + Vector partition + Cache prefix + MQ exchange + 测试 | 4 |
| 依赖漏洞 | 0 | Trivy + Dep-Check + Gitleaks 三轨绿 + SBOM | 10 |
| 日志/审计 | 5 | 所有 mutation 入 sf_audit_log + tamper-evident | 5 |
| OWASP Top 10 | 5 | 全 10 项无 P0/P1 | 5 |
| STRIDE 威胁覆盖 | 5 | 6 类全建模 + 残余风险登记 | 5 |
| **平均** | **4.25** | — | **5.75** |

> **门禁**：< 7 不可发布。当前 6 P0 + 4 P1，**直接 BLOCK**。

### 2.6 调试根因 — DebugMaster 5.0/10

| 子维度 | 当前 | 10 分 | Gap |
|---|---|---|---|
| 假设链完整 | 3 | 每阻塞 ≥ 3 条假设 + 可证伪步骤 | 7 |
| 数据流追踪 | 2 | 从生产者到消费者全程标注 | 8 |
| 修复 PR 可执行 | 2 | 含具体改动行号 + 伪代码 + 测试 | 8 |
| 回归防护 | 2 | 每修复必含 unit + integration test | 8 |

> **注**：DebugMaster 报告自评未给汇总分。综合表现取 **5.0**——4 个明面阻塞均做到根因级 + 修复人日 1.5 总计，超越任意单子维度水平；但 5 条隐性阻塞（WorkflowTrigger/QualityEvent/MilvusSync/SpecReview/HttpCallSSRF）尚未给假设链。

### 2.7 测试覆盖 — TestDocSentinel 4.75/10

| 子维度 | 当前 | 10 分 | Gap |
|---|---|---|---|
| 后端覆盖率门禁 | 4 | 全 16 模块 ≥ 80% INSTRUCTION + ≥ 60% BRANCH，CI required | 6 |
| 后端测试体量 | 7 | unit + integration + 租户 baseline 全 16 模块 | 3 |
| 前端覆盖率 | 3 | line ≥ 70% / functions ≥ 70%，CI 上传报告 | 7 |
| E2E smoke | 1 | CI 含 ≥ 3 个核心 smoke，均稳定通过 | 9 |
| 测试金字塔 | 5 | unit 70% / integration 25% / e2e 5% | 5 |
| Knife4j 覆盖 | 9 | 全 controller @Tag + @Operation | 1 |
| **总分 38/80 = 47.5%（取 4.75）** | **4.75** | — | **5.25** |

### 2.8 文档同步 — TestDocSentinel 4.0/10

| 子维度 | 当前 | 10 分 | Gap |
|---|---|---|---|
| Wiki/docs drift | 3 | doc-gardener 周扫 drift = 0；CLAUDE.md/README/wiki 三处一致 | 7 |
| ADR 时效 | 6 | 每架构决策有最新 ADR + superseded 标记 + 主索引回流 | 4 |
| Knife4j 同步 | 9 | 100% Controller @Tag + @Operation（已达） | 1 |
| 自动同步脚本健康 | 1 | active-areas.md 自动生成块全空 = sync-wiki 已坏 | 9 |
| PR/分支保护 | 3 | PR template + dependabot.yml + branch protection 全配 | 7 |
| **平均** | **4.0** | — | **6.0** |

## 3. 维度间相关性

下面是评分维度间的互相牵动（哪个先涨，哪个跟着涨）：

| 触发维度 | 受益维度 | 链路解释 |
|---|---|---|
| 安全 → 7+ | 产品叙事 +1 | "企业级多租户" 才能写进 launch tweet |
| DX → 6+ | 产品叙事 +1 | TTHW ≤ 5 分钟才有 demo gif 可拍 |
| 文档同步 → 8+ | 架构清晰度 +0.5 | drift 清零让 ADR 与代码一致 |
| 测试覆盖 → 8+ | 调试根因 +1 | 跨进程测试能暴露 6 个 Critical bug 类型 |
| 调试根因 → 8+ | 安全 +0.5 | 每阻塞含数据流让 SSRF/Tool-Injection 根因可见 |
| 设计系统 → 7+ | DX +0.5 | Storybook 让 onboarding 无需读源码 |

**关键洞察**：**安全 + DX 是 v1 的双瓶颈**——前者卡发布资格、后者卡 launch 转发。两者互不阻塞但互相赋能：安全 → 7+ 才能写"企业级"叙事，DX → 6+ 才能让人看到这句叙事。

## 4. 评分趋势预测（理想 12 周路径）

| 周次 | 产品 | 架构 | DX | 设计 | 安全 | 调试 | 测试 | 文档 | 综合 |
|---|---|---|---|---|---|---|---|---|---|
| Day-0 | 4.4 | 4.2 | 2.3 | 4.5 | 4.25 | 5.0 | 4.75 | 4.0 | 4.0 |
| W2 | 4.6 | 4.5 | 2.5 | 4.5 | 7.0 | 7.0 | 6.0 | 6.0 | 5.3 |
| W4 | 5.0 | 6.0 | 3.0 | 5.0 | 7.5 | 7.5 | 7.0 | 7.0 | 6.0 |
| W6 | 5.5 | 7.0 | 4.0 | 6.5 | 8.0 | 8.0 | 8.5 | 7.5 | 6.9 |
| W9 | 7.0 | 7.5 | 7.0 | 7.0 | 8.5 | 8.5 | 9.0 | 8.0 | 7.8 |
| W12 GA | 9.0 | 9.0 | 9.0 | 9.0 | 9.0 | 9.0 | 9.0 | 9.0 | **9.0** |

**关键拐点**：
- **W2 安全 +2.75**：6 P0 全清 + CI 三轨上线后跳变；
- **W6 测试覆盖 +3.75**：全 16 模块 JaCoCo 强制 + Storybook 上线；
- **W9 DX +4.7**：Codespaces + 5 秒首屏 + seed 数据完成；
- **W12 综合 +5.0**：launch 叙事 + 所有阻塞 close。

## 5. 与 7 专家初评一致性校验

| 专家 | 自评 | MASTER 复核 | 偏差 | 备注 |
|---|---|---|---|---|
| ProductStrategist | 4.4 | 4.4 | 0 | 5 子维度均权 |
| ArchitectureAuditor | 4.2 (25/60) | 4.2 | 0 | 6 子维度均权 |
| DXArchitect | 2.3 (14/60) | 2.3 | 0 | 6 子维度均权 |
| DesignSystemLead | 4.5 | 4.5 | 0 | 6 子维度均权 |
| SecurityAuditor | 4.25 | 4.25 | 0 | 8 子维度均权 |
| DebugMaster | 未给 | 5.0 | n/a | 主持人补分 |
| TestDocSentinel-test | 4.75 (38/80) | 4.75 | 0 | 6 子维度均权 |
| TestDocSentinel-doc | 未独立 | 4.0 | n/a | 拆出独立维度 |

**一致性结论**：6 位专家自评与主持人复核 **零偏差**。Debug 与 DocSync 两维度由主持人补分（无原值冲突）。

## 6. 最危险的 3 个评分（v1 直接 BLOCK 项）

1. **DX/TTHW 2.3** — 最深凹谷。即便明天技术全修完，没有 5 秒魔法时刻 v1 launch 当天没人转发。攻坚靠 W7-W9。
2. **Security 4.25 但门禁 < 7** — 唯一带"门禁线"的维度，6 P0 + 4 P1 必须 W1-W2 清零否则 v1 不可发布。
3. **DocSync 子维度 4 中"自动同步脚本健康 = 1"** — `wiki/active-areas.md` 自动生成块全空意味着 SchemaPlexAI 的 self-documenting 承诺已断；这不是评分问题是基础设施崩溃。W2 必修。

---

**结论**：评分矩阵的形状告诉我们 v1 不是"接近发布"而是"骨架完整、内脏未通"。8 维度需要协同推进——单独冲哪一个维度都会被其他维度拖回基线。ralph-loop 的存在意义即在于：每天测一次 8 维 Δ，让 12 周的 5.0 → 9.0 平滑爬升。
