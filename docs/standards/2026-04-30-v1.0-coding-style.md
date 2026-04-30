---
topic: coding-style
stage: standard
version: v1.0
status: 已批准
supersedes: ""
---

# 编码规范

> **主题**: `coding-style`
> **阶段**: `standard`
> **版本**: v1.0
> **状态**: 已批准
> **日期**: 2026-04-30

---

## 1. 不可变性（核心原则）

ALWAYS 创建新对象，NEVER 修改现有对象：

```java
// WRONG: 修改传入的对象
public void updateUser(User user, String name) {
    user.setName(name);  // 副作用！
}

// CORRECT: 返回新对象
public User updateUser(User user, String name) {
    return User.builder()
        .from(user)
        .name(name)
        .build();
}
```

## 2. 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `AgentStateMachine`, `NotificationService` |
| 接口 | PascalCase，形容词/能力 | `AgentStateHandler`, `ToolAdapter` |
| 方法/变量 | camelCase | `markAsRead()`, `unreadCount` |
| 常量 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE`, `DEFAULT_TTL` |
| 布尔值 | is/has/should/can 前缀 | `isRead`, `hasPermission` |
| 包名 | 全小写，单数 | `com.schemaplexai.web.controller` |
| 数据库表 | sf_前缀，下划线分隔 | `sf_agent_execution` |
| 枚举 | PascalCase，值 UPPER_SNAKE_CASE | `AgentExecutionState.THINKING` |

## 3. 文件组织

- **200-400 行** 为理想文件大小，**800 行** 为硬性上限
- **按功能/领域组织**，不按类型（`notification/` 而非 `controllers/` + `services/`）
- 每个类单一职责，超过 800 行强制拆分

## 4. 函数规范

- **50 行** 为上限，超过必须拆分
- 参数不超过 **4 个**，超过使用 Builder 或 DTO
- 使用 early return 避免深层嵌套（> 4 层强制重构）

```java
// 推荐：early return
public Result<NotificationVO> markAsRead(Long id) {
    Notification notification = notificationMapper.selectById(id);
    if (notification == null) {
        return error(ErrorCode.NOT_FOUND);
    }
    if (!notification.getUserId().equals(currentUserId())) {
        return error(ErrorCode.NOT_FOUND);  // 避免信息泄露，统一返回 404
    }
    notificationMapper.updateReadStatus(id, true);
    return success(toVO(notification));
}
```

## 5. 异常处理

- 使用 `BaseException` 或子类，禁止抛出裸 `RuntimeException`
- 错误码使用整数，按模块分段（1000-1999: system, 2000-2999: agent, ...）
- 异常信息中不得包含敏感数据（密码、Token、密钥）
- Controller 层统一捕获并包装为 `Result<T>`

## 6. 注释规范

- **禁止**写 "What" 注释（代码本身应说明）
- **必须**写 "Why" 注释（隐藏约束、业务规则、 hack/workaround）
- 公共 API 使用 JavaDoc，说明参数、返回值、异常

```java
// 禁止：注释描述代码在做什么
// 增加 1
i++;

// 推荐：注释说明为什么这么做
// 乐观锁重试：高并发下 CAS 失败概率约 5%，重试 3 次覆盖 99.9% 场景
for (int retry = 0; retry < 3; retry++) {
    ...
}
```

## 7. 日志规范

- 使用 SLF4J，禁止 `System.out.println`
- 关键路径 INFO，调试 DEBUG，错误 ERROR
- 日志中包含 `traceId` 和 `tenantId`（MDC）
- 禁止在日志中打印完整 SQL 结果集（可能非常大）

## 8. 测试规范

- 测试类命名：`XxxTest`
- 测试方法：描述行为，而非方法名
  - 推荐：`shouldReturn404WhenNotificationNotFound()`
  - 禁止：`testMarkAsRead()`
- 使用 AAA 模式：Arrange → Act → Assert

## 9. 相关文档

- `docs/standards/tdd-guide.md` — TDD 实践指南
- `docs/standards/feature-workflow.md` — 功能开发流程
- `docs/standards/fix-workflow.md` — Bug 修复流程
