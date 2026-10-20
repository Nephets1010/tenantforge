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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TenantRegistry} 的边界与语义测试（与 {@code TenantRegistryTest} 互补，聚焦顺序、大小写、一致性等）。
 *
 * <p>为什么测这些：注册表是 store 模块的唯一职责，其查找语义（大小写敏感、插入顺序、实例一致性） 直接决定「按 id 取租户元数据」是否可靠；这些是最容易在重构中悄悄变味的契约点。
 */
class TenantRegistryEdgeCaseTest {

    @Test
    void registerPreservesInsertionOrder() {
        // LinkedHashMap 保序：列表顺序必须等于注册顺序，UI/导出依赖此确定性。
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("c"));
        registry.register(Tenant.of("a"));
        registry.register(Tenant.of("b"));
        List<String> ids = registry.list().stream().map(Tenant::getId).toList();
        assertThat(ids).containsExactly("c", "a", "b");
    }

    @Test
    void containsIsCaseSensitive() {
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("Acme"));
        assertThat(registry.contains("Acme")).isTrue();
        assertThat(registry.contains("acme")).isFalse();
    }

    @Test
    void requireReturnsSameInstanceAsGet() {
        TenantRegistry registry = new TenantRegistry();
        Tenant tenant = Tenant.of("acme");
        registry.register(tenant);
        assertThat(registry.require("acme")).isSameAs(registry.get("acme")).isSameAs(tenant);
    }

    @Test
    void registerTenantWithEmptyProperties() {
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("acme"));
        assertThat(registry.require("acme").getProperties()).isEmpty();
    }

    @Test
    void twoTenantsSameNameDifferentId() {
        // name 仅用于展示，id 才是主键：两个不同 id 但同 name 的租户应各自独立可查。
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("alpha", "Shared"));
        registry.register(Tenant.of("beta", "Shared"));
        assertThat(registry.require("alpha").getId()).isEqualTo("alpha");
        assertThat(registry.require("beta").getId()).isEqualTo("beta");
        assertThat(registry.size()).isEqualTo(2);
    }

    @Test
    void requireAndGetConsistentProperties() {
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("acme", "Acme", Map.of("plan", "pro")));
        assertThat(registry.get("acme").getProperty("plan")).isEqualTo("pro");
        assertThat(registry.require("acme").getProperty("plan")).isEqualTo("pro");
    }

    @Test
    void containsFalseBeforeRegisterTrueAfter() {
        TenantRegistry registry = new TenantRegistry();
        assertThat(registry.contains("x")).isFalse();
        registry.register(Tenant.of("x"));
        assertThat(registry.contains("x")).isTrue();
    }

    @Test
    void registerManyTenantsSizeAccurate() {
        TenantRegistry registry = new TenantRegistry();
        for (int i = 0; i < 20; i++) {
            registry.register(Tenant.of("t" + i));
        }
        assertThat(registry.size()).isEqualTo(20);
        assertThat(registry.list()).hasSize(20);
    }

    @Test
    void getReturnsRegisteredInstance() {
        TenantRegistry registry = new TenantRegistry();
        Tenant tenant = Tenant.of("acme");
        registry.register(tenant);
        assertThat(registry.get("acme")).isSameAs(tenant);
    }

    @Test
    void requireThrowsWithIdInMessage() {
        TenantRegistry registry = new TenantRegistry();
        assertThatThrownBy(() -> registry.require("ghost"))
                .isInstanceOf(TenantUnknownException.class)
                .hasMessageContaining("ghost");
    }
}
