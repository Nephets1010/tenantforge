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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Tenant} 值对象的边界与契约测试。
 *
 * <p>为什么测这些：{@code Tenant} 是整库对外的「租户身份」载体，其不可变性、null/blank 防御、 properties 不可变拷贝、equals/hashCode
 * 契约直接决定注册表查找与跨模块传递的正确性，必须被钉死。
 */
class TenantValueObjectTest {

    @Test
    void ofWithIdOnlyDefaultsNameToId() {
        // 只给 id 时，可读性默认回退到 id，避免 name 为 null 导致下游 NPE。
        Tenant t = Tenant.of("acme");
        assertThat(t.getId()).isEqualTo("acme");
        assertThat(t.getName()).isEqualTo("acme");
    }

    @Test
    void ofWithNullNameDefaultsToId() {
        Tenant t = Tenant.of("acme", null);
        assertThat(t.getName()).isEqualTo("acme");
    }

    @Test
    void ofWithNullPropertiesYieldsEmptyMap() {
        Tenant t = Tenant.of("acme", "Acme", null);
        assertThat(t.getProperties()).isEmpty();
    }

    @Test
    void getPropertyReturnsDefaultWhenAbsent() {
        Tenant t = Tenant.of("acme");
        assertThat(t.getProperty("plan", "free")).isEqualTo("free");
    }

    @Test
    void getPropertyReturnsValueWhenPresent() {
        Tenant t = Tenant.of("acme", "Acme", Map.of("plan", "pro"));
        assertThat(t.getProperty("plan")).isEqualTo("pro");
        assertThat(t.getProperty("plan", "free")).isEqualTo("pro");
    }

    @Test
    void equalsTrueWhenIdNameAndPropertiesMatch() {
        Tenant a = Tenant.of("acme", "Acme", Map.of("k", "1"));
        Tenant b = Tenant.of("acme", "Acme", Map.of("k", "1"));
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equalsFalseWhenNameDiffersDespiteSameId() {
        // equals 同时比较 name，因此同 id 不同 name 视为不同租户（语义上 id 才是主键）。
        Tenant a = Tenant.of("acme", "Acme");
        Tenant b = Tenant.of("acme", "ACME");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsFalseWhenPropertiesDiffer() {
        Tenant a = Tenant.of("acme", "Acme", Map.of("k", "1"));
        Tenant b = Tenant.of("acme", "Acme", Map.of("k", "2"));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsFalseForNull() {
        assertThat(Tenant.of("acme")).isNotEqualTo(null);
    }

    @Test
    void equalsFalseForUnrelatedType() {
        assertThat(Tenant.of("acme")).isNotEqualTo("acme");
    }

    @Test
    void hashCodeConsistentForEqualTenants() {
        Tenant a = Tenant.of("acme", "Acme", Map.of("k", "1"));
        Tenant b = Tenant.of("acme", "Acme", Map.of("k", "1"));
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toStringContainsId() {
        assertThat(Tenant.of("acme").toString()).contains("acme");
    }

    @Test
    void propertiesIsUnmodifiableCopy() {
        // Map.copyOf 生成不可变视图，外部无法污染内部状态（防御性拷贝）。
        Map<String, String> source = new HashMap<>();
        source.put("k", "v");
        Tenant t = Tenant.of("acme", "Acme", source);
        assertThatThrownBy(() -> t.getProperties().put("k2", "v2"))
                .isInstanceOf(UnsupportedOperationException.class);
        // 修改外部 source 不应影响已构造的租户。
        source.put("k", "hacked");
        assertThat(t.getProperty("k")).isEqualTo("v");
    }

    @Test
    void blankIdRejected() {
        assertThatThrownBy(() -> Tenant.of("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void nullIdRejected() {
        assertThatThrownBy(() -> new Tenant(null, "acme", Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("不能为 null");
    }

    @Test
    void getPropertyWithNullDefaultReturnsNullWhenAbsent() {
        assertThat(Tenant.of("acme").getProperty("missing", null)).isNull();
    }
}
