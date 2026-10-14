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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 租户上下文：基于 {@link InheritableThreadLocal} 的当前租户持有器。
 *
 * <p>提供 {@link #setTenant}/{@link #currentTenant}/{@link #clear} 等基础操作，以及 {@link #runWith}/{@link
 * #withTenant} 的 try-with-resources 风格包装，确保任务执行后恢复原上下文。
 */
public final class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);

    private static final ThreadLocal<String> CURRENT = new InheritableThreadLocal<>();

    private TenantContext() {}

    /** 设置当前线程的租户 id（覆盖已有值）。 */
    public static void setTenant(String tenantId) {
        if (log.isDebugEnabled()) {
            log.debug("设置当前线程租户上下文: tenantId={}", tenantId);
        }
        CURRENT.set(tenantId);
    }

    /** 返回当前线程的租户 id；未设置时为 {@code null}。 */
    public static String currentTenant() {
        return CURRENT.get();
    }

    /** 当前线程是否已设置租户。 */
    public static boolean isSet() {
        return CURRENT.get() != null;
    }

    /** 清除当前线程的租户（必须配对调用，避免线程池污染）。 */
    public static void clear() {
        if (log.isDebugEnabled()) {
            log.debug("清除当前线程租户上下文");
        }
        CURRENT.remove();
    }

    /** 在指定租户上下文中执行 {@link Runnable}，结束（含异常）后恢复原上下文。 */
    public static void runWith(String tenantId, Runnable task) {
        if (log.isDebugEnabled()) {
            log.debug("进入租户上下文执行 Runnable: tenantId={}", tenantId);
        }
        String previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            task.run();
        } finally {
            restore(previous);
        }
    }

    /** 在指定租户上下文中执行 {@link Callable} 并返回其结果，结束（含异常）后恢复原上下文。 */
    public static <T> T withTenant(String tenantId, Callable<T> task) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("进入租户上下文执行 Callable: tenantId={}", tenantId);
        }
        String previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            return task.call();
        } finally {
            restore(previous);
        }
    }

    /** 返回当前租户 id，未设置时抛出 {@link TenantNotResolvedException}。 */
    public static String requireTenant() {
        String tenantId = CURRENT.get();
        if (tenantId == null) {
            log.warn("当前线程未解析到租户上下文");
            throw new TenantNotResolvedException();
        }
        return tenantId;
    }

    /**
     * 捕获当前租户 id，供 {@link #propagateToVirtualThread(Runnable)} 等显式传播使用。
     *
     * <p>返回 {@code null} 表示「当前没有租户」。传播包装器会据此在目标线程上清理上下文（而不是设置一个空值），
     * 并在任务结束后恢复该线程自身原本的值，因此不会把「无租户」错误地写进复用中的线程。
     */
    public static String capture() {
        return CURRENT.get();
    }

    /**
     * 把当前租户上下文显式传播到虚拟线程：捕获<b>调用时刻</b>的租户 id，返回一个包装后的 {@link Runnable}；该 Runnable
     * 被虚拟线程执行时会先设置租户、执行任务，结束后恢复该线程原本的上下文。
     *
     * <p>用法：
     *
     * <pre>{@code
     * TenantContext.setTenant("acme");
     * Thread vt = Thread.ofVirtual().start(TenantContext.propagateToVirtualThread(() -> {
     *     // 这里 TenantContext.currentTenant() 为 "acme"
     * }));
     * }</pre>
     *
     * <p>之所以需要显式传播：{@link InheritableThreadLocal} 的继承发生在线程创建瞬间， 若任务被提交到 早已创建好的线程（如线程池中的载体线程、或复用中的
     * executor），继承并不发生；而本方法在任务真正 执行时重新设置上下文，因此与线程何时被创建无关。
     *
     * <p><b>名字里的「虚拟线程」是动机场景，不是使用限制</b>：返回的包装器是线程无关的， 在平台线程、线程池、executor 上同样安全可用 ——
     * 它只是「在执行时设置租户、结束时恢复」，与执行它的线程种类无关。
     */
    public static Runnable propagateToVirtualThread(Runnable task) {
        Objects.requireNonNull(task, "task 不能为 null");
        String tenantId = CURRENT.get();
        if (log.isDebugEnabled()) {
            log.debug("构造显式租户传播包装(Runnable): tenantId={}", tenantId);
        }
        return () -> runWith(tenantId, task);
    }

    /**
     * {@link Callable} 版本的显式传播，语义同 {@link #propagateToVirtualThread(Runnable)}。
     *
     * <p>同样与线程种类无关，可安全用于平台线程、线程池与 executor。
     */
    public static <T> Callable<T> propagateToVirtualThread(Callable<T> task) {
        Objects.requireNonNull(task, "task 不能为 null");
        String tenantId = CURRENT.get();
        if (log.isDebugEnabled()) {
            log.debug("构造显式租户传播包装(Callable): tenantId={}", tenantId);
        }
        return () -> withTenant(tenantId, task);
    }

    private static void restore(String previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
