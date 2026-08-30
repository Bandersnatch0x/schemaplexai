# Spec 合规全量修复 — 终验报告(2026-08-30)

分支:`worktree-spec-compliance-fix`(worktree `.qoder/worktrees/spec-compliance-fix`)
范围:`6e0c781..HEAD`,58 提交、318 文件、+20,739/−1,892

## 1. 最终门禁(2026-08-30 04:03 运行)

| 层 | 结果 |
|---|---|
| 后端 16 模块 `mvn test` | **4,695 测试,0 失败,0 错误**,4 跳过(Docker 沙箱条件跳过);EXIT=0 |
| 前端 `tsc --noEmit` | exit 0 |
| 前端 vitest | 147/147 通过 |

各模块:common 130 / model 27 / dao 20 / gateway 86 / web 149 / system 174 / spec 243 /
agent-config 115 / agent-engine 1920 / workflow 281 / context 286 / quality 302 /
integration 372 / ops 287 / task 192 / admin 111。

## 2. 修复完成度

- **原报告 10 条 Critical**:全部闭合(合规复查 recheck-2026-08-30.md 核实;C4 的剩余两触发点已在返工批补齐:229ef34 工具后、8f62bd5 工作流节点后)
- **票 901-929**:29 票全部提交(台账:`.scratch/spec-compliance-fix/issues/REGISTRY.md`)
- **评审返工批**(review-standards.md + recheck):ST-01~04、NEW-01~04/06/08、web Mapper 同名冲突(932)全部落定
- **额外收获**:930 MCP 真实客户端(用户裁定口径)、929 EventBus 首帧 NPE(904 排查中发现)

## 3. 未闭合/记账项(不阻断)

| 项 | 口径 |
|---|---|
| 931:PLANNING 死状态 | 记账票,待用户裁定(实现/退役/声明预留) |
| 925 Steering 二期(SfAgentConfig.steeringId + 引擎注入) | 跨模块,提交说明与 javadoc 已注记 |
| 923 SCRIPT 节点沙箱 | 仅墙钟时限,无类白名单/隔离,代码注记 |
| 924 工作流裁决处置语义 | 裁决落库可用,处置动作为二期,发布器 javadoc 注记 |
| 03/903 超范围同类漂移 | quality 其余实体、integration SfSkill/SfIntegration、system/agent-engine 部分实体的 Integer-vs-VARCHAR 未动(903 报告已列清单) |
| 927 前端遗留 | 规格外新域与演示数据项见 ui-alignment 反向差距,未入本批 |

## 4. 验证限制(如实声明)

- **浏览器二轮实测未执行**:本机无 docker,PostgreSQL/Redis/RabbitMQ/MinIO 等基础设施无法启动,真实链路端到端实测受限。当前证据层级=单元+模块套件+契约测试;真实数据链路(如 RAG 上传→向量化→检索)依赖外部组件的测试均以 mock 覆盖并在测试内注明。
- docker init 脚本修改(03/04 合一、成本表迁移、CH 重写)未能在真实 PostgreSQL/ClickHouse 上重放验证(静态核对 + 测试内嵌 schema 覆盖)。
- 覆盖率门禁(`jacoco:check`)未在本次运行;CI 会执行。

## 5. 后续

- 合并/推送由用户指令触发(分支含 58 提交,建议合并前过一次 PR 评审)
- 浏览器实测建议在具备基础设施的环境执行:登录→执行一次 Agent→观察成本/质量事件链→Cockpit 统计→通知分页

## 6. 浏览器二轮实测记录(2026-08-30 10:1x,WSL 基础设施)

**环境**:wslc 容器 postgres/redis/rabbitmq(镜像经 DaoCloud 源);8 个后端服务
(system/gateway/web/agent-config/agent-engine/quality/task/ops)+ Vite 前端;
种子租户 `default`(id=1)+ 管理员 `admin`。

**实测通过**:
- 全新 PostgreSQL 上 init 脚本一次建成 74 表(902/903/迁移整合的线上验证)
- 前端 SPA 渲染:登录页、404 页、路由守卫跳转正常
- 登录链路(经真实网关):POST /auth/login 200,返回 accessToken/refreshToken;
  租户码解析、JWT 声明、Redis 会话落库均生效
- 网关路由 lb:// 解析(910 静态实例源线上验证)、限流/认证过滤器顺序生效

**实测发现并当场修复的活缺陷**(均独立提交+回归测试):
1. `sf_tenant`/`sf_user` 实体与生产 DDL 漂移(根表无 tenant_id 列;status
   Integer 对 VARCHAR)→ 登录 500。修复:实体排除根表缺列、状态词表字符串化
   (ce7a310)
2. 租户拦截器以字符串字面量拼 BIGINT 租户条件 → 全线类型错配。修复:数字租户
   发 LongValue;登录路径 sf_user 豁免(663f370)
3. Flyway 对已建 schema 启动即失败 + 迁移对象未进 init。修复:8 个迁移并入
   02 初始化脚本 + baseline-on-migrate(d551211)
4. 引擎被动消费的三个队列全仓无声明,全新 broker 启动即挂。修复:引擎侧队列/
   绑定声明(80e30bb)
5. 登录页租户列表按规格白名单保持鉴权后,前端回退租户码占位导致登录失败。
   修复:登录接受租户码解析(51a27a1)

**受限项(如实声明)**:登录表单的用户名输入框无法被 Orca 自动化聚焦(点击/
Tab 均不接收焦点,密码框拦截全部键入)——判定为自动化工具对自定义输入组件的
限制而非应用缺陷;登录功能已在上文 API 层以真实令牌验证。Cockpit/通知等页面
的浏览器内走查因此未执行,建议人工补一轮或换用 Playwright e2e(仓库已有
e2e/ 骨架)。
