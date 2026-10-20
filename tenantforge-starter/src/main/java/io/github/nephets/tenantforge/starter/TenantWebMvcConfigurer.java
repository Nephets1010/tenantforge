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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 将 {@link TenantInterceptor} 注册到 Spring MVC 拦截器链。 */
public class TenantWebMvcConfigurer implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(TenantWebMvcConfigurer.class);

    private final TenantInterceptor interceptor;

    public TenantWebMvcConfigurer(TenantInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (log.isDebugEnabled()) {
            log.debug("注册租户拦截器到 Spring MVC 拦截器链");
        }
        registry.addInterceptor(interceptor);
    }

    public TenantInterceptor interceptor() {
        return interceptor;
    }
}
