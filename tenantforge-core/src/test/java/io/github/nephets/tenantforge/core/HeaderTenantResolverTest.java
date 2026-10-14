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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeaderTenantResolverTest {

    @Test
    void resolvesFromConfiguredHeader() {
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        Optional<String> result = resolver.resolve(Map.of("X-Tenant-Id", "acme"));
        assertThat(result).contains("acme");
    }

    @Test
    void missingHeaderReturnsEmpty() {
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        assertThat(resolver.resolve(Map.of("Other", "x"))).isEmpty();
    }

    @Test
    void blankHeaderReturnsEmpty() {
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        assertThat(resolver.resolve(Map.of("X-Tenant-Id", "   "))).isEmpty();
    }

    @Test
    void nullHeadersReturnsEmpty() {
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        assertThat(resolver.resolve(null)).isEmpty();
    }

    @Test
    void trimsWhitespace() {
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        assertThat(resolver.resolve(Map.of("X-Tenant-Id", "  acme  "))).contains("acme");
    }

    @Test
    void headerNameIsStored() {
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant");
        assertThat(resolver.headerName()).isEqualTo("X-Tenant");
    }

    @Test
    void customHeaderNameUsed() {
        HeaderTenantResolver resolver = new HeaderTenantResolver("Tenant");
        assertThat(resolver.resolve(Map.of("Tenant", "t1"))).contains("t1");
        assertThat(resolver.resolve(Map.of("X-Tenant-Id", "t1"))).isEmpty();
    }

    @Test
    void nullHeaderNameRejected() {
        assertThatThrownBy(() -> new HeaderTenantResolver(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolvesEmptyValueAsMissing() {
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        assertThat(resolver.resolve(Map.of("X-Tenant-Id", ""))).isEmpty();
    }

    @Test
    void multipleHeadersStillPicksConfigured() {
        HeaderTenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        assertThat(resolver.resolve(Map.of("a", "1", "X-Tenant-Id", "acme", "b", "2")))
                .contains("acme");
    }
}
