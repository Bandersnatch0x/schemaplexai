---
topic: tdd-guide
stage: standard
version: v1.0
status: approved
---

# TDD 指南（Test-Driven Development）

> 先写测试，再写实现。没有测试的代码不允许提交。

---

## 1. TDD 循环

```
RED  →  GREEN  →  REFACTOR
写测试    写实现    重构优化
（失败）  （通过）  （保持通过）
```

## 2. 测试类型矩阵

| 类型 | 范围 | 工具 | 覆盖率要求 | 执行速度 |
|------|------|------|-----------|---------|
| **单元测试** | 单个类/方法 | JUnit 5 + Mockito | >= 80% | < 1s |
| **集成测试** | 模块内多组件 | Spring Boot Test + Testcontainers | 核心链路覆盖 | < 30s |
| **契约测试** | API 接口 | REST Assured | 所有端点 | < 10s |
| **E2E 测试** | 关键用户流程 | Playwright | 黄金路径 | 分钟级 |

## 3. 单元测试规范（Java）

### 3.1 结构（AAA Pattern）

```java
@Test
void shouldReturnEmptyArrayWhenNoMarketsMatchQuery() {
    // Arrange
    String query = "nonexistent";
    List<Market> allMarkets = List.of();

    // Act
    List<Market> result = marketService.search(query, allMarkets);

    // Assert
    assertThat(result).isEmpty();
}
```

### 3.2 命名规范

```java
// GOOD: 描述行为，不是方法名
test('returns empty array when no markets match query')
test('throws error when API key is missing')
test('falls back to substring search when Redis is unavailable')

// BAD: 仅复述方法名
test('search() returns empty list')
test('search() throws exception')
```

### 3.3 禁止

- 不写测试直接提交代码
- 测试依赖外部真实服务（数据库/Redis/MQ 用 Testcontainers 或 Mockito）
- 测试之间共享可变状态
- 为了凑覆盖率而写无意义的测试

## 4. 集成测试规范

- 使用 `@SpringBootTest` + `Testcontainers`
- 每个测试类独立启动容器，测试结束后清理数据
- 禁止测试数据污染其他测试（@Transactional 或手动清理）

## 5. 前端测试规范（TypeScript/React）

- **框架**：Vitest + React Testing Library
- **范围**：Store 逻辑、工具函数、关键组件交互
- **禁止**：测试实现细节（如内部 state）、过度使用 snapshot

## 6. CI 门禁

```yaml
# .github/workflows/ci.yml 阶段
1. mvn test                          # 所有测试必须通过
2. mvn jacoco:check                  # 单元覆盖率 >= 80%
3. npm run test:run                  # 前端测试通过
4. mvn spotbugs:check                # 静态分析（non-blocking）
5. mvn checkstyle:check              # 代码风格（non-blocking）
```

## 7. 测试基础设施

### 7.1 后端依赖（pom.xml）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### 7.2 前端依赖（package.json）

```json
{
  "devDependencies": {
    "vitest": "^1.x",
    "@testing-library/react": "^14.x",
    "@testing-library/jest-dom": "^6.x",
    "jsdom": "^24.x"
  }
}
```
