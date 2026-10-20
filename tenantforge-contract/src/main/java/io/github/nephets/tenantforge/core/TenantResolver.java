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
import java.util.Optional;

/** 租户解析器：从一组请求头/属性中解析出租户 id。无结果时返回 {@link Optional#empty()}。 */
@FunctionalInterface
public interface TenantResolver {

    Optional<String> resolve(Map<String, String> headers);
}
