---
title: DXArchitect 强化问题清单
agent: DXArchitect
date: 2026-05-08
domain: dx
purpose: 在 v1 GA 决策前，与产品负责人/工程负责人/早期用户进行交互式访谈，校准 DX 投入方向。
---

# DX 强化问题清单（35+）

> 使用方法：每节按顺序问完再进入下一节；任何"我不知道"的回答都是高优先级 backlog 输入。
> 答题人建议：PM、Tech Lead、首批 3 名外部 Beta 用户。

---

## 一、开发者画像（5 题）

1. **目标开发者是谁？** 内部研发团队 / 开源社区贡献者 / 客户工程师 / 个人开发者，分别是几比几？
2. **他/她最近一次评估的同类产品是什么？**（Cursor / Devin / Replit / Windsurf / 自研内部平台）评价是什么？
3. **第一次接触 SchemaPlexAI，他/她有多少分钟的耐心？** 以"放弃"为指标——超过几分钟未看到 demo 就关 tab？
4. **他/她的本地环境是什么？** Mac M1 / Windows WSL / Linux 工位 / GitHub Codespaces？是否假设他有 Docker Desktop？
5. **他/她日常 IDE 是什么？** 是否需要提供 VS Code 插件或 IntelliJ Run Configurations？

---

## 二、TTHW（8 题）

6. **你能接受新人 TTHW 是多少？** v1 目标：≤ 5 分钟（Codespaces）/ ≤ 15 分钟（本地）。同意吗？
7. **是否愿意提供托管 demo？** `demo.schemaplexai.com` 公网 demo 站的运维成本（LLM token 消耗）能承担吗？
8. **是否提供 Codespaces？** 月免费 60h，对个人 Beta 用户够用，是否接受添加 `.devcontainer/`？
9. **`docker-compose.lite.yml` 决策**：能否接受 v1 默认仅启动 PostgreSQL / Redis / MinIO 三个中间件，把 Milvus / ES / ClickHouse / ClamAV / Jaeger 全部放到 `--profile full` 里？这意味着 RAG / 安全扫描 / 分析 / 追踪在 lite 模式不可用——是否在 Cockpit 显式标注"功能开关"？
10. **预编译镜像**：能否接受发布 `ghcr.io/schemaplexai/agent-engine:v1`，让用户跳过 mvn 编译？
11. **Seed 数据策略**：默认 seed 哪些示例 Agent？建议：Summarizer、Translator、Code-Reviewer 三件套——同意吗？
12. **MockLlmProvider 决策**：是否接受 v1 默认在 `dev` profile 启用 mock，让用户无需 LLM key 也能完成"5-second magic"？
13. **首启失败兜底**：如果某中间件 healthcheck 失败，是要"立即报错并提示"还是"降级模式继续"？

---

## 三、魔法时刻（6 题）

14. **§4 三个候选魔法时刻 A/B/C，哪个最该是 v1 默认？** 评估维度：实现成本 / 视觉冲击 / 与竞品差异化。
15. **5 秒首屏 SSE 的"5 秒"如何度量？** 从点击 "Try Demo" 到第 1 个 token 出现？还是到完整回答完成？
16. **demo prompt 的内容是什么？** 是要展示推理深度（"分析这段代码的安全风险"）还是展示速度（"用三句话总结"）？
17. **是否用 streaming markdown 渲染？** 还是纯文本带光标动画？前者更"WOW"，后者实现更稳。
18. **完成后的 CTA（Call-To-Action）是什么？** "Create Your Own Agent" / "Connect Your LLM Key" / "View Workflow"？
19. **是否做"Replay 模式"？** 允许用户分享一个执行 ID，他人 5 秒内重放整个 SSE 流——这是企业 R&D 协作的高频需求。

---

## 四、痛点（8 题）

20. **当前最让人懵的报错是哪一条？**（请给出具体的截图或日志）
21. **新人最常问的 3 个问题是什么？** 这些问题是否已写进 quickstart？
22. **网关 502 时前端如何提示？** 当前 axios 拦截器是直接 toast 还是直接跳 `/login`？是否暴露了"后端未启动"的真因？
23. **`X-Tenant-Id` 缺失时的 UX**：是要静默选 default 租户，还是强制选择？
24. **JWT 过期 401 后的体验**：当前在 `request.ts` 里有 refresh，refresh 失败跳 `/login`——但用户输入到一半的 prompt 会丢吗？
25. **Composer 文件上传失败的提示**：ClamAV 扫描不可用时，当前 `Tooltip` 已说明，但没有"重试 / 联系管理员"按钮——是否需要补？
26. **AgentCanvas X6 大画布性能**：节点超过 50 个时是否还流畅？是否做了虚拟化？
27. **i18n 完成度**：当前看到 `useTranslation`，但实际中英文覆盖率多少？v1 是中英双语都达标，还是只保中文？

---

## 五、竞品对比（6 题）

28. **vs Cursor**：Cursor 优势在 IDE 内，SchemaPlexAI 是 Web。我们的差异化是"团队协作 + 工作流"——这一点在首屏体现了吗？
29. **vs v0**：v0 输出 React 组件，30 秒可见。我们能否输出"可执行的 Agent 配置 JSON"，30 秒可见？
30. **vs Replit Agent**：Replit 主打"零环境"，我们能在 Codespaces 做到"零环境"吗？
31. **vs Devin**：Devin 主打"自主执行"，我们的"AI 工作流编排"如何与之差异化？
32. **vs LangChain Hub / Flowise**：开源同类已饱和，我们的"企业级多租户 + 质量门禁 + 成本分析"差异化在 v1 是否落地？
33. **是否做"产品对比页"？** `docs/comparisons/cursor.md`、`docs/comparisons/v0.md`，让搜索引擎导流。

---

## 六、删减勇气（4 题）

34. **Quality 四页面（QualityCenter / Issues / Gates / SecurityAudit）v1 砍掉**，你接受吗？理由：后端无测试无实现，留页面误导用户。
35. **Tasks 三页面合并为单页 + Drawer**，你接受吗？
36. **`schemaplexai-admin` 空模块从 pom.xml 暂时移除**，你接受吗？
37. **IntegrationCenter v1 仅保留 GitHub + MCP**，GitLab/Jenkins 隐藏入口，你接受吗？

---

## 七、路线图（8 题）

38. **v1 GA 时间线是什么？** 4 周 / 8 周 / 12 周？决定我们能在 DX-Expand 投入多少。
39. **谁是 v1 的"DX Owner"？** 现在没有这个角色——谁来扛 5 秒魔法时刻这个 KPI？
40. **DX 度量指标是什么？** 建议：TTHW（手动计时 + Codespaces 自动化）、首日激活率、demo 完成率、`docker-compose up` 失败率（Sentry 上报）。
41. **是否做内测计划？** 邀请 5–10 名外部开发者，记录他们的 TTHW，每周复盘 top-3 阻塞点？
42. **是否提供 `schemaplexai cli`？**（Go 或 Node 实现）让 power user 可在终端创建 Agent / 触发执行 / 查看日志？
43. **VS Code 插件优先级**？早做能在 IDE 内嵌 Composer，与 Cursor 抢心智。
44. **OSS 还是 Source-available？** 决定我们能否接受社区 PR、能否走 GitHub Star 路线。
45. **首批"标杆案例"是谁？** 在 v1 之前能否锁定 1 家内部业务团队 + 1 家外部客户作为 reference design partner？

---

## 八、回答收集模板

> 建议把以上 45 题分发给 PM / Tech Lead / 3 名 Beta 用户，每人答 60 分钟内可完成的子集（约 15 题）。
> 收集后由 DXArchitect 汇总，输出 `dx-decisions.md`，作为 v1 DX backlog 的 source of truth。

| 题号 | 答题人 | 回答 | 优先级（P0/P1/P2） | 关联到 §6 哪一项决策 |
|---|---|---|---|---|
| 1 |   |   |   |   |
| ... |   |   |   |   |
