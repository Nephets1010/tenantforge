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
package io.github.nephets.tenantforge.sample;

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
 * 非 Spring 的纯逻辑集成测试：验证示例应用所用的核心机制（注册表 + 解析器 + 上下文）能独立工作。
 *
 * <p>为什么测这些：示例应用的 {@code @SpringBootTest} 套件偏重 HTTP 链路且启动慢；这里用纯单元级集成 把「注册表存租户 → 解析器取 id → 上下文携带 →
 * 注册表回查」这条与 {@code /whoami} 等价的逻辑单独钉死， 作为慢测试的快速补充，且明确「已知/未知租户」的判定边界。
 */
class TenantResolutionPlainTest {

    private final TenantRegistry registry = new TenantRegistry();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void registryStoresSeededTenants() {
        registry.register(Tenant.of("acme", "Acme Corporation"));
        registry.register(Tenant.of("globex", "Globex"));
        assertThat(registry.require("acme").getName()).isEqualTo("Acme Corporation");
        assertThat(registry.require("globex").getName()).isEqualTo("Globex");
    }

    @Test
    void headerResolutionThenRegistryLookup() {
        registry.register(Tenant.of("acme", "Acme Corporation"));
        String id =
                new HeaderTenantResolver("X-Tenant-Id")
                        .resolve(Map.of("X-Tenant-Id", "acme"))
                        .orElseThrow();
        TenantContext.setTenant(id);
        assertThat(registry.require(TenantContext.requireTenant()).getName())
                .isEqualTo("Acme Corporation");
    }

    @Test
    void unknownTenantNotInRegistryThrows() {
        assertThatThrownBy(() -> registry.require("ghost"))
                .isInstanceOf(TenantUnknownException.class);
    }

    @Test
    void compositeChainFallsBackToSubdomain() {
        registry.register(Tenant.of("globex", "Globex"));
        CompositeTenantResolver chain =
                new CompositeTenantResolver(
                        List.of(
                                new HeaderTenantResolver("X-Tenant-Id"),
                                new SubdomainTenantResolver()));
        String id = chain.resolve(Map.of("Host", "globex.example.com")).orElseThrow();
        assertThat(id).isEqualTo("globex");
        TenantContext.setTenant(id);
        assertThat(registry.require(TenantContext.requireTenant()).getName()).isEqualTo("Globex");
    }

    @Test
    void contextRequireAfterResolve() {
        String id =
                new HeaderTenantResolver("X-Tenant-Id")
                        .resolve(Map.of("X-Tenant-Id", "acme"))
                        .orElseThrow();
        TenantContext.setTenant(id);
        assertThat(TenantContext.requireTenant()).isEqualTo("acme");
    }
}
