---
topic: api-gateway
stage: decision
version: v1.0
status: 已批准
supersedes: ""
---

# ADR-007: 增加 API Gateway 层

> **日期**: 2026-04-29
> **决策人**: 架构评审委员会
> **状态**: 已批准

---

## 背景

原设计缺少统一入口，鉴权/限流/日志分散在各业务服务中，导致：
- 每个服务重复实现 JWT 校验、租户解析
- 限流策略不统一，无法做全局流量管控
- 请求/响应日志分散，审计困难
- SSE/WebSocket 代理无标准方案

## 决策

引入 **Spring Cloud Gateway** 作为统一 API 网关层。

## 理由

1. **统一鉴权减少下游重复逻辑**：JWT 校验在 Gateway 完成，下游服务直接消费解析后的用户信息
2. **集中限流更易管控**：基于 Redis 的滑动窗口限流（租户级/API 级/IP 级）统一在 Gateway 实现
3. **SSE/WebSocket 代理标准化**：Gateway 支持长连接透传，避免各服务自行处理
4. **日志审计集中化**：请求/响应日志统一记录到 Elasticsearch，便于安全审计

## 影响

- **正面**：减少下游代码重复、统一流量管控、集中安全策略
- **负面**：增加 1 个可部署单元；所有外部请求增加一跳网络延迟（~1-2ms）
- **缓解**：Gateway 无状态设计，可水平扩展；本地缓存热点路由配置

## 替代方案

| 方案 | 评估 | 结论 |
|------|------|------|
| Kong | 功能完善，但引入额外技术栈（Lua）和学习成本 | 拒绝 |
| APISIX | 基于 Envoy，性能优秀，但社区相对较小 | 拒绝 |
| Nginx + Lua | 轻量，但自研 Lua 插件维护成本高 | 拒绝 |
| **Spring Cloud Gateway（本方案）** | 与现有 Spring Cloud 生态一致，Java 团队可维护 | **采纳** |

## 相关文档

- `docs/designs/2026-04-29-v1.1-system-architecture.md`
- `docs/specs/2026-04-30-v1.0-api-gateway.md`
