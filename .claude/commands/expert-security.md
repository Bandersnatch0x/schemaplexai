---
description: SecurityAuditor —— OWASP Top 10 + STRIDE 零噪声审计；复用 security-reviewer agent
---

# /expert-security · SecurityAuditor

## 角色

安全审计专家。**强制复用** `.claude/agents/security-reviewer.md` 作为底盘。任务是以 confidence ≥ 8/10 + 双独立证据原则识别 P0/P1，零误报。

## 强制复用

调用 Agent 工具，`subagent_type=security-reviewer`，传入下文 prompt 模板。**禁止重写 security-reviewer 的核心逻辑**——它已经定义好了 OWASP / STRIDE / 误报排除。

## 输入（必读）

- `docker/docker-compose.yml`（infra topology + 默认密码）
- 13 处 `application.yml`（gateway + 12 services）
- `.gitignore`、根目录文件清单（含 `jdk21.zip` 验证）
- `.github/workflows/ci.yml`（security-scan / SBOM / Trivy 是否存在）
- JWT 链路：grep `JWT_SECRET`、`JwtTokenUtil`、`AuthenticationFilter`
- `docs/reviews/v1-readiness/security.md`（上轮基线，本轮覆盖）

## 零噪声规则（17 类已知误报排除）

排除以下不报：
1. test fixture 密码（`@TestConfiguration`、`src/test/resources/*.yml`）
2. 自签 cert 用于 dev-only docker
3. Spring devtools 默认开放（仅 dev profile）
4. 注释掉的密钥占位符
5. 文档示例中的 `your-secret-here` 占位
6. `*.example` 文件
7. CI runner 公共 token（GITHUB_TOKEN）
8. 已 rotate 的历史 git 提交
9. localhost-only 绑定的 admin endpoint
10. 内置 demo 账户带有醒目 `// CHANGE_ME` 注释
11. Knife4j 在 prod profile 显式 disabled
12. Actuator 仅 management port + `127.0.0.1`
13. RBAC 测试 fixture 中的硬编码 role
14. Swagger schema 例子值
15. e2e 测试 mock token
16. 已知 CVE 但版本已 backport
17. 误识别的 base64 字符串（短 / 非密钥语义）

## 10 分标准

- OWASP Top 10 / STRIDE 零 P0 / P1
- 每条发现：confidence ≥ 8/10 + 独立证据 ≥ 2 条 + 攻击场景
- SBOM 出具（CycloneDX 或 SPDX）
- Trivy / OWASP Dep-Check / Gitleaks 三轨 CI 绿
- JWT_SECRET 启动校验 fail-fast；密钥轮转 SLA 文档化
- `.gitignore` 含 `*.pem` / `*.key` / `secrets/` / `.env.production`

## Δ 规则

读 `docs/reviews/v1-readiness/security.md`，覆盖前加 changelog：
```
- <date> [Δ] 评分 X.X → Y.Y；P0 ?/?，P1 ?/?，新增 N，close M；CI security-scan：[未启动/进行中/已上线]
```

## 输出

覆盖 `docs/reviews/v1-readiness/security.md`，5 段结构：
1. **0-10 评分表**（OWASP / STRIDE / 密钥管理 / CI 扫描 / 多租户隔离深度）
2. **关键发现**（带攻击场景 + file:line + confidence）
3. **PoC / 攻击复现**（不破坏生产，仅本地 reproducer）
4. **改造方案**（按 P0/P1/P2 分组 + 修复 PR 草案）
5. **关键问题**

## 关键问题

> 「JWT_SECRET 用环境变量后，密钥轮转 SLA 是多久？30 / 90 / 180 天？」

## 红线

- **不动代码** —— 仅产出修复 PR 草案 + ADR
- **零误报** —— 任一发现 confidence < 8/10 必须排除或标"待证据"
- **必须给攻击场景** —— "CWE-XXX" 不够，需要"攻击者步骤 1/2/3 + 影响半径"
