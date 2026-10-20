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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 隔离异常的契约测试：仅覆盖 {@link TenantNotResolvedException} 与 {@link TenantUnknownException} 两个异常类型本身（位于
 * contract 模块）。它们被 {@code TenantRegistry}/{@code TenantContext} 抛出， 那些「抛出来源」的断言分别放在 store / core
 * 的测试里，避免 contract 反向依赖下游模块。
 */
class TenantExceptionsTest {

    @Test
    void tenantNotResolvedHasMessage() {
        TenantNotResolvedException e = new TenantNotResolvedException();
        assertThat(e).isInstanceOf(RuntimeException.class);
        assertThat(e.getMessage()).contains("租户");
    }

    @Test
    void tenantUnknownContainsId() {
        TenantUnknownException e = new TenantUnknownException("ghost");
        assertThat(e.getMessage()).contains("ghost");
    }

    @Test
    void customNotResolvedMessage() {
        TenantNotResolvedException e = new TenantNotResolvedException("nope");
        assertThat(e.getMessage()).isEqualTo("nope");
    }
}
