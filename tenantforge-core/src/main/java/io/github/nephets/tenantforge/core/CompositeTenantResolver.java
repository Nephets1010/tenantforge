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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 组合解析器：按序尝试多个 {@link TenantResolver}，返回第一个非空结果。 */
public final class CompositeTenantResolver implements TenantResolver {

    private static final Logger log = LoggerFactory.getLogger(CompositeTenantResolver.class);

    private final List<TenantResolver> resolvers;

    public CompositeTenantResolver(List<TenantResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    public static CompositeTenantResolver of(TenantResolver... resolvers) {
        List<TenantResolver> list = new ArrayList<>();
        for (TenantResolver r : resolvers) {
            list.add(r);
        }
        return new CompositeTenantResolver(list);
    }

    @Override
    public Optional<String> resolve(Map<String, String> headers) {
        for (TenantResolver resolver : resolvers) {
            Optional<String> result = resolver.resolve(headers);
            if (result.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.debug(
                            "组合解析器命中租户: resolver={}, tenantId={}",
                            resolver.getClass().getSimpleName(),
                            result.get());
                }
                return result;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("组合解析器未解析到任何租户: resolverCount={}", resolvers.size());
        }
        return Optional.empty();
    }

    public List<TenantResolver> resolvers() {
        return resolvers;
    }
}
