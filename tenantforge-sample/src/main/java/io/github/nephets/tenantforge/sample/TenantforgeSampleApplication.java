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
package io.github.nephets.tenantforge.sample;

import io.github.nephets.tenantforge.core.Tenant;
import io.github.nephets.tenantforge.core.TenantContext;
import io.github.nephets.tenantforge.core.TenantRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 示例应用：注册若干租户（acme / globex），并通过自动装配的拦截器按 {@code X-Tenant-Id} 请求头隔离租户上下文。 {@code /whoami}
 * 端点回显当前请求解析到的租户，验证「解析 → 写入上下文 → 业务读取」链路。
 */
@SpringBootApplication
@RestController
public class TenantforgeSampleApplication {

    private static final Logger log = LoggerFactory.getLogger(TenantforgeSampleApplication.class);

    private final TenantRegistry registry;

    public TenantforgeSampleApplication(TenantRegistry registry) {
        this.registry = registry;
    }

    public static void main(String[] args) {
        SpringApplication.run(TenantforgeSampleApplication.class, args);
    }

    @GetMapping("/whoami")
    public Map<String, String> whoami() {
        if (!TenantContext.isSet()) {
            if (log.isInfoEnabled()) {
                log.info("GET /whoami: 未设置租户上下文，返回 anonymous");
            }
            return Map.of("tenant", "anonymous");
        }
        String id = TenantContext.currentTenant();
        if (log.isInfoEnabled()) {
            log.info("GET /whoami: tenantId={}", id);
        }
        Map<String, String> body = new LinkedHashMap<>();
        body.put("tenant", id);
        if (registry.contains(id)) {
            body.put("name", registry.require(id).getName());
            body.put("known", "true");
        } else {
            body.put("known", "false");
        }
        return body;
    }

    @Bean
    public CommandLineRunner seedTenants() {
        return args -> {
            registry.register(Tenant.of("acme", "Acme Corporation"));
            registry.register(Tenant.of("globex", "Globex"));
            if (log.isInfoEnabled()) {
                log.info("示例租户已就绪: acme, globex (总数={})", registry.size());
            }
        };
    }
}
