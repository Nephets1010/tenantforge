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

class JwtClaimTenantResolverTest {

    private final JwtClaimTenantResolver resolver = new JwtClaimTenantResolver();

    /** 构造一个形如 header.payload.signature 的 JWT（payload 段为 base64url 无填充编码）。 */
    private static String jwt(String payloadJson) {
        return encode("{\"alg\":\"none\"}") + "." + encode(payloadJson) + ".signature";
    }

    private static String encode(String raw) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void resolvesUnderscoreTenantIdClaim() {
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":\"acme\"}"))).contains("acme");
    }

    @Test
    void resolvesCamelCaseTenantIdClaim() {
        assertThat(resolver.resolveToken(jwt("{\"tenantId\":\"acme\"}"))).contains("acme");
    }

    @Test
    void prefersUnderscoreClaimWhenBothPresent() {
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":\"first\",\"tenantId\":\"second\"}")))
                .contains("first");
    }

    @Test
    void customClaimNamesAreHonored() {
        JwtClaimTenantResolver custom =
                new JwtClaimTenantResolver("Authorization", List.of("org_id"));
        assertThat(custom.resolveToken(jwt("{\"org_id\":\"globex\"}"))).contains("globex");
    }

    @Test
    void customClaimOrderDefinesPrecedence() {
        JwtClaimTenantResolver custom =
                new JwtClaimTenantResolver("Authorization", List.of("tenantId", "tenant_id"));
        assertThat(custom.resolveToken(jwt("{\"tenant_id\":\"first\",\"tenantId\":\"second\"}")))
                .contains("second");
    }

    @Test
    void missingClaimReturnsEmpty() {
        assertThat(resolver.resolveToken(jwt("{\"sub\":\"user-1\"}"))).isEmpty();
    }

    @Test
    void blankClaimValueReturnsEmpty() {
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":\"   \"}"))).isEmpty();
    }

    @Test
    void numericClaimReturnsEmpty() {
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":42}"))).isEmpty();
    }

    @Test
    void booleanClaimReturnsEmpty() {
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":true}"))).isEmpty();
    }

    @Test
    void objectClaimReturnsEmpty() {
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":{\"name\":\"acme\"}}"))).isEmpty();
    }

    @Test
    void arrayClaimReturnsEmpty() {
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":[\"acme\"]}"))).isEmpty();
    }

    @Test
    void malformedTokenWithoutDotsReturnsEmpty() {
        assertThat(resolver.resolveToken("not-a-jwt")).isEmpty();
    }

    @Test
    void invalidBase64PayloadReturnsEmpty() {
        assertThat(resolver.resolveToken("header.!!!.signature")).isEmpty();
    }

    @Test
    void payloadWithImpossibleBase64LengthReturnsEmpty() {
        // base64url 段长度模 4 为 1 时恒不合法：这类畸形输入应直接判定为空，而不是靠抛异常兜底
        assertThat(resolver.resolveToken("header.AAAAA.signature")).isEmpty();
        assertThat(resolver.resolveToken("header.A.signature")).isEmpty();
    }

    @Test
    void nullTokenReturnsEmpty() {
        assertThat(resolver.resolveToken(null)).isEmpty();
    }

    @Test
    void blankTokenReturnsEmpty() {
        assertThat(resolver.resolveToken("  ")).isEmpty();
    }

    @Test
    void bearerPrefixIsStripped() {
        assertThat(resolver.resolveToken("Bearer " + jwt("{\"tenant_id\":\"acme\"}")))
                .contains("acme");
    }

    @Test
    void bearerPrefixIsCaseInsensitive() {
        assertThat(resolver.resolveToken("bearer " + jwt("{\"tenant_id\":\"acme\"}")))
                .contains("acme");
    }

    @Test
    void rawTokenWithoutBearerPrefixIsAccepted() {
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":\"acme\"}"))).contains("acme");
    }

    @Test
    void whitespaceAroundHeaderValueIsTrimmed() {
        assertThat(resolver.resolveToken("  Bearer " + jwt("{\"tenant_id\":\"acme\"}") + "  "))
                .contains("acme");
    }

    @Test
    void resolvesFromAuthorizationHeader() {
        assertThat(
                        resolver.resolve(
                                Map.of(
                                        "Authorization",
                                        "Bearer " + jwt("{\"tenant_id\":\"acme\"}"))))
                .contains("acme");
    }

    @Test
    void customHeaderNameIsHonored() {
        JwtClaimTenantResolver custom = new JwtClaimTenantResolver("X-Jwt");
        assertThat(custom.resolve(Map.of("X-Jwt", jwt("{\"tenant_id\":\"acme\"}"))))
                .contains("acme");
        assertThat(
                        custom.resolve(
                                Map.of(
                                        "Authorization",
                                        "Bearer " + jwt("{\"tenant_id\":\"acme\"}"))))
                .isEmpty();
    }

    @Test
    void missingHeaderReturnsEmpty() {
        assertThat(resolver.resolve(Map.of("X-Other", "value"))).isEmpty();
    }

    @Test
    void nullHeadersReturnsEmpty() {
        assertThat(resolver.resolve(null)).isEmpty();
    }

    @Test
    void escapedCharactersInClaimAreDecoded() {
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":\"a\\\"b\"}"))).contains("a\"b");
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":\"a\\\\b\"}"))).contains("a\\b");
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":\"a\\nb\"}"))).contains("a\nb");
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":\"a\\tb\"}"))).contains("a\tb");
    }

    @Test
    void unicodeEscapeInClaimIsDecoded() {
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":\"caf\\u00e9\"}"))).contains("café");
    }

    @Test
    void invalidUnicodeEscapeReturnsEmpty() {
        assertThat(resolver.resolveToken(jwt("{\"tenant_id\":\"caf\\uZZZZ\"}"))).isEmpty();
    }

    @Test
    void unterminatedStringReturnsEmpty() {
        assertThat(JwtClaimTenantResolver.extractStringClaim("{\"tenant_id\":\"abc", "tenant_id"))
                .isEmpty();
    }

    @Test
    void unknownEscapeSequenceReturnsEmpty() {
        assertThat(
                        JwtClaimTenantResolver.extractStringClaim(
                                "{\"tenant_id\":\"a\\xb\"}", "tenant_id"))
                .isEmpty();
    }

    @Test
    void decodesPayloadToJson() {
        assertThat(JwtClaimTenantResolver.decodePayload(jwt("{\"tenant_id\":\"acme\"}")))
                .contains("{\"tenant_id\":\"acme\"}");
    }

    @Test
    void decodePayloadRejectsMalformedToken() {
        assertThat(JwtClaimTenantResolver.decodePayload("nodots")).isEmpty();
        assertThat(JwtClaimTenantResolver.decodePayload(null)).isEmpty();
    }

    @Test
    void claimValueReadsSpecificClaim() {
        String token = jwt("{\"tenant_id\":\"acme\",\"org\":\"globex\"}");
        assertThat(resolver.claimValue(token, "org")).contains("globex");
        assertThat(resolver.claimValue(token, "absent")).isEmpty();
        assertThat(resolver.claimValue(token, null)).isEmpty();
        assertThat(resolver.claimValue(token, "  ")).isEmpty();
    }

    @Test
    void exposesConfiguration() {
        assertThat(resolver.headerName()).isEqualTo("Authorization");
        assertThat(resolver.claimNames()).containsExactly("tenant_id", "tenantId");
    }

    @Test
    void emptyClaimNamesFallsBackToDefaults() {
        JwtClaimTenantResolver custom = new JwtClaimTenantResolver("Authorization", List.of());
        assertThat(custom.claimNames()).containsExactly("tenant_id", "tenantId");
    }
}
