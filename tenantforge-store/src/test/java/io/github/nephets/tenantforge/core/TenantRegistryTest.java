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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantRegistryTest {

    @Test
    void registerStoresTenant() {
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("acme", "Acme"));
        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.contains("acme")).isTrue();
        assertThat(registry.get("acme").getName()).isEqualTo("Acme");
    }

    @Test
    void requireReturnsExisting() {
        TenantRegistry registry = new TenantRegistry();
        Tenant tenant = Tenant.of("acme");
        registry.register(tenant);
        assertThat(registry.require("acme")).isEqualTo(tenant);
    }

    @Test
    void requireThrowsForMissing() {
        TenantRegistry registry = new TenantRegistry();
        assertThatThrownBy(() -> registry.require("ghost"))
                .isInstanceOf(TenantUnknownException.class);
    }

    @Test
    void duplicateRegistrationRejected() {
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("acme"));
        assertThatThrownBy(() -> registry.register(Tenant.of("acme")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("acme");
    }

    @Test
    void nullTenantRejected() {
        TenantRegistry registry = new TenantRegistry();
        assertThatThrownBy(() -> registry.register(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void getMissingReturnsNull() {
        assertThat(new TenantRegistry().get("missing")).isNull();
    }

    @Test
    void containsMissingFalse() {
        assertThat(new TenantRegistry().contains("missing")).isFalse();
    }

    @Test
    void listReturnsRegisteredTenants() {
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("a"));
        registry.register(Tenant.of("b"));
        List<Tenant> list = registry.list();
        assertThat(list).hasSize(2);
        assertThat(list.stream().map(Tenant::getId)).containsExactly("a", "b");
    }

    @Test
    void listIsImmutable() {
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("a"));
        List<Tenant> list = registry.list();
        assertThatThrownBy(() -> list.add(Tenant.of("b")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyRegistrySizeZero() {
        assertThat(new TenantRegistry().size()).isZero();
        assertThat(new TenantRegistry().list()).isEmpty();
    }

    @Test
    void multipleTenantsIndependent() {
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("a", "A", java.util.Map.of("k", "1")));
        registry.register(Tenant.of("b", "B", java.util.Map.of("k", "2")));
        assertThat(registry.require("a").getProperty("k")).isEqualTo("1");
        assertThat(registry.require("b").getProperty("k")).isEqualTo("2");
    }

    @Test
    void requireMessageContainsId() {
        TenantRegistry registry = new TenantRegistry();
        try {
            registry.require("nope");
            org.assertj.core.api.Assertions.fail("expected exception");
        } catch (TenantUnknownException e) {
            assertThat(e.getMessage()).contains("nope");
        }
    }

    @Test
    void registerThenHasTrue() {
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("x"));
        assertThat(registry.contains("x")).isTrue();
    }

    @Test
    void reregisterDifferentIdSucceeds() {
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("a"));
        registry.register(Tenant.of("b"));
        assertThat(registry.size()).isEqualTo(2);
    }

    @Test
    void getAfterRegisterReturnsSame() {
        TenantRegistry registry = new TenantRegistry();
        Tenant t = Tenant.of("a");
        registry.register(t);
        assertThat(registry.get("a")).isSameAs(t);
    }

    @Test
    void unknownRequireIsRuntimeException() {
        TenantRegistry registry = new TenantRegistry();
        assertThatThrownBy(() -> registry.require("z")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void registerWithProperties() {
        TenantRegistry registry = new TenantRegistry();
        registry.register(Tenant.of("acme", "Acme", java.util.Map.of("plan", "pro")));
        assertThat(registry.require("acme").getProperty("plan")).isEqualTo("pro");
    }
}
