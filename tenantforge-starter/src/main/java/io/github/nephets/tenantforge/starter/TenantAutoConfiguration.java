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

import io.github.nephets.tenantforge.core.HeaderTenantResolver;
import io.github.nephets.tenantforge.core.TenantRegistry;
import io.github.nephets.tenantforge.core.TenantResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 租户自动装配：提供 {@link TenantRegistry}、{@link TenantResolver}、{@link TenantInterceptor} 与 {@link
 * TenantWebMvcConfigurer} 等 Bean，应用只需注册租户即可获得按请求隔离的租户上下文。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TenantProperties.class)
public class TenantAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TenantAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public TenantRegistry tenantRegistry() {
        if (log.isInfoEnabled()) {
            log.info("装配租户注册表 TenantRegistry");
        }
        return new TenantRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantResolver tenantResolver(TenantProperties properties) {
        if (log.isInfoEnabled()) {
            log.info("装配租户解析器 TenantResolver: headerName={}", properties.getHeaderName());
        }
        return new HeaderTenantResolver(properties.getHeaderName());
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantInterceptor tenantInterceptor(
            TenantResolver resolver, TenantProperties properties) {
        if (log.isInfoEnabled()) {
            log.info("装配租户拦截器 TenantInterceptor");
        }
        return new TenantInterceptor(resolver, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantWebMvcConfigurer tenantWebMvcConfigurer(TenantInterceptor interceptor) {
        if (log.isInfoEnabled()) {
            log.info("装配租户 WebMvc 配置 TenantWebMvcConfigurer");
        }
        return new TenantWebMvcConfigurer(interceptor);
    }
}
