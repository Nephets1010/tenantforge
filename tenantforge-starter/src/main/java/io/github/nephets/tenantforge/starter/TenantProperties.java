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

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 多租户配置属性，前缀 {@code tenantforge}。 */
@ConfigurationProperties(prefix = "tenantforge")
public class TenantProperties {

    /** 用于解析租户的请求头名称。默认 {@code X-Tenant-Id}。 */
    private String headerName = "X-Tenant-Id";

    /** 是否启用租户自动解析。默认 true。 */
    private boolean enabled = true;

    /** 当请求头缺失时的默认租户 id（可空，表示不设置默认）。 */
    private String defaultTenantId;

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultTenantId() {
        return defaultTenantId;
    }

    public void setDefaultTenantId(String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }
}
