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
package io.github.nephets.tenantforge.starter;

import io.github.nephets.tenantforge.core.CompositeTenantResolver;
import io.github.nephets.tenantforge.core.HeaderTenantResolver;
import io.github.nephets.tenantforge.core.SubdomainTenantResolver;
import io.github.nephets.tenantforge.core.Tenant;
import io.github.nephets.tenantforge.core.TenantContext;
import io.github.nephets.tenantforge.core.TenantRegistry;
import io.github.nephets.tenantforge.core.TenantUnknownException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 跨模块全栈集成测试：把 store（TenantRegistry）、contract/core（解析器、TenantContext）拼成完整链路， 不依赖 Spring，直接验证「解析 →
 * 写入上下文 → 按上下文回查注册表」这一对外核心价值路径。
 *
 * <p>为什么测这些：模块拆分后，各模块的单元测试各自绿，但「解析出的租户 id 真能在注册表里查到元数据」这一端到端契约， 只有跨模块的集成测试能钉死；它是调用方最关心的 guarantees。
 */
class TenantStackIntegrationTest {

    private final TenantRegistry registry = new TenantRegistry();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void resolvedHeaderTenantIsLookupableInRegistry() {
        registry.register(Tenant.of("acme", "Acme Corporation"));
        registry.register(Tenant.of("globex", "Globex"));
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        String tenant = resolver.resolve(Map.of("X-Tenant-Id", "acme")).orElseThrow();
        TenantContext.setTenant(tenant);
        assertThat(TenantContext.requireTenant()).isEqualTo("acme");
        assertThat(registry.require(TenantContext.requireTenant()).getName())
                .isEqualTo("Acme Corporation");
    }

    @Test
    void compositeChainResolvesHeaderThenRegistry() {
        registry.register(Tenant.of("acme", "Acme Corporation"));
        CompositeTenantResolver chain =
                new CompositeTenantResolver(
                        List.of(
                                new HeaderTenantResolver("X-Tenant-Id"),
                                new SubdomainTenantResolver()));
        String tenant =
                chain.resolve(Map.of("X-Tenant-Id", "acme", "Host", "globex.example.com"))
                        .orElseThrow();
        TenantContext.setTenant(tenant);
        assertThat(tenant).isEqualTo("acme");
        assertThat(registry.require(TenantContext.requireTenant()).getName())
                .isEqualTo("Acme Corporation");
    }

    @Test
    void subdomainResolutionFlowsToRegistry() {
        registry.register(Tenant.of("globex", "Globex"));
        SubdomainTenantResolver resolver = new SubdomainTenantResolver();
        String tenant = resolver.resolve(Map.of("Host", "globex.example.com")).orElseThrow();
        TenantContext.setTenant(tenant);
        assertThat(registry.require(TenantContext.requireTenant()).getName()).isEqualTo("Globex");
    }

    @Test
    void resolvedButUnregisteredTenantThrowsOnRegistryLookup() {
        // 解析成功但注册表里没有该租户：上下文里有值，但回查元数据必须如实抛 Unknown。
        TenantContext.setTenant("ghost");
        assertThatThrownBy(() -> registry.require(TenantContext.requireTenant()))
                .isInstanceOf(TenantUnknownException.class);
    }

    @Test
    void runWithScopesRegistryLookup() throws Exception {
        registry.register(Tenant.of("acme", "Acme Corporation"));
        String tenant =
                new HeaderTenantResolver("X-Tenant-Id")
                        .resolve(Map.of("X-Tenant-Id", "acme"))
                        .orElseThrow();
        // withTenant 返回包裹内计算结果，验证「作用域内核取的租户能回查到注册表元数据」。
        String name =
                TenantContext.withTenant(
                        tenant, () -> registry.require(TenantContext.requireTenant()).getName());
        assertThat(name).isEqualTo("Acme Corporation");
    }

    @Test
    void contextIsolationBetweenSequentialRequests() {
        registry.register(Tenant.of("acme", "Acme Corporation"));
        registry.register(Tenant.of("globex", "Globex"));
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");

        TenantContext.setTenant(resolver.resolve(Map.of("X-Tenant-Id", "acme")).orElseThrow());
        assertThat(registry.require(TenantContext.requireTenant()).getName())
                .isEqualTo("Acme Corporation");
        TenantContext.clear();

        TenantContext.setTenant(resolver.resolve(Map.of("X-Tenant-Id", "globex")).orElseThrow());
        assertThat(registry.require(TenantContext.requireTenant()).getName()).isEqualTo("Globex");
    }
}
