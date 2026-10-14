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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeTenantResolverTest {

    private final TenantResolver always(String v) {
        return headers -> Optional.of(v);
    }

    private final TenantResolver never() {
        return headers -> Optional.empty();
    }

    @Test
    void firstResolverWins() {
        CompositeTenantResolver resolver = CompositeTenantResolver.of(always("a"), always("b"));
        assertThat(resolver.resolve(Map.of())).contains("a");
    }

    @Test
    void fallsThroughToSecond() {
        CompositeTenantResolver resolver = CompositeTenantResolver.of(never(), always("b"));
        assertThat(resolver.resolve(Map.of())).contains("b");
    }

    @Test
    void allEmptyReturnsEmpty() {
        CompositeTenantResolver resolver = CompositeTenantResolver.of(never(), never());
        assertThat(resolver.resolve(Map.of())).isEmpty();
    }

    @Test
    void ofVarargsBuildsList() {
        CompositeTenantResolver resolver = CompositeTenantResolver.of(always("x"));
        assertThat(resolver.resolvers()).hasSize(1);
    }

    @Test
    void resolversListPreservesOrder() {
        CompositeTenantResolver resolver =
                CompositeTenantResolver.of(always("1"), always("2"), always("3"));
        assertThat(resolver.resolvers()).hasSize(3);
        assertThat(resolver.resolve(Map.of())).contains("1");
    }

    @Test
    void emptyCompositeReturnsEmpty() {
        CompositeTenantResolver resolver = new CompositeTenantResolver(List.of());
        assertThat(resolver.resolve(Map.of())).isEmpty();
    }

    @Test
    void firstWinsEvenIfSecondAlsoMatches() {
        CompositeTenantResolver resolver =
                CompositeTenantResolver.of(
                        headers -> Optional.of("first"), headers -> Optional.of("second"));
        assertThat(resolver.resolve(Map.of())).contains("first");
    }

    @Test
    void headerResolverCombinedWithConstant() {
        HeaderTenantResolver header = new HeaderTenantResolver("X-Tenant-Id");
        CompositeTenantResolver resolver = CompositeTenantResolver.of(header, always("default"));
        assertThat(resolver.resolve(Map.of("X-Tenant-Id", "acme"))).contains("acme");
        assertThat(resolver.resolve(Map.of())).contains("default");
    }

    @Test
    void threeLevelFallback() {
        CompositeTenantResolver resolver =
                CompositeTenantResolver.of(never(), never(), always("deep"));
        assertThat(resolver.resolve(Map.of())).contains("deep");
    }

    @Test
    void resolversListIsImmutable() {
        CompositeTenantResolver resolver = CompositeTenantResolver.of(always("x"));
        assertThat(resolver.resolvers()).isInstanceOf(List.class);
    }
}
