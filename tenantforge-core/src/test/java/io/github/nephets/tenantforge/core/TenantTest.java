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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantTest {

    @Test
    void ofWithIdDefaultsNameToId() {
        Tenant tenant = Tenant.of("acme");
        assertThat(tenant.getId()).isEqualTo("acme");
        assertThat(tenant.getName()).isEqualTo("acme");
    }

    @Test
    void ofWithIdAndName() {
        Tenant tenant = Tenant.of("acme", "Acme Inc");
        assertThat(tenant.getId()).isEqualTo("acme");
        assertThat(tenant.getName()).isEqualTo("Acme Inc");
    }

    @Test
    void ofWithProperties() {
        Tenant tenant = Tenant.of("acme", "Acme", Map.of("region", "us"));
        assertThat(tenant.getProperty("region")).isEqualTo("us");
        assertThat(tenant.getProperty("missing", "def")).isEqualTo("def");
    }

    @Test
    void nullIdRejected() {
        assertThatThrownBy(() -> new Tenant(null, "n", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void blankIdRejected() {
        assertThatThrownBy(() -> new Tenant("  ", "n", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullNameDefaultsToId() {
        Tenant tenant = new Tenant("acme", null, null);
        assertThat(tenant.getName()).isEqualTo("acme");
    }

    @Test
    void nullPropertiesBecomeEmptyMap() {
        Tenant tenant = new Tenant("acme", "n", null);
        assertThat(tenant.getProperties()).isEmpty();
    }

    @Test
    void propertiesAreImmutableCopy() {
        Map<String, String> src = new java.util.HashMap<>();
        src.put("k", "v");
        Tenant tenant = new Tenant("acme", "n", src);
        src.put("k", "changed");
        assertThat(tenant.getProperty("k")).isEqualTo("v");
    }

    @Test
    void getPropertyReturnsValue() {
        Tenant tenant = Tenant.of("a", "A", Map.of("tier", "gold"));
        assertThat(tenant.getProperty("tier")).isEqualTo("gold");
    }

    @Test
    void getPropertyDefaultWhenAbsent() {
        Tenant tenant = Tenant.of("a");
        assertThat(tenant.getProperty("x", "fallback")).isEqualTo("fallback");
    }

    @Test
    void equalsBasedOnIdNameProperties() {
        assertThat(Tenant.of("a", "A", Map.of("k", "1")))
                .isEqualTo(Tenant.of("a", "A", Map.of("k", "1")));
        assertThat(Tenant.of("a", "A")).isNotEqualTo(Tenant.of("a", "B"));
        assertThat(Tenant.of("a")).isNotEqualTo(Tenant.of("b"));
    }

    @Test
    void hashCodeConsistentWithEquals() {
        assertThat(Tenant.of("a", "A").hashCode()).isEqualTo(Tenant.of("a", "A").hashCode());
    }

    @Test
    void toStringContainsId() {
        assertThat(Tenant.of("acme").toString()).contains("acme");
    }

    @Test
    void fullConstructorStoresAll() {
        Tenant tenant = new Tenant("id1", "Name1", Map.of("a", "b"));
        assertThat(tenant.getId()).isEqualTo("id1");
        assertThat(tenant.getName()).isEqualTo("Name1");
        assertThat(tenant.getProperty("a")).isEqualTo("b");
    }
}
