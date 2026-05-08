---
topic: notification-v1-channel-reduction
stage: decision
version: v1.0
status: 已批准
supersedes: ""
---

# ADR-013: Notification v1 Channel Reduction

> **日期**: 2026-05-08
> **决策人**: 架构评审委员会
> **状态**: 已批准

---

## 背景

SchemaPlexAI 通知系统原设计支持多通道：in-app（应用内）、email、SMS、webhook。当前 v1 开发状态：

- **in-app 通知**：已完成，支持消息中心、未读红点、站内信
- **email 通道**：SMTP 配置框架完成，但模板引擎、退信处理、垃圾邮件评分未实现
- **SMS 通道**：阿里云 SMS SDK 已引入，但签名审核、模板审核、频率限制未配置
- **webhook 通道**：HTTP 推送框架完成，但重试策略、签名验证、IP 白名单未实现

同时，安全评审提出以下 concerns：
- email 通道：SMTP 凭证管理、钓鱼邮件风险、退信泄露信息
- SMS 通道：短信轰炸风险、国际号码验证、费用失控
- webhook 通道：SSRF 攻击面、 payload 签名验证不完整

## 决策

v1 通知系统**仅保留 in-app 通道**，email / SMS / webhook 通道的实现代码保留但路由至 **DLQ（死信队列）**，待 v1.1 安全加固后启用。

### 实现细节

1. **v1 可用通道**
   - in-app：完全可用，支持实时推送（WebSocket）+ 消息中心

2. **降级通道处理**
   - email / SMS / webhook 的消息仍允许入队（保持 API 兼容性）
   - 消费者识别通道类型后，直接路由至 `notification.dlq`（死信队列）
   - DLQ 消息持久化 30 天，支持 v1.1 批量重放

3. **API 兼容性**
   - 通知发送 API 仍接受 `channels: ["in-app", "email", "sms", "webhook"]` 参数
   - 非 in-app 通道在响应中返回 `status: "deferred"`，不报错
   - 前端通道选择器仅展示 "应用内通知"，隐藏其他选项

4. **v1.1 恢复条件**
   - email：SMTP 凭证迁入 Vault、模板审核流程、退信处理完成
   - SMS：签名/模板审核通过、频率限制（每分钟/每小时/每天）实现
   - webhook：SSRF 防护、HMAC-SHA256 签名验证、重试指数退避完成
   - 安全评审重新通过

## 理由

- **安全优先**：未加固的 email/SMS/webhook 通道存在已知安全风险，v1 上线不可接受
- **上线时间**：in-app 已满足 80% 用户场景（任务完成提醒、审批通知、系统公告）
- **向后兼容**：API 不破坏，下游系统调用不受影响，仅实际投递行为改变
- **可恢复性**：DLQ 保留消息，v1.1 安全加固后可批量重放，无数据丢失

## 影响

- **对现有代码的影响**
  - `schemaplexai-ops` 通知消费者增加通道类型判断逻辑
  - 新增 DLQ 队列 `notification.dlq`（RabbitMQ）
  - 移除前端通道选择器的 email/SMS/webhook 选项

- **对下游系统的影响**
  - 期望接收 email/SMS/webhook 通知的下游系统（如外部审批系统、企业微信机器人）在 v1 期间收不到通知
  - 需在 v1 发布说明中明确告知，建议下游系统轮询 in-app 消息 API 作为替代

- **对运维的影响**
  - 监控 DLQ 积压量，防止异常增长
  - 30 天过期策略确保 DLQ 不会无限膨胀

## 替代方案

| 方案 | 优点 | 缺点 | 结果 |
|------|------|------|------|
| 赶工完成所有通道 | 功能完整 | 安全风险未消除，可能延期 | 拒绝 |
| 禁用非 in-app 通道（直接丢弃） | 实现最简单 | 消息丢失，下游系统无法恢复 | 拒绝 |
| **路由至 DLQ（本方案）** | 安全、兼容、可恢复 | 下游系统 v1 期间无通知 | **采纳** |
| 仅开放 webhook（风险最低的外部通道） | 部分外部集成可用 | SSRF 风险仍存在，安全评审不通过 | 拒绝 |

## 相关文档

- `docs/designs/notification-architecture.md`
- `docs/specs/v1.1-notification-channels-spec.md`
- ADR-011: JWT Key Rotation SLA（安全评审流程参考）
