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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SubdomainTenantResolver} 的深层边界测试（与 {@code SubdomainTenantResolverTest} 互补）。
 *
 * <p>为什么测这些：主机名归一化（去端口、去结尾点、转小写）与「最左标签是否落入系统标签集合」的组合， 是判定「有没有子域租户」的核心逻辑；端口+结尾点+深层子域的叠加情形最容易在重构时回归。
 */
class SubdomainTenantResolverExtraTest {

    @Test
    void trailingDotAndPortCombo() {
        assertThat(new SubdomainTenantResolver().resolveHost("acme.example.com.:8080"))
                .contains("acme");
    }

    @Test
    void systemLabelAsLeftmostAmongManyLabels() {
        assertThat(new SubdomainTenantResolver().resolveHost("www.alpha.example.com")).isEmpty();
    }

    @Test
    void leftmostNonSystemLabelResolves() {
        assertThat(new SubdomainTenantResolver().resolveHost("tenant.alpha.beta.example.com"))
                .contains("tenant");
    }

    @Test
    void customSystemLabelIsCaseInsensitive() {
        SubdomainTenantResolver resolver = new SubdomainTenantResolver("Host", Set.of("ADMIN"));
        assertThat(resolver.resolveHost("admin.example.com")).isEmpty();
        assertThat(resolver.resolveHost("ADMIN.example.com")).isEmpty();
    }

    @Test
    void deepSubdomainWithPortAndTrailingDot() {
        assertThat(new SubdomainTenantResolver().resolveHost("alpha.beta.example.com.:443"))
                .contains("alpha");
    }
}
