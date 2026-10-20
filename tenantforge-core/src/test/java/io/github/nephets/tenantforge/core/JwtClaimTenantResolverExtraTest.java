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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JwtClaimTenantResolver} 的深层边界测试（与 {@code JwtClaimTenantResolverTest} 互补）。
 *
 * <p>为什么测这些：该解析器的 JSON 提取是自写的「朴素扫描」，不是通用解析器，有其刻意保留的边界 （跳过非字符串 claim、引号界定避免子串误命中、仅解码第二个 JWT
 * 段、容忍冒号周围空白、过滤空 claim 名）。 这些边界一旦被改坏，会在「上游已鉴权」的链路里静默返回空，必须被钉死。
 */
class JwtClaimTenantResolverExtraTest {

    private static String jwt(String payloadJson) {
        return encode("{\"alg\":\"none\"}") + "." + encode(payloadJson) + ".signature";
    }

    private static String encode(String raw) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void numericClaimFollowedByStringClaimResolvesString() {
        // 第一个 tenant_id 是数字（被跳过），继续找下一个同名校验的字符串值。
        assertThat(resolver().resolveToken(jwt("{\"tenant_id\":42,\"tenant_id\":\"acme\"}")))
                .contains("acme");
    }

    @Test
    void keySubstringInsideOtherValueDoesNotMisresolve() {
        // 文档化边界：另一个字符串值里恰好出现 "tenant_id" 子串时，因按带引号的键定位，不会误命中。
        assertThat(
                        resolver()
                                .resolveToken(
                                        jwt(
                                                "{\"note\":\"use tenant_id here\",\"tenant_id\":\"acme\"}")))
                .contains("acme");
    }

    @Test
    void extraDotSegmentsStillUseSecondSegment() {
        // JWT 实际只有三段；多余段（如签名带点）应被忽略，始终取第二段 payload。
        String payload = encode("{\"tenant_id\":\"acme\"}");
        String token = "header." + payload + ".sig.extra";
        assertThat(resolver().resolveToken(token)).contains("acme");
    }

    @Test
    void spacingAroundColonIsTolerated() {
        assertThat(resolver().resolveToken(jwt("{\"tenant_id\" : \"acme\"}"))).contains("acme");
    }

    @Test
    void blankClaimNamesAreFiltered() {
        JwtClaimTenantResolver custom =
                new JwtClaimTenantResolver("Authorization", List.of(" ", "org_id"));
        assertThat(custom.claimNames()).containsExactly("org_id");
        assertThat(custom.resolveToken(jwt("{\"org_id\":\"globex\"}"))).contains("globex");
    }

    @Test
    void claimValueWithNumericClaimReturnsEmpty() {
        // claimValue 走与 resolve 相同的字符串定位，数字值同样被过滤。
        String token = jwt("{\"tenant_id\":42}");
        assertThat(resolver().claimValue(token, "tenant_id")).isEmpty();
    }

    @Test
    void slashEscapeIsDecoded() {
        // readJsonString 的 '/' 分支（switch case '/'）未被主测试覆盖，这里专门钉死。
        assertThat(resolver().resolveToken(jwt("{\"tenant_id\":\"a\\/b\"}"))).contains("a/b");
    }

    private JwtClaimTenantResolver resolver() {
        return new JwtClaimTenantResolver();
    }
}
