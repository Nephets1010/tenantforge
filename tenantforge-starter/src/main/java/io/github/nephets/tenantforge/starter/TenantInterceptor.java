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

import io.github.nephets.tenantforge.core.TenantContext;
import io.github.nephets.tenantforge.core.TenantResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 租户解析拦截器：在请求进入时按配置解析租户并写入 {@link TenantContext}，请求结束后清除，避免线程池污染。
 *
 * <p>优先级：解析器命中 → 使用解析结果；否则若有默认租户则使用默认；都没有则不设置上下文。
 */
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);

    private final TenantResolver resolver;

    private final TenantProperties properties;

    public TenantInterceptor(TenantResolver resolver, TenantProperties properties) {
        this.resolver = Objects.requireNonNull(resolver, "resolver 不能为空");
        this.properties = Objects.requireNonNull(properties, "properties 不能为空");
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        Map<String, String> headers = extractHeaders(request);
        Optional<String> tenant = resolver.resolve(headers);
        if (tenant.isPresent()) {
            TenantContext.setTenant(tenant.get());
            if (log.isInfoEnabled()) {
                log.info(
                        "请求解析到租户(解析器命中): path={}, tenantId={}",
                        request.getRequestURI(),
                        tenant.get());
            }
        } else if (properties.getDefaultTenantId() != null) {
            TenantContext.setTenant(properties.getDefaultTenantId());
            if (log.isInfoEnabled()) {
                log.info(
                        "请求使用默认租户: path={}, tenantId={}",
                        request.getRequestURI(),
                        properties.getDefaultTenantId());
            }
        } else if (log.isDebugEnabled()) {
            log.debug("请求未解析到租户(无默认): path={}", request.getRequestURI());
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        if (ex != null) {
            log.error("请求处理异常，清除租户上下文: path={}", request.getRequestURI(), ex);
        } else if (log.isDebugEnabled()) {
            log.debug("请求结束，清除租户上下文: path={}", request.getRequestURI());
        }
        TenantContext.clear();
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                headers.put(name, request.getHeader(name));
            }
        }
        return headers;
    }
}
