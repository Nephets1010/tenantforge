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

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 子域名租户解析器：从主机名（默认取自 {@code Host} 请求头）解析出租户标识。
 *
 * <p>解析规则（按序）：
 *
 * <ul>
 *   <li>剥离端口、结尾的 {@code .}，并统一转小写；
 *   <li>标签数少于 3 视为「没有子域名」，返回空 —— 避免把注册域名本身（{@code example.com}） 或裸主机名（{@code localhost}）误判成租户；
 *   <li>最左标签命中「系统标签」集合（默认 {@code www}/{@code api}/{@code admin}/{@code mail}/ {@code static}/{@code
 *       cdn}）时返回空，这类标签属于基础设施而非租户；
 *   <li>其余返回最左标签，例如 {@code acme.example.com} → {@code acme}。
 * </ul>
 *
 * <p>核心解析不依赖任何 web/Spring 类型：{@link #resolveHost(String)} 直接接受一个主机名字符串， 因此也可用于网关、定时任务等非 HTTP 场景。
 */
public final class SubdomainTenantResolver implements TenantResolver {

    private static final Logger log = LoggerFactory.getLogger(SubdomainTenantResolver.class);

    /** 默认被忽略的系统子域标签。 */
    private static final Set<String> DEFAULT_SYSTEM_LABELS =
            Set.of("www", "api", "admin", "mail", "static", "cdn");

    private final String headerName;

    private final Set<String> systemLabels;

    public SubdomainTenantResolver() {
        this("Host");
    }

    public SubdomainTenantResolver(String headerName) {
        this(headerName, DEFAULT_SYSTEM_LABELS);
    }

    /**
     * @param headerName 承载主机名的请求头名称，通常为 {@code Host}
     * @param systemLabels 需要忽略的系统子域标签；为 {@code null} 时表示不忽略任何标签
     */
    public SubdomainTenantResolver(String headerName, Set<String> systemLabels) {
        this.headerName = Objects.requireNonNull(headerName, "headerName 不能为 null");
        Set<String> normalized = new LinkedHashSet<>();
        if (systemLabels != null) {
            for (String label : systemLabels) {
                if (label != null && !label.isBlank()) {
                    normalized.add(label.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        this.systemLabels = Set.copyOf(normalized);
    }

    @Override
    public Optional<String> resolve(Map<String, String> headers) {
        if (headers == null) {
            return Optional.empty();
        }
        return resolveHost(headers.get(headerName));
    }

    /** 从主机名解析租户标识；无法解析时返回 {@link Optional#empty()}。 */
    public Optional<String> resolveHost(String host) {
        if (host == null || host.isBlank()) {
            if (log.isDebugEnabled()) {
                log.debug("子域名解析器未提供主机名: headerName={}", headerName);
            }
            return Optional.empty();
        }
        String[] labels = normalize(host).split("\\.");
        if (labels.length < 3) {
            if (log.isDebugEnabled()) {
                log.debug("子域名解析器未解析到租户(标签数<3): host={}", host);
            }
            return Optional.empty();
        }
        String subdomain = labels[0];
        if (subdomain.isEmpty() || systemLabels.contains(subdomain)) {
            if (log.isDebugEnabled()) {
                log.debug("子域名解析器忽略系统/空子域: host={}, subdomain={}", host, subdomain);
            }
            return Optional.empty();
        }
        if (log.isDebugEnabled()) {
            log.debug("子域名解析器命中租户: host={}, tenantId={}", host, subdomain);
        }
        return Optional.of(subdomain);
    }

    public String headerName() {
        return headerName;
    }

    public Set<String> systemLabels() {
        return systemLabels;
    }

    /** 剥离端口与结尾的点，并转小写，使 {@code ACME.Example.com.:8080} 归一为 {@code acme.example.com}。 */
    private static String normalize(String host) {
        String value = host.trim();
        if (value.startsWith("[")) {
            // IPv6 字面量：地址本身含 ':'，不能按 ':' 截端口（否则 "[::1]:8080" 会被截成 "["）。
            // 取方括号内的地址主体；括号未闭合时退化为去掉开头的 '['。
            // 结果不含 '.'，最终仍会因「标签数 < 3」返回空 —— 这里只是让原因显式而非靠偶然。
            int closingBracket = value.indexOf(']');
            value = closingBracket >= 0 ? value.substring(1, closingBracket) : value.substring(1);
        } else {
            int colon = value.indexOf(':');
            if (colon >= 0) {
                value = value.substring(0, colon);
            }
        }
        while (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
