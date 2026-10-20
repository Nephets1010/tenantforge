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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HeaderTenantResolver} 的边界与契约补充测试（与 {@code HeaderTenantResolverTest} 互补）。
 *
 * <p>为什么测这些：请求头名匹配是大小写敏感的（底层用 {@code Map.get}，由 web 容器决定 key 大小写）， 且请求头值可能为 {@code null}
 * 或带内部空格；这些是最容易被「以为会 trim/忽略大小写」而误用的点，必须钉死。
 */
class HeaderTenantResolverEdgeTest {

    @Test
    void headerKeyIsCaseSensitive() {
        // 配置的是 X-Tenant-Id，传入 x-tenant-id（小写）应视为缺失——Map.get 区分大小写。
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        Optional<String> result = resolver.resolve(Map.of("x-tenant-id", "acme"));
        assertThat(result).isEmpty();
    }

    @Test
    void internalWhitespaceIsPreserved() {
        // trim() 只去首尾空白，值内部空格必须保留（租户 id 理论上是任意字符串）。
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        assertThat(resolver.resolve(Map.of("X-Tenant-Id", "ac me"))).contains("ac me");
    }

    @Test
    void emptyHeadersMapReturnsEmpty() {
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        assertThat(resolver.resolve(Map.of())).isEmpty();
    }

    @Test
    void nullHeaderValueInMapReturnsEmpty() {
        // 请求头存在但值为 null（极端但合法的 Map 状态）应安全返回空，而非 NPE。
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Tenant-Id", null);
        assertThat(resolver.resolve(headers)).isEmpty();
    }
}
