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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 虚拟线程上下文传播的专门断言：用 {@link Thread#ofVirtual()} 真实启动虚拟线程，断言其内部能读到
 * 主线程设置的租户，并验证隔离性（子线程改动不影响父线程、晚设置的租户不会泄漏进已创建的线程）。
 */
class TenantVirtualThreadPropagationTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void virtualThreadSeesTenantSetByParent() throws Exception {
        TenantContext.setTenant("acme");
        AtomicReference<String> seen = new AtomicReference<>();
        Thread vt = Thread.ofVirtual().start(() -> seen.set(TenantContext.currentTenant()));
        vt.join();
        assertThat(seen.get()).isEqualTo("acme");
    }

    @Test
    void propagateToVirtualThreadRunnableSeesTenant() throws Exception {
        TenantContext.setTenant("acme");
        AtomicReference<String> seen = new AtomicReference<>();
        Thread vt =
                Thread.ofVirtual()
                        .start(
                                TenantContext.propagateToVirtualThread(
                                        () -> seen.set(TenantContext.currentTenant())));
        vt.join();
        assertThat(seen.get()).isEqualTo("acme");
    }

    @Test
    void propagateToVirtualThreadCallableReturnsValue() throws Exception {
        TenantContext.setTenant("acme");
        Callable<String> task =
                TenantContext.propagateToVirtualThread(
                        () -> "value-for-" + TenantContext.currentTenant());
        AtomicReference<String> result = new AtomicReference<>();
        Thread vt =
                Thread.ofVirtual()
                        .start(
                                () -> {
                                    try {
                                        result.set(task.call());
                                    } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                    }
                                });
        vt.join();
        assertThat(result.get()).isEqualTo("value-for-acme");
    }

    @Test
    void captureReturnsCurrentTenant() {
        TenantContext.setTenant("acme");
        assertThat(TenantContext.capture()).isEqualTo("acme");
        TenantContext.clear();
        assertThat(TenantContext.capture()).isNull();
    }

    @Test
    void virtualThreadMutationDoesNotAffectParent() throws Exception {
        TenantContext.setTenant("acme");
        Thread vt =
                Thread.ofVirtual()
                        .start(
                                TenantContext.propagateToVirtualThread(
                                        () -> TenantContext.setTenant("inner")));
        vt.join();
        assertThat(TenantContext.currentTenant()).isEqualTo("acme");
    }

    @Test
    void virtualThreadCreatedBeforeTenantSetDoesNotSeeIt() throws Exception {
        TenantContext.clear();
        AtomicReference<String> seen = new AtomicReference<>("init");
        Thread vt = Thread.ofVirtual().start(() -> seen.set(TenantContext.currentTenant()));
        vt.join();
        TenantContext.setTenant("late");
        assertThat(seen.get()).isNull();
    }

    @Test
    void nestedVirtualThreadPropagation() throws Exception {
        TenantContext.setTenant("acme");
        AtomicReference<String> seen = new AtomicReference<>();
        Runnable inner =
                TenantContext.propagateToVirtualThread(
                        () -> seen.set(TenantContext.currentTenant()));
        Thread outer =
                Thread.ofVirtual()
                        .start(
                                TenantContext.propagateToVirtualThread(
                                        () -> {
                                            Thread innerThread = Thread.ofVirtual().start(inner);
                                            try {
                                                innerThread.join();
                                            } catch (InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                            }
                                        }));
        outer.join();
        assertThat(seen.get()).isEqualTo("acme");
    }

    @Test
    void manyVirtualThreadsConcurrentlySeeTenant() throws Exception {
        TenantContext.setTenant("acme");
        int count = 50;
        List<AtomicReference<String>> slots = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AtomicReference<String> slot = new AtomicReference<>();
            slots.add(slot);
            threads.add(
                    Thread.ofVirtual()
                            .start(
                                    TenantContext.propagateToVirtualThread(
                                            () -> slot.set(TenantContext.currentTenant()))));
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertThat(slots).hasSize(count);
        assertThat(slots.stream().map(AtomicReference::get)).containsOnly("acme");
    }

    @Test
    void noTenantPropagatesAsNull() throws Exception {
        TenantContext.clear();
        AtomicReference<String> seen = new AtomicReference<>("init");
        Thread vt =
                Thread.ofVirtual()
                        .start(
                                TenantContext.propagateToVirtualThread(
                                        () -> seen.set(TenantContext.currentTenant())));
        vt.join();
        assertThat(seen.get()).isNull();
    }

    @Test
    void runWithInsideVirtualThreadIsolatesAndRestores() throws Exception {
        TenantContext.setTenant("acme");
        AtomicReference<String> during = new AtomicReference<>();
        AtomicReference<String> after = new AtomicReference<>();
        Thread vt =
                Thread.ofVirtual()
                        .start(
                                TenantContext.propagateToVirtualThread(
                                        () -> {
                                            during.set(TenantContext.currentTenant());
                                            TenantContext.runWith("globex", () -> {});
                                            after.set(TenantContext.currentTenant());
                                        }));
        vt.join();
        assertThat(during.get()).isEqualTo("acme");
        assertThat(after.get()).isEqualTo("acme");
    }

    @Test
    void exceptionInsideWrappedTaskDoesNotCorruptParent() throws Exception {
        TenantContext.setTenant("acme");
        // 显式转型为 Runnable：仅含 throw 的 lambda 同时兼容 Runnable 与 Callable，会产生重载歧义
        Runnable exploding =
                () -> {
                    throw new IllegalStateException("boom");
                };
        Thread vt = Thread.ofVirtual().start(TenantContext.propagateToVirtualThread(exploding));
        vt.join();
        assertThat(TenantContext.currentTenant()).isEqualTo("acme");
    }

    @Test
    void nullTaskIsRejected() {
        assertThatThrownBy(() -> TenantContext.propagateToVirtualThread((Runnable) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TenantContext.propagateToVirtualThread((Callable<?>) null))
                .isInstanceOf(NullPointerException.class);
    }
}
