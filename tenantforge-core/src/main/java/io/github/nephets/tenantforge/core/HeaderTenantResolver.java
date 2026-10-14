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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** 基于指定请求头名称的租户解析器（如 {@code X-Tenant-Id}）。 */
public final class HeaderTenantResolver implements TenantResolver {

    private static final Logger log = LoggerFactory.getLogger(HeaderTenantResolver.class);

    private final String headerName;

    public HeaderTenantResolver(String headerName) {
        this.headerName = Objects.requireNonNull(headerName, "headerName 不能为 null");
    }

    @Override
    public Optional<String> resolve(Map<String, String> headers) {
        if (headers == null) {
            return Optional.empty();
        }
        String value = headers.get(headerName);
        if (value == null || value.isBlank()) {
            if (log.isDebugEnabled()) {
                log.debug("请求头解析器未命中: headerName={}", headerName);
            }
            return Optional.empty();
        }
        String tenantId = value.trim();
        if (log.isDebugEnabled()) {
            log.debug("请求头解析器命中租户: headerName={}, tenantId={}", headerName, tenantId);
        }
        return Optional.of(tenantId);
    }

    public String headerName() {
        return headerName;
    }
}
