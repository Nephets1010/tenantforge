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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 租户注册表：维护租户元数据，支持注册、查询与存在性校验。 */
public final class TenantRegistry {

    private static final Logger log = LoggerFactory.getLogger(TenantRegistry.class);

    private final Map<String, Tenant> tenants = new LinkedHashMap<>();

    public void register(Tenant tenant) {
        Objects.requireNonNull(tenant, "tenant 不能为 null");
        if (tenants.containsKey(tenant.getId())) {
            log.error("租户重复注册被拒绝: tenantId={}", tenant.getId());
            throw new IllegalStateException("租户已注册: " + tenant.getId());
        }
        tenants.put(tenant.getId(), tenant);
        if (log.isInfoEnabled()) {
            log.info("注册租户: tenantId={}, name={}", tenant.getId(), tenant.getName());
        }
    }

    public Tenant get(String id) {
        return tenants.get(id);
    }

    /** 返回租户；不存在时抛出 {@link TenantUnknownException}。 */
    public Tenant require(String id) {
        Tenant tenant = tenants.get(id);
        if (tenant == null) {
            log.warn("请求了未知租户: tenantId={}", id);
            throw new TenantUnknownException(id);
        }
        return tenant;
    }

    public boolean contains(String id) {
        return tenants.containsKey(id);
    }

    public List<Tenant> list() {
        return List.copyOf(tenants.values());
    }

    public int size() {
        return tenants.size();
    }
}
