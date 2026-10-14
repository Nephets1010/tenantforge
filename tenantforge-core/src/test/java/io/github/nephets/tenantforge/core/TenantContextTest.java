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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void initiallyNotSet() {
        TenantContext.clear();
        assertThat(TenantContext.isSet()).isFalse();
        assertThat(TenantContext.currentTenant()).isNull();
    }

    @Test
    void setTenantStoresCurrent() {
        TenantContext.setTenant("acme");
        assertThat(TenantContext.currentTenant()).isEqualTo("acme");
        assertThat(TenantContext.isSet()).isTrue();
    }

    @Test
    void clearRemovesCurrent() {
        TenantContext.setTenant("acme");
        TenantContext.clear();
        assertThat(TenantContext.isSet()).isFalse();
    }

    @Test
    void runWithSetsDuringAndRestoresAfter() {
        TenantContext.setTenant("outer");
        TenantContext.runWith(
                "inner",
                () -> {
                    assertThat(TenantContext.currentTenant()).isEqualTo("inner");
                });
        assertThat(TenantContext.currentTenant()).isEqualTo("outer");
    }

    @Test
    void runWithWithNoPriorRestoresToNull() {
        TenantContext.clear();
        TenantContext.runWith(
                "inner", () -> assertThat(TenantContext.currentTenant()).isEqualTo("inner"));
        assertThat(TenantContext.currentTenant()).isNull();
    }

    @Test
    void nestedRunWithRestoresOuter() {
        TenantContext.runWith(
                "a",
                () -> {
                    TenantContext.runWith(
                            "b",
                            () -> {
                                assertThat(TenantContext.currentTenant()).isEqualTo("b");
                            });
                    assertThat(TenantContext.currentTenant()).isEqualTo("a");
                });
        assertThat(TenantContext.currentTenant()).isNull();
    }

    @Test
    void withTenantReturnsValue() throws Exception {
        String result =
                TenantContext.withTenant(
                        "acme", () -> "value-for-" + TenantContext.currentTenant());
        assertThat(result).isEqualTo("value-for-acme");
    }

    @Test
    void withTenantRestoresAfter() throws Exception {
        TenantContext.setTenant("keep");
        TenantContext.withTenant("temp", () -> "x");
        assertThat(TenantContext.currentTenant()).isEqualTo("keep");
    }

    @Test
    void withTenantRestoresEvenOnException() {
        TenantContext.setTenant("keep");
        assertThatThrownBy(
                        () ->
                                TenantContext.withTenant(
                                        "temp",
                                        () -> {
                                            throw new IllegalStateException("boom");
                                        }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(TenantContext.currentTenant()).isEqualTo("keep");
    }

    @Test
    void requireTenantReturnsWhenSet() {
        TenantContext.setTenant("acme");
        assertThat(TenantContext.requireTenant()).isEqualTo("acme");
    }

    @Test
    void requireTenantThrowsWhenNotSet() {
        TenantContext.clear();
        assertThatThrownBy(TenantContext::requireTenant)
                .isInstanceOf(TenantNotResolvedException.class);
    }

    @Test
    void childThreadInheritsTenant() throws Exception {
        TenantContext.setTenant("parent");
        String[] seen = new String[1];
        Thread child = new Thread(() -> seen[0] = TenantContext.currentTenant());
        child.start();
        child.join(2000);
        assertThat(seen[0]).isEqualTo("parent");
    }

    @Test
    void setTenantOverwrites() {
        TenantContext.setTenant("a");
        TenantContext.setTenant("b");
        assertThat(TenantContext.currentTenant()).isEqualTo("b");
    }

    @Test
    void runWithOverwritesDuringExecution() {
        TenantContext.runWith(
                "a",
                () -> {
                    TenantContext.setTenant("b");
                    assertThat(TenantContext.currentTenant()).isEqualTo("b");
                });
    }

    @Test
    void withTenantGenericTypePreserved() throws Exception {
        Integer value = TenantContext.withTenant("acme", () -> 42);
        assertThat(value).isEqualTo(42);
    }

    @Test
    void clearIsIdempotent() {
        TenantContext.setTenant("a");
        TenantContext.clear();
        TenantContext.clear();
        assertThat(TenantContext.isSet()).isFalse();
    }

    @Test
    void runWithRestoresPreviousNullAfterNestedException() {
        assertThatThrownBy(
                        () ->
                                TenantContext.runWith(
                                        "x",
                                        () -> {
                                            throw new RuntimeException("e");
                                        }))
                .isInstanceOf(RuntimeException.class);
        assertThat(TenantContext.currentTenant()).isNull();
    }

    @Test
    void withTenantNonNullResultAfterNested() throws Exception {
        String r =
                TenantContext.withTenant(
                        "a",
                        () -> {
                            TenantContext.runWith("b", () -> {});
                            return TenantContext.currentTenant();
                        });
        assertThat(r).isEqualTo("a");
    }
}
