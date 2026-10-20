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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 {@link TenantRegistry#require(String)} 在租户不存在时抛出 {@link TenantUnknownException}。 放在 store 模块（与
 * {@code TenantRegistry} 同模块），异常类型本身由 contract 的 {@code TenantExceptionsTest} 覆盖。
 */
class TenantRegistryExceptionTest {

    @Test
    void registryRequireThrowsUnknown() {
        TenantRegistry registry = new TenantRegistry();
        assertThatThrownBy(() -> registry.require("missing"))
                .isInstanceOf(TenantUnknownException.class);
    }
}
