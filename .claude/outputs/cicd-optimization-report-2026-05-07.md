# SchemaPlexAI CI/CD 优化报告

- **审查日期**: 2026-05-07
- **审查范围**: CI/CD 配置、构建优化、部署策略、质量门禁
- **技术栈**: GitHub Actions, Maven, Docker Compose, Playwright

---

## 总体评估

| 维度 | 评分 | 备注 |
|------|------|------|
| CI 流水线 | 7/10 | 3 个 workflow 存在，覆盖后端/前端/E2E |
| 构建优化 | 6/10 | Maven 缓存已配置，但缺少并行构建 |
| 质量门禁 | 5/10 | SpotBugs/Checkstyle 存在但 continue-on-error |
| 部署策略 | 3/10 | 无自动化部署，无环境管理 |
| 监控告警 | 4/10 | Prometheus/Grafana 在 docker-compose 中，但无 CI 集成 |

**CI/CD 成熟度**: Level 2/5（基本 CI，无 CD）

---

## P0 — 必须

### D-001: JaCoCo 覆盖率门禁缺失
- **位置**: `.github/workflows/ci.yml:40-41`
- **描述**: `mvn verify` 只验证构建，未执行 `jacoco:check` 验证 ≥80% 覆盖率
- **建议**: 添加 JaCoCo check 步骤，失败时阻断合并
```yaml
- name: Check test coverage
  run: mvn jacoco:check -pl schemaplexai-common,schemaplexai-agent-engine,schemaplexai-context -am
```
- **优先级**: 立即

### D-002: SpotBugs/Checkstyle 为 continue-on-error
- **位置**: `.github/workflows/ci.yml:44-49`
- **描述**: 静态分析失败不阻断 CI，等于没有门禁
- **建议**: 收集 baseline 后将 `continue-on-error: true` 改为 `false`
- **优先级**: 立即

### D-003: 前端 ESLint 配置缺失
- **位置**: `schemaplexai-ui/`
- **描述**: CI 执行 `npm run lint` 但项目无 `.eslintrc` 配置文件，导致 lint 步骤可能失败
- **建议**: 添加 ESLint 配置文件或移除 CI 中的 lint 步骤
- **优先级**: 立即

---

## P1 — 重要

### D-004: Maven 并行构建未启用
- **位置**: `.github/workflows/ci.yml:35`
- **描述**: `mvn clean compile -q` 未使用 `-T 1C` 并行参数
- **建议**: `mvn clean compile -T 1C -q`（每核心 1 线程）
- **预期收益**: 构建时间减少 30-40%

### D-005: 无 Docker 镜像构建和推送
- **位置**: `.github/workflows/ci.yml`
- **描述**: CI 只做编译和测试，无 Docker 镜像构建步骤
- **建议**: 添加 Docker build + push 步骤（推送到 GHCR 或 Docker Hub）
```yaml
- name: Build Docker images
  run: |
    docker build -t schemaplexai-gateway ./schemaplexai-gateway
    docker build -t schemaplexai-agent-engine ./schemaplexai-agent-engine
```

### D-006: 无数据库迁移管理
- **位置**: 项目根目录
- **描述**: 无 Flyway/Liquibase 配置，数据库 schema 变更靠手动 SQL
- **建议**: 引入 Flyway，SQL 迁移脚本版本化管理
- **优先级**: Sprint 1

### D-007: 无环境分离配置
- **位置**: `application.yml` 文件
- **描述**: 只有默认 profile，无 dev/staging/prod 环境配置分离
- **建议**: 添加 `application-dev.yml`, `application-staging.yml`, `application-prod.yml`
- **优先级**: Sprint 1

### D-008: 无 Dependabot/Renovate 配置
- **位置**: `.github/`
- **描述**: 无自动依赖更新配置，安全漏洞依赖无法自动发现
- **建议**: 添加 `.github/dependabot.yml`
```yaml
version: 2
updates:
  - package-ecosystem: maven
    directory: "/"
    schedule:
      interval: weekly
  - package-ecosystem: npm
    directory: "/schemaplexai-ui"
    schedule:
      interval: weekly
```

---

## P2 — 建议

### D-009: 无部署策略配置
- **描述**: 无蓝绿部署、金丝雀发布、滚动更新配置
- **建议**: 
  - Docker Compose: 使用 `deploy.update_config` 滚动更新
  - K8s: 配置 Deployment strategy + PodDisruptionBudget

### D-010: 监控未集成到 CI
- **描述**: Prometheus/Grafana 在 docker-compose 中但未与 CI/CD 集成
- **建议**: 
  - CI 中添加 smoke test 验证 metrics endpoint
  - 部署后自动验证 Grafana dashboard 数据

### D-011: 无安全扫描步骤
- **描述**: CI 无 SCA（Software Composition Analysis）扫描
- **建议**: 添加 `mvn dependency-check:check` 或 GitHub 的 Dependabot alerts

### D-012: E2E 测试仅 smoke
- **位置**: `.github/workflows/ci.yml:152`
- **描述**: `e2e/smoke.spec.ts` 只覆盖基本页面加载
- **建议**: 逐步扩展 E2E 覆盖核心用户流程（登录、Agent 创建、工作流执行）

### D-013: 无构建缓存优化
- **描述**: Maven 缓存只缓存 `~/.m2/repository`，未缓存构建产物
- **建议**: 使用 `mvn -o`（离线模式）在缓存命中时跳过依赖解析

---

## 现有 CI 配置亮点

| 特性 | 状态 | 说明 |
|------|------|------|
| Maven 依赖缓存 | ✅ | `actions/cache@v4` + pom.xml hash |
| npm 缓存 | ✅ | `setup-node@v4` 内置缓存 |
| JaCoCo 报告生成 | ✅ | CSV + badge 生成 |
| SpotBugs 报告上传 | ✅ | artifact 上传 |
| 安全文件变更检测 | ✅ | PR 时自动检查敏感文件 |
| E2E Playwright | ✅ | 基础 smoke test |
| 测试结果上传 | ✅ | surefire XML 报告 |

---

## 改进路线图

### Phase 1 — 质量门禁加固（立即）
- [ ] D-001: JaCoCo 覆盖率 ≥80% 门禁
- [ ] D-002: SpotBugs/Checkstyle 从 warning 升级为 blocking
- [ ] D-003: ESLint 配置补全

### Phase 2 — 构建优化（Sprint 1）
- [ ] D-004: Maven 并行构建
- [ ] D-005: Docker 镜像构建
- [ ] D-008: Dependabot 配置

### Phase 3 — 部署自动化（Sprint 2）
- [ ] D-006: Flyway 数据库迁移
- [ ] D-007: 环境分离配置
- [ ] D-009: 部署策略
- [ ] D-010: 监控集成

---

## 总结

项目 CI 基础良好（3 个 workflow、缓存、报告上传），但质量门禁形同虚设（SpotBugs/Checkstyle continue-on-error，无 JaCoCo check），且完全缺少 CD（无 Docker 构建、无部署、无环境管理）。建议优先加固质量门禁（Phase 1），再逐步建立部署自动化。
