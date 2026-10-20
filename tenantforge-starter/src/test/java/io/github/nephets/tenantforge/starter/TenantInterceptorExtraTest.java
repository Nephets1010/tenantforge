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
import io.github.nephets.tenantforge.core.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@link TenantInterceptor} 的边界与契约补充测试（与 {@code TenantInterceptorTest} 互补）。 */
class TenantInterceptorExtraTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private TenantProperties properties() {
        return new TenantProperties();
    }

    @Test
    void clearsContextEvenWhenPresetBeforeRequest() {
        // 进入拦截器前已存在上下文：preHandle 覆盖为请求租户，afterCompletion 必须无条件清除，避免泄漏。
        TenantContext.setTenant("preset");
        TenantInterceptor interceptor =
                new TenantInterceptor(
                        new HeaderTenantResolver(properties().getHeaderName()), properties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "acme");
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        assertThat(TenantContext.currentTenant()).isEqualTo("acme");
        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        assertThat(TenantContext.isSet()).isFalse();
    }

    @Test
    void usesDefaultWhenHeaderBlank() {
        TenantProperties props = properties();
        props.setDefaultTenantId("anon");
        TenantInterceptor interceptor =
                new TenantInterceptor(new HeaderTenantResolver(props.getHeaderName()), props);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "   ");
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        assertThat(TenantContext.currentTenant()).isEqualTo("anon");
    }

    @Test
    void afterCompletionClearsContextEvenOnException() {
        TenantInterceptor interceptor =
                new TenantInterceptor(
                        new HeaderTenantResolver(properties().getHeaderName()), properties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "acme");
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        interceptor.afterCompletion(
                request, new MockHttpServletResponse(), new Object(), new RuntimeException("boom"));
        assertThat(TenantContext.isSet()).isFalse();
    }

    @Test
    void nullResolverRejected() {
        assertThatThrownBy(() -> new TenantInterceptor(null, properties()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullPropertiesRejected() {
        assertThatThrownBy(
                        () -> new TenantInterceptor(new HeaderTenantResolver("X-Tenant-Id"), null))
                .isInstanceOf(NullPointerException.class);
    }
}
