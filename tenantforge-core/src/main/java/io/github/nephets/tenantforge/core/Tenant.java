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

import java.util.Map;
import java.util.Objects;

/** 租户：不可变标识，含名称与可选的自定义属性。 */
public final class Tenant {

    private final String id;

    private final String name;

    private final Map<String, String> properties;

    public Tenant(String id, String name, Map<String, String> properties) {
        this.id = Objects.requireNonNull(id, "tenant id 不能为 null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("tenant id 不能为空");
        }
        this.name = name == null ? id : name;
        this.properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    public static Tenant of(String id) {
        return new Tenant(id, id, null);
    }

    public static Tenant of(String id, String name) {
        return new Tenant(id, name, null);
    }

    public static Tenant of(String id, String name, Map<String, String> properties) {
        return new Tenant(id, name, properties);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Tenant that)) {
            return false;
        }
        return id.equals(that.id) && name.equals(that.name) && properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, properties);
    }

    @Override
    public String toString() {
        return "Tenant{id='" + id + "', name='" + name + "'}";
    }
}
