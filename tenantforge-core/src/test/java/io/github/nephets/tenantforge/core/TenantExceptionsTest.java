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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void registryRequireThrowsUnknown() {
        TenantRegistry registry = new TenantRegistry();
        assertThatThrownBy(() -> registry.require("missing"))
                .isInstanceOf(TenantUnknownException.class);
    }

    @Test
    void contextRequireThrowsNotResolved() {
        TenantContext.clear();
        assertThatThrownBy(TenantContext::requireTenant)
                .isInstanceOf(TenantNotResolvedException.class);
    }
}
