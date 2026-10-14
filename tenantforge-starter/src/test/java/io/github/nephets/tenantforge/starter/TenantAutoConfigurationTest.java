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

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TenantAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class));

    @Test
    void registersAllBeans() {
        runner.run(
                context -> {
                    assertThat(context).hasSingleBean(TenantRegistry.class);
                    assertThat(context).hasSingleBean(TenantResolver.class);
                    assertThat(context).hasSingleBean(TenantInterceptor.class);
                    assertThat(context).hasSingleBean(TenantWebMvcConfigurer.class);
                    assertThat(context).hasSingleBean(TenantProperties.class);
                });
    }

    @Test
    void resolverDefaultsToHeaderResolver() {
        runner.run(
                context ->
                        assertThat(context.getBean(TenantResolver.class))
                                .isInstanceOf(HeaderTenantResolver.class));
    }

    @Test
    void honorsUserProvidedResolver() {
        runner.withUserConfiguration(CustomResolverConfig.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(TenantResolver.class);
                            assertThat(context.getBean(TenantResolver.class))
                                    .isSameAs(CustomResolverConfig.RESOLVER);
                        });
    }

    @Test
    void honorsUserProvidedRegistry() {
        runner.withUserConfiguration(CustomRegistryConfig.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(TenantRegistry.class);
                            assertThat(context.getBean(TenantRegistry.class))
                                    .isSameAs(CustomRegistryConfig.REGISTRY);
                        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomResolverConfig {

        static final TenantResolver RESOLVER = new HeaderTenantResolver("X-Tenant-Id");

        @Bean
        TenantResolver tenantResolver() {
            return RESOLVER;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRegistryConfig {

        static final TenantRegistry REGISTRY = new TenantRegistry();

        @Bean
        TenantRegistry tenantRegistry() {
            return REGISTRY;
        }
    }
}
