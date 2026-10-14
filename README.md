# tenantforge

多租户隔离锻造库：纯 Java 的租户上下文 / 解析 / 注册表核心 + Spring Boot 自动装配，按请求解析租户并隔离上下文。

## 模块

| 模块 | 说明 |
| --- | --- |
| `tenantforge-core` | 零 Spring 核心：租户上下文（ThreadLocal）、租户解析（Header / 组合）、租户注册表与隔离异常。仅依赖 JDK 21。 |
| `tenantforge-starter` | Spring Boot 自动装配：按 `X-Tenant-Id` 请求头解析租户并写入 `TenantContext`，提供 `TenantRegistry` / `TenantResolver` / 拦截器 Bean。 |
| `tenantforge-sample` | 示例 Web 应用：注册 acme / globex 两个租户，提供 `/whoami` 端点回显当前租户。 |

## 快速开始

```yaml
tenantforge:
  header-name: X-Tenant-Id
  default-tenant-id: anonymous   # 可选
```

```java
@RestController
public class Api {
  @GetMapping("/whoami")
  public String whoami() {
    return TenantContext.requireTenant();
  }
}
```

## 构建

```bash
./mvnw -B verify
```

## 架构约束

`tenantforge-core` 严禁依赖 `org.springframework.*`（CI 的 `architecture-boundaries` job 强制扫描）。
