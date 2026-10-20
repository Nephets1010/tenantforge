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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CompositeTenantResolver} 的边界与契约补充测试（与 {@code CompositeTenantResolverTest} 互补）。
 *
 * <p>为什么测这些：组合器持有「解析器列表」这一内部集合，其不可变性、对 null 元素的防御、 以及把同一份 headers 透传给每个解析器的语义，都是组合链可靠性的根基。
 */
class CompositeTenantResolverEdgeTest {

    private TenantResolver always(String v) {
        return headers -> Optional.of(v);
    }

    private TenantResolver never() {
        return headers -> Optional.empty();
    }

    @Test
    void nullElementInConstructorListRejected() {
        // List.copyOf 拒绝 null 元素，防止组合器内部持有空解析器导致运行时 NPE。
        assertThatThrownBy(() -> new CompositeTenantResolver(List.of(always("a"), null)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullElementInOfVarargsRejected() {
        assertThatThrownBy(() -> CompositeTenantResolver.of(always("a"), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolversListIsImmutable() {
        CompositeTenantResolver resolver = CompositeTenantResolver.of(always("x"));
        assertThatThrownBy(() -> resolver.resolvers().add(never()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void passesSameHeadersToEachResolver() {
        AtomicReference<Map<String, String>> captured = new AtomicReference<>();
        TenantResolver capturer =
                headers -> {
                    captured.set(headers);
                    return Optional.empty();
                };
        Map<String, String> headers = Map.of("X-Tenant-Id", "acme");
        new CompositeTenantResolver(List.of(capturer, always("fallback"))).resolve(headers);
        assertThat(captured.get()).isSameAs(headers);
    }

    @Test
    void singleResolverInCompositeReturnsItsValue() {
        CompositeTenantResolver resolver = CompositeTenantResolver.of(always("z"));
        assertThat(resolver.resolve(Map.of())).contains("z");
    }
}
