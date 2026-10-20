# tenantforge

多租户隔离锻造库：纯 Java 的租户上下文 / 解析 / 注册表核心 + Spring Boot 自动装配，按请求解析租户并隔离上下文。

## 模块

tenantforge 采用「契约 / 存储 / 运行时 / 装配 / 示例」五模块分层，每个模块职责单一、可独立构建与测试：

| 模块 | 职责 |
| --- | --- |
| `tenantforge-contract` | 跨模块共享**契约**：租户值对象 `Tenant`、解析器接口 `TenantResolver`、隔离相关异常（`TenantNotResolvedException` / `TenantUnknownException`）。零运行时依赖（仅 JDK 21），是被 core / store / starter 共同依赖的「对外接口面」。 |
| `tenantforge-store` | 租户**存储**：`TenantRegistry` 维护租户元数据（注册 / 查询 / 存在性校验 / 列表）。依赖 contract；零 Spring、零第三方存储框架，仅用 JDK 集合 + slf4j 日志门面。 |
| `tenantforge-core` | 纯 Java（零 Spring）**运行时**：租户上下文 `TenantContext`（InheritableThreadLocal + 虚拟线程显式传播）与各类租户解析器（`Header` / `JWT Claim` / `Subdomain` / `Composite`）。依赖 contract 提供的契约类型，仅额外引入 slf4j-api。 |
| `tenantforge-starter` | Spring Boot **自动装配**：按 `X-Tenant-Id` 等请求头解析租户并写入 `TenantContext`，提供 `TenantRegistry` / `TenantResolver` / 拦截器 Bean，并支持默认租户。 |
| `tenantforge-sample` | 示例 Web 应用：注册 acme / globex 两个租户，提供 `/whoami` 端点回显当前租户，端到端演示整条链路。 |

> 包名说明：拆分后 `Tenant`、`TenantResolver`、各解析器等仍位于包 `io.github.nephets.tenantforge.core`，
> 全限定类名（包名 + 类名）保持不变——只移动了物理模块目录与对应 pom 依赖，避免对已有调用方造成任何 API 层面的破坏。

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

`tenantforge-core` / `tenantforge-store` / `tenantforge-contract` 严禁依赖 `org.springframework.*`
（CI 的 `architecture-boundaries` job 强制扫描，违反即失败）。核心运行时保持零 Spring、零强运行时第三方依赖，
仅通过 slf4j-api 日志门面与宿主应用解耦。

## 设计决策

本节记录关键设计背后的「为什么」，而不是「是什么」。

1. **为什么拆成 contract / store / core 三个内核模块，而不是一个大 core？**
   租户系统的概念天然分三层：对外契约（值对象、接口、异常）、租户元数据的存储、以及请求期的解析与上下文隔离。
   把契约独立成 `tenantforge-contract`，让 store 与 core 都只依赖它而不互相依赖，使「接口面」与「实现」解耦；
   调用方若只想要类型与异常（如写自己的解析器），只需依赖 contract，无需引入运行时。

2. **为什么物理拆模块却保留原包名 `io.github.nephets.tenantforge.core`？**
   模块拆分是「物理目录 + pom 依赖」的重组，而全限定类名是稳定的公共契约。保持包名不变，
   已有调用方的 `import` 与二进制兼容性完全不受影响——这是一次零 API 破坏的重构，也是本战役对所有仓的统一约束。

3. **为什么 `TenantContext` 同时用 `InheritableThreadLocal` 又提供 `propagateToVirtualThread`？**
   `InheritableThreadLocal` 能在「创建子线程」的瞬间把父线程租户上下文继承过去，覆盖大多数同步派生的场景；
   但它的继承只发生在**线程创建那一刻**。若任务被提交到早已创建好的线程池载体线程或复用中的 executor，
   继承并不发生。因此额外提供 `propagateToVirtualThread`——在任务真正**执行时**重新设置上下文并在结束后恢复，
   与线程种类（平台线程 / 虚拟线程 / 线程池）无关。名字里的「虚拟线程」只是动机场景，不是使用限制。

4. **为什么 JWT Claim 解析器只解码 payload、绝不验签？**
   签名校验是网关 / 认证层的职责；把它放进 core 会迫使 core 引入 JWT 库，破坏「零第三方依赖」的底线。
   本解析器刻意只做 base64url 解码 + 朴素字段定位，且文档明确「仅应在上游已完成鉴权的链路中使用」，把信任边界外推。

5. **为什么 JWT 的 claim 提取用「朴素字符串扫描」而不是通用 JSON 解析器？**
   同样为了 core 零第三方依赖：不引入任何 JSON 库，只做「够用」的 `"key"` 字面量定位 + 字符串转义解码。
   其边界（可能误命中另一字符串值内部的同名子串、只取字符串类型值、不做嵌套解析）已在代码与测试中被显式钉死，
   因此对「结构已知、上游已鉴权」的 JWT payload 是安全且充分的。

6. **为什么 `TenantRegistry` 用内存 `LinkedHashMap` 且刻意不做线程安全？**
   租户元数据是「启动时注册一次、之后只读」的元数据，并非高频并发写场景；用 `LinkedHashMap` 既保留注册顺序
   （供列表/导出确定性）又零并发开销。对其做 `synchronized` 包裹只会无谓地拖慢读路径。
   非线程安全是**有意为之**并被测试如实记录（单线程语义），需要热更新时应由调用方在外层加锁，而不是污染内核。

7. **为什么解析器返回 `Optional<String>`、组合解析器取「第一个非空」？**
   `Optional` 把「没解析到」与「解析到空串」统一成显式的空结果，避免用 `null` 表达缺失；
   `CompositeTenantResolver` 按序尝试、返回第一个非空，使「Header 优先、Subdomain 兜底、JWT 再兜底」这类优先级
   仅靠列表顺序即可表达，无需任何配置开关——组合优于配置。

8. **为什么 core / store 只引入 slf4j-api（日志门面）、不引入任何绑定/实现？**
   日志绑定（logback/log4j）应由最终宿主应用决定。内核只依赖门面，保证「零运行时强依赖」，
   宿主应用把自己的绑定放进 classpath 即可，不会与内核发生日志实现冲突。

9. **为什么 core / store / contract 严禁 `org.springframework.*`？**
   多租户隔离的核心机制本就与 Web 框架无关（可在网关、定时任务、RPC 框架中复用）。
   把 Spring 依赖挡在内核之外，既保证了可移植性，也用 CI 的架构扫描把这条边界「机器化」守住，防止演进中悄悄耦合。
