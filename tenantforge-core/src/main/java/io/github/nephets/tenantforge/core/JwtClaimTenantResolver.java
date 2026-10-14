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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JWT claim 租户解析器：从 {@code Authorization: Bearer <jwt>} 中取出 JWT，用 {@link Base64} 解码其 payload 段（JWT
 * 的第二段），再读取指定 claim 的字符串值作为租户标识。
 *
 * <p>默认依次尝试 {@code tenant_id} 与 {@code tenantId}，取第一个命中且非空白的值；claim 名称可配置。
 *
 * <p><b>只解码、不验签</b>：签名校验属于网关/认证层的职责，本类刻意不引入任何 JWT 库，以保持 core 零依赖（除 JDK 外）。因此它只应在「上游已完成鉴权」的链路中使用。
 */
public final class JwtClaimTenantResolver implements TenantResolver {

    private static final Logger log = LoggerFactory.getLogger(JwtClaimTenantResolver.class);

    /** 默认尝试的 claim 名称，按序优先。 */
    private static final List<String> DEFAULT_CLAIMS = List.of("tenant_id", "tenantId");

    private final String headerName;

    private final List<String> claimNames;

    public JwtClaimTenantResolver() {
        this("Authorization");
    }

    public JwtClaimTenantResolver(String headerName) {
        this(headerName, DEFAULT_CLAIMS);
    }

    /**
     * @param headerName 承载 JWT 的请求头名称，通常为 {@code Authorization}
     * @param claimNames 候选 claim 名称，按序优先；为 {@code null} 或空时使用默认值
     */
    public JwtClaimTenantResolver(String headerName, List<String> claimNames) {
        this.headerName = Objects.requireNonNull(headerName, "headerName 不能为 null");
        List<String> names = new ArrayList<>();
        List<String> source = claimNames == null ? DEFAULT_CLAIMS : claimNames;
        for (String claim : source) {
            if (claim != null && !claim.isBlank()) {
                names.add(claim.trim());
            }
        }
        this.claimNames = List.copyOf(names.isEmpty() ? DEFAULT_CLAIMS : names);
    }

    @Override
    public Optional<String> resolve(Map<String, String> headers) {
        if (headers == null) {
            return Optional.empty();
        }
        return resolveToken(headers.get(headerName));
    }

    /** 从 {@code Bearer <jwt>} 或裸 JWT 字符串解析租户标识。 */
    public Optional<String> resolveToken(String authorizationOrToken) {
        if (authorizationOrToken == null || authorizationOrToken.isBlank()) {
            if (log.isDebugEnabled()) {
                log.debug("JWT 解析器未提供令牌: headerName={}", headerName);
            }
            return Optional.empty();
        }
        String token = stripBearer(authorizationOrToken);
        if (token.isEmpty()) {
            return Optional.empty();
        }
        // 只解码一次 payload：多个候选 claim 复用同一份 JSON。
        // 此前每个 claim 都走一次 claimValue()，N 个候选 claim 会触发 N 次 Base64 解码 + String 构造。
        Optional<String> payload = decodePayload(token);
        if (payload.isEmpty()) {
            return Optional.empty();
        }
        String json = payload.get();
        for (String claim : claimNames) {
            Optional<String> value = extractStringClaim(json, claim).filter(v -> !v.isBlank());
            if (value.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.debug("JWT 解析器命中租户: claim={}, tenantId={}", claim, value.get());
                }
                return value;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("JWT 解析器未在声明中找到租户: claims={}", claimNames);
        }
        return Optional.empty();
    }

    /** 解码 JWT 并读取单个 claim 的字符串值；缺失、非字符串或空白时返回空。 */
    public Optional<String> claimValue(String token, String claimName) {
        if (claimName == null || claimName.isBlank()) {
            return Optional.empty();
        }
        return decodePayload(token)
                .flatMap(json -> extractStringClaim(json, claimName))
                .filter(value -> !value.isBlank());
    }

    public String headerName() {
        return headerName;
    }

    public List<String> claimNames() {
        return claimNames;
    }

    /** 解码 JWT 第二段（base64url）为 JSON 文本；任何畸形输入都返回空而非抛异常。 */
    static Optional<String> decodePayload(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return Optional.empty();
        }
        String payload = parts[1];
        // base64url 段长度模 4 为 1 时恒不合法（补 3 个 '=' 也救不回来），显式判定为畸形。
        // 畸形 token 是常见输入而非罕见异常，这里避免为它构造一次异常（省掉栈填充开销）。
        // 其余非法字符仍由下面的 try/catch 兜底。
        if (payload.length() % 4 == 1) {
            return Optional.empty();
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(pad(payload));
            return Optional.of(new String(decoded, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * 极简 JSON 字段提取：定位 {@code "claim"} 后的字符串字面量并解码转义。
     *
     * <p><b>这是刻意的朴素扫描，不是 JSON 解析器</b>：core 必须保持零第三方依赖，因此不引入 JSON 库， 只做「够用」的字段定位。已知边界：
     *
     * <ul>
     *   <li>以 {@code "key"} 字面量做定位，理论上可能命中另一个 claim 的<b>字符串值内部</b>出现的同名序列 （例如某字段的值恰好是 {@code
     *       "tenant_id"}）；
     *   <li>只提取<b>字符串</b>类型的值；若命中的 key 对应数字/布尔/对象/数组，会跳过并继续找下一处同名 key；
     *   <li>不做嵌套/数组下标解析，也不需要 JSON 整体合法。
     * </ul>
     *
     * <p>因此本方法只应用于「结构已知、上游已鉴权」的 JWT payload，不应被当作通用 JSON 解析入口使用。
     */
    static Optional<String> extractStringClaim(String json, String claimName) {
        String needle = "\"" + claimName + "\"";
        int from = 0;
        while (true) {
            int index = json.indexOf(needle, from);
            if (index < 0) {
                return Optional.empty();
            }
            int colon = json.indexOf(':', index + needle.length());
            if (colon < 0) {
                return Optional.empty();
            }
            int cursor = colon + 1;
            while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
                cursor++;
            }
            // 命中的 key 若不是字符串值（数字/布尔/对象/数组），继续找下一处同名 key
            if (cursor < json.length() && json.charAt(cursor) == '"') {
                return readJsonString(json, cursor + 1);
            }
            from = index + 1;
        }
    }

    private static String stripBearer(String value) {
        String trimmed = value.trim();
        if (trimmed.length() > 7 && trimmed.substring(0, 7).equalsIgnoreCase("Bearer ")) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }

    /** base64url 解码器要求补齐 {@code =}，JWT 通常省略填充。 */
    private static String pad(String base64url) {
        int remainder = base64url.length() % 4;
        if (remainder == 0) {
            return base64url;
        }
        StringBuilder padded = new StringBuilder(base64url);
        for (int i = remainder; i < 4; i++) {
            padded.append('=');
        }
        return padded.toString();
    }

    private static Optional<String> readJsonString(String json, int start) {
        StringBuilder value = new StringBuilder();
        int cursor = start;
        while (cursor < json.length()) {
            char current = json.charAt(cursor);
            if (current == '"') {
                return Optional.of(value.toString());
            }
            if (current == '\\') {
                cursor++;
                if (cursor >= json.length()) {
                    return Optional.empty();
                }
                char escaped = json.charAt(cursor);
                switch (escaped) {
                    case '"':
                        value.append('"');
                        break;
                    case '\\':
                        value.append('\\');
                        break;
                    case '/':
                        value.append('/');
                        break;
                    case 'b':
                        value.append('\b');
                        break;
                    case 'f':
                        value.append('\f');
                        break;
                    case 'n':
                        value.append('\n');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    case 'u':
                        if (cursor + 4 >= json.length()) {
                            return Optional.empty();
                        }
                        String hex = json.substring(cursor + 1, cursor + 5);
                        try {
                            value.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            return Optional.empty();
                        }
                        cursor += 4;
                        break;
                    default:
                        return Optional.empty();
                }
                cursor++;
            } else {
                value.append(current);
                cursor++;
            }
        }
        return Optional.empty();
    }
}
