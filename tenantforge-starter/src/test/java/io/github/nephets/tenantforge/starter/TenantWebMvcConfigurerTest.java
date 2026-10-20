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
import io.github.nephets.tenantforge.core.TenantResolver;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.assertj.core.api.Assertions.assertThat;

class TenantWebMvcConfigurerTest {

    @Test
    void registersInterceptor() {
        TenantInterceptor interceptor =
                new TenantInterceptor(
                        new HeaderTenantResolver("X-Tenant-Id"), new TenantProperties());
        TenantWebMvcConfigurer configurer = new TenantWebMvcConfigurer(interceptor);
        InterceptorRegistry registry = new InterceptorRegistry();
        configurer.addInterceptors(registry);
        assertThat(configurer.interceptor()).isSameAs(interceptor);
    }

    @Test
    void interceptorGetter() {
        TenantInterceptor interceptor =
                new TenantInterceptor(
                        new HeaderTenantResolver("X-Tenant-Id"), new TenantProperties());
        TenantWebMvcConfigurer configurer = new TenantWebMvcConfigurer(interceptor);
        assertThat(configurer.interceptor()).isSameAs(interceptor);
    }

    @Test
    void resolverReferenceAvailable() {
        TenantResolver resolver = new HeaderTenantResolver("X-Tenant-Id");
        TenantInterceptor interceptor = new TenantInterceptor(resolver, new TenantProperties());
        assertThat(interceptor).isNotNull();
    }
}
