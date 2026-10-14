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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SubdomainTenantResolverTest {

    private final SubdomainTenantResolver resolver = new SubdomainTenantResolver();

    @Test
    void resolvesLeftmostLabelAsTenant() {
        assertThat(resolver.resolveHost("acme.example.com")).contains("acme");
    }

    @Test
    void stripsPortBeforeParsing() {
        assertThat(resolver.resolveHost("acme.example.com:8080")).contains("acme");
    }

    @Test
    void stripsTrailingDot() {
        assertThat(resolver.resolveHost("acme.example.com.")).contains("acme");
    }

    @Test
    void lowercasesResult() {
        assertThat(resolver.resolveHost("ACME.Example.COM")).contains("acme");
    }

    @Test
    void ignoresWwwSystemLabel() {
        assertThat(resolver.resolveHost("www.example.com")).isEmpty();
    }

    @Test
    void ignoresApiSystemLabel() {
        assertThat(resolver.resolveHost("api.example.com")).isEmpty();
    }

    @Test
    void bareDomainHasNoTenantSubdomain() {
        assertThat(resolver.resolveHost("example.com")).isEmpty();
    }

    @Test
    void bareDomainWithPortHasNoTenantSubdomain() {
        assertThat(resolver.resolveHost("example.com:80")).isEmpty();
    }

    @Test
    void localhostHasNoSubdomain() {
        assertThat(resolver.resolveHost("localhost")).isEmpty();
    }

    @Test
    void nullHostReturnsEmpty() {
        assertThat(resolver.resolveHost(null)).isEmpty();
    }

    @Test
    void blankHostReturnsEmpty() {
        assertThat(resolver.resolveHost("   ")).isEmpty();
    }

    @Test
    void ipv6HostReturnsEmpty() {
        assertThat(resolver.resolveHost("[::1]:8080")).isEmpty();
    }

    @Test
    void ipv6LiteralWithoutPortReturnsEmpty() {
        assertThat(resolver.resolveHost("[2001:db8::1]")).isEmpty();
    }

    @Test
    void deepSubdomainUsesLeftmostLabel() {
        assertThat(resolver.resolveHost("alpha.beta.example.com")).contains("alpha");
    }

    @Test
    void hyphenatedSubdomainIsPreserved() {
        assertThat(resolver.resolveHost("acme-corp.example.com")).contains("acme-corp");
    }

    @Test
    void numericSubdomainIsPreserved() {
        assertThat(resolver.resolveHost("tenant1.example.com")).contains("tenant1");
    }

    @Test
    void resolvesFromHostHeader() {
        assertThat(resolver.resolve(Map.of("Host", "acme.example.com"))).contains("acme");
    }

    @Test
    void missingHostHeaderReturnsEmpty() {
        assertThat(resolver.resolve(Map.of("X-Other", "value"))).isEmpty();
    }

    @Test
    void nullHeadersReturnsEmpty() {
        assertThat(resolver.resolve(null)).isEmpty();
    }

    @Test
    void customHeaderNameIsHonored() {
        SubdomainTenantResolver custom = new SubdomainTenantResolver("X-Forwarded-Host");
        assertThat(custom.resolve(Map.of("X-Forwarded-Host", "globex.example.com")))
                .contains("globex");
        assertThat(custom.resolve(Map.of("Host", "globex.example.com"))).isEmpty();
    }

    @Test
    void customSystemLabelsOverrideDefaults() {
        SubdomainTenantResolver custom = new SubdomainTenantResolver("Host", Set.of("acme"));
        assertThat(custom.resolveHost("acme.example.com")).isEmpty();
        assertThat(custom.resolveHost("www.example.com")).contains("www");
    }

    @Test
    void emptySystemLabelsAllowAnyLabel() {
        SubdomainTenantResolver custom = new SubdomainTenantResolver("Host", Set.of());
        assertThat(custom.resolveHost("www.example.com")).contains("www");
    }

    @Test
    void systemLabelsAreNormalizedToLowerCase() {
        SubdomainTenantResolver custom =
                new SubdomainTenantResolver("Host", Set.of("WWW", " Api "));
        assertThat(custom.resolveHost("www.example.com")).isEmpty();
        assertThat(custom.resolveHost("api.example.com")).isEmpty();
    }

    @Test
    void exposesConfiguration() {
        assertThat(resolver.headerName()).isEqualTo("Host");
        assertThat(resolver.systemLabels()).contains("www", "api");
    }
}
