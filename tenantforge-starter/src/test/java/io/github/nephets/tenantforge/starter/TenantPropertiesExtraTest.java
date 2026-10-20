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
package io.github.nephets.tenantforge.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link TenantProperties} 的默认值与读写往返测试（与 {@code TenantPropertiesTest} 互补）。 */
class TenantPropertiesExtraTest {

    @Test
    void defaultHeaderNameIsXTenantId() {
        assertThat(new TenantProperties().getHeaderName()).isEqualTo("X-Tenant-Id");
    }

    @Test
    void enabledDefaultsTrue() {
        assertThat(new TenantProperties().isEnabled()).isTrue();
    }

    @Test
    void settersRoundTrip() {
        TenantProperties props = new TenantProperties();
        props.setHeaderName("Tenant");
        props.setEnabled(false);
        props.setDefaultTenantId("anon");
        assertThat(props.getHeaderName()).isEqualTo("Tenant");
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getDefaultTenantId()).isEqualTo("anon");
    }
}
