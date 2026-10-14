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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 把新增的解析器接入 {@link CompositeTenantResolver} 解析链：验证 first-non-empty 语义与可配置顺序。 */
class CompositeTenantResolverChainTest {

    private final HeaderTenantResolver header = new HeaderTenantResolver("X-Tenant-Id");

    private final SubdomainTenantResolver subdomain = new SubdomainTenantResolver();

    private final JwtClaimTenantResolver jwt = new JwtClaimTenantResolver();

    private final CompositeTenantResolver chain =
            new CompositeTenantResolver(List.of(header, subdomain, jwt));

    /** 构造 {@code Bearer <jwt>}，其 payload 含 {@code tenant_id}。 */
    private static String bearerJwt(String tenantId) {
        String payload =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                ("{\"tenant_id\":\"" + tenantId + "\"}")
                                        .getBytes(StandardCharsets.UTF_8));
        return "Bearer header." + payload + ".signature";
    }

    @Test
    void headerWinsWhenPresent() {
        Map<String, String> headers =
                Map.of(
                        "X-Tenant-Id",
                        "acme",
                        "Host",
                        "globex.example.com",
                        "Authorization",
                        bearerJwt("initech"));
        assertThat(chain.resolve(headers)).contains("acme");
    }

    @Test
    void fallsBackToSubdomainWhenHeaderMissing() {
        Map<String, String> headers =
                Map.of("Host", "globex.example.com", "Authorization", bearerJwt("initech"));
        assertThat(chain.resolve(headers)).contains("globex");
    }

    @Test
    void fallsBackToJwtWhenHeaderAndSubdomainMiss() {
        Map<String, String> headers =
                Map.of("Host", "www.example.com", "Authorization", bearerJwt("initech"));
        assertThat(chain.resolve(headers)).contains("initech");
    }

    @Test
    void allMissReturnsEmpty() {
        Map<String, String> headers = Map.of("Host", "www.example.com");
        assertThat(chain.resolve(headers)).isEmpty();
    }

    @Test
    void subdomainFirstOrderChangesPrecedence() {
        CompositeTenantResolver reordered =
                new CompositeTenantResolver(List.of(subdomain, header, jwt));
        Map<String, String> headers = Map.of("X-Tenant-Id", "acme", "Host", "globex.example.com");
        assertThat(reordered.resolve(headers)).contains("globex");
    }

    @Test
    void jwtFirstOrderChangesPrecedence() {
        CompositeTenantResolver reordered = new CompositeTenantResolver(List.of(jwt, header));
        Map<String, String> headers =
                Map.of("X-Tenant-Id", "acme", "Authorization", bearerJwt("initech"));
        assertThat(reordered.resolve(headers)).contains("initech");
    }

    @Test
    void blankHeaderValueFallsThroughToNextResolver() {
        Map<String, String> headers = Map.of("X-Tenant-Id", "   ", "Host", "globex.example.com");
        assertThat(chain.resolve(headers)).contains("globex");
    }

    @Test
    void blankBearerTokenFallsThroughToEmpty() {
        Map<String, String> headers = Map.of("Host", "www.example.com", "Authorization", "Bearer ");
        assertThat(chain.resolve(headers)).isEmpty();
    }

    @Test
    void emptyChainReturnsEmpty() {
        assertThat(new CompositeTenantResolver(List.of()).resolve(Map.of())).isEmpty();
    }

    @Test
    void chainExposesConfiguredOrder() {
        assertThat(chain.resolvers()).containsExactly(header, subdomain, jwt).hasSize(3);
    }
}
