/*
 * Copyright 2026 The tenantforge Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.nephets.tenantforge.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 跨特性集成测试：把「解析器」与「租户上下文」串起来，模拟一次请求内「解析 → 写入上下文 → 业务读取」的链路。
 *
 * <p>为什么测这些：单个解析器/上下文各自的单元测试已充分，但它们之间的契约—— 「解析结果必须能从 {@link TenantContext}
 * 取回、未解析时上下文保持未设置、作用域包裹后正确恢复」—— 只有集成测试能钉死；这是整个库对外价值的关键路径。
 */
class TenantResolverContextIntegrationTest {

    private final HeaderTenantResolver header = new HeaderTenantResolver("X-Tenant-Id");

    private final SubdomainTenantResolver subdomain = new SubdomainTenantResolver();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void headerResolutionFlowsIntoContext() {
        Optional<String> resolved = header.resolve(Map.of("X-Tenant-Id", "acme"));
        assertThat(resolved).contains("acme");
        resolved.ifPresent(TenantContext::setTenant);
        assertThat(TenantContext.requireTenant()).isEqualTo("acme");
    }

    @Test
    void subdomainResolutionFlowsIntoContext() {
        Optional<String> resolved = subdomain.resolve(Map.of("Host", "globex.example.com"));
        assertThat(resolved).contains("globex");
        resolved.ifPresent(TenantContext::setTenant);
        assertThat(TenantContext.requireTenant()).isEqualTo("globex");
    }

    @Test
    void compositeChainPrefersHeaderThenSubdomain() {
        CompositeTenantResolver chain = new CompositeTenantResolver(List.of(header, subdomain));
        Map<String, String> headers = Map.of("X-Tenant-Id", "acme", "Host", "globex.example.com");
        String tenant = chain.resolve(headers).orElseThrow();
        assertThat(tenant).isEqualTo("acme");
        TenantContext.setTenant(tenant);
        assertThat(TenantContext.requireTenant()).isEqualTo("acme");
    }

    @Test
    void compositeFallsBackToSubdomainWhenHeaderMissing() {
        CompositeTenantResolver chain = new CompositeTenantResolver(List.of(header, subdomain));
        Map<String, String> headers = Map.of("Host", "globex.example.com");
        String tenant = chain.resolve(headers).orElseThrow();
        assertThat(tenant).isEqualTo("globex");
        TenantContext.setTenant(tenant);
        assertThat(TenantContext.requireTenant()).isEqualTo("globex");
    }

    @Test
    void resolutionIsScopedViaRunWith() {
        String resolved =
                new CompositeTenantResolver(List.of(header, subdomain))
                        .resolve(Map.of("X-Tenant-Id", "acme"))
                        .orElseThrow();
        TenantContext.runWith(
                resolved, () -> assertThat(TenantContext.requireTenant()).isEqualTo("acme"));
    }

    @Test
    void clearBetweenRequestsIsolatesTenants() {
        TenantContext.setTenant("acme");
        assertThat(TenantContext.requireTenant()).isEqualTo("acme");
        TenantContext.clear();
        TenantContext.setTenant("globex");
        assertThat(TenantContext.requireTenant()).isEqualTo("globex");
    }

    @Test
    void unresolvedRequestLeavesContextUnset() {
        Optional<String> resolved = header.resolve(Map.of("X-Other", "x"));
        assertThat(resolved).isEmpty();
        // 解析为空时不应写入上下文，后续业务读取应如实抛出「未解析」异常。
        assertThatThrownBy(TenantContext::requireTenant)
                .isInstanceOf(TenantNotResolvedException.class);
    }

    @Test
    void runWithRestoresOuterAfterResolutionScope() {
        TenantContext.setTenant("outer");
        TenantContext.runWith(
                "inner",
                () -> {
                    assertThat(TenantContext.requireTenant()).isEqualTo("inner");
                    header.resolve(Map.of("X-Tenant-Id", "inner2"))
                            .ifPresent(TenantContext::setTenant);
                    assertThat(TenantContext.requireTenant()).isEqualTo("inner2");
                });
        assertThat(TenantContext.requireTenant()).isEqualTo("outer");
    }
}
