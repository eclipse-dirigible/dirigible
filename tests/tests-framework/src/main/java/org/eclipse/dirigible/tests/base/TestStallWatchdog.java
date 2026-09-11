/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.base;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Dumps every thread of the test JVM when a test or lifecycle method has been running for
 * {@link #STALL_THRESHOLD_MINUTES} minutes, while it is still running.
 *
 * <p>
 * A wedged integration test used to take the whole CI shard with it: nothing bounded a single
 * method, so the job ran into its own {@code timeout-minutes}, the run was cancelled, and neither
 * the failsafe reports nor any thread dump survived - leaving nothing that told a hang apart from a
 * shard that merely ran out of budget (#7283). {@link IntegrationTest} now bounds every method with
 * {@code @Timeout}, and this watchdog takes the picture that bound cannot. It fires a few minutes
 * EARLIER, from its own thread, so the stalled stack is still live rather than already unwound; and
 * it fires even where the timeout's interrupt cannot land - a blocking socket read or a monitor
 * deadlock ignores {@code Thread.interrupt()}, and the timeout then never completes either.
 *
 * <p>
 * The dump is written under {@code target/failsafe-reports} - which the workflows upload even when
 * the job is cancelled - BEFORE it is logged, because if what stalled is the logging path itself
 * the file is the only copy that gets out.
 */
class TestStallWatchdog implements InvocationInterceptor {

    /**
     * How long a single test or lifecycle method may run before its stack is assumed to be stuck. The
     * slowest method in the suite measured 7.6 minutes (a browser journey), and {@link IntegrationTest}
     * fails a method at 15, so this sits between the two: late enough not to report healthy tests,
     * early enough to still catch the timeout in the act.
     */
    static final int STALL_THRESHOLD_MINUTES = 12;

    private static final Logger LOGGER = LoggerFactory.getLogger(TestStallWatchdog.class);

    private static final Path DUMP_FOLDER = Path.of("target", "failsafe-reports");

    private static final DateTimeFormatter DUMP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                                                                             .withZone(ZoneId.systemDefault());

    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "test-stall-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        watch(invocation, invocationContext);
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        watch(invocation, invocationContext);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        watch(invocation, invocationContext);
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        watch(invocation, invocationContext);
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        watch(invocation, invocationContext);
    }

    @Override
    public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        watch(invocation, invocationContext);
    }

    private void watch(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext) throws Throwable {
        Method method = invocationContext.getExecutable();
        String invoked = method.getDeclaringClass()
                               .getSimpleName()
                + "." + method.getName();
        Thread invocationThread = Thread.currentThread();

        ScheduledFuture<?> scheduledDump =
                WATCHDOG.schedule(() -> dumpThreads(invoked, invocationThread), STALL_THRESHOLD_MINUTES, TimeUnit.MINUTES);
        try {
            invocation.proceed();
        } finally {
            scheduledDump.cancel(false);
        }
    }

    private static void dumpThreads(String invoked, Thread invocationThread) {
        String dump = renderDump(invoked, invocationThread);
        Path dumpFile = DUMP_FOLDER.resolve(invoked + "-threaddump-" + DUMP_TIMESTAMP.format(Instant.now()) + ".txt");
        try {
            Files.createDirectories(DUMP_FOLDER);
            Files.writeString(dumpFile, dump);
        } catch (IOException e) {
            LOGGER.error("Cannot write the thread dump of the stalled [{}] to [{}]", invoked, dumpFile, e);
        }
        LOGGER.error("[{}] has been running for [{}] minutes and looks stalled. Dumped all threads to [{}]:\n{}", invoked,
                STALL_THRESHOLD_MINUTES, dumpFile, dump);
    }

    private static String renderDump(String invoked, Thread invocationThread) {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        StringBuilder dump = new StringBuilder(64 * 1024);

        dump.append('[')
            .append(invoked)
            .append("] has been running for ")
            .append(STALL_THRESHOLD_MINUTES)
            .append(" minutes on thread \"")
            .append(invocationThread.getName())
            .append("\" id=")
            .append(invocationThread.threadId())
            .append('\n');

        long[] deadlocked = threads.findDeadlockedThreads();
        if (deadlocked != null) {
            dump.append("DEADLOCK detected between thread ids ")
                .append(Arrays.toString(deadlocked))
                .append('\n');
        }

        for (ThreadInfo threadInfo : threads.dumpAllThreads(true, true)) {
            appendThread(dump, threadInfo);
        }
        return dump.toString();
    }

    /**
     * Renders one thread the way a {@code jstack} entry reads. {@link ThreadInfo#toString()} is not
     * used: it truncates the stack at eight frames, and which test call reached the blocking point is
     * usually deeper than that.
     */
    private static void appendThread(StringBuilder dump, ThreadInfo threadInfo) {
        dump.append("\n\"")
            .append(threadInfo.getThreadName())
            .append("\" id=")
            .append(threadInfo.getThreadId())
            .append(' ')
            .append(threadInfo.getThreadState());
        if (threadInfo.getLockName() != null) {
            dump.append(" on ")
                .append(threadInfo.getLockName());
        }
        if (threadInfo.getLockOwnerName() != null) {
            dump.append(" owned by \"")
                .append(threadInfo.getLockOwnerName())
                .append("\" id=")
                .append(threadInfo.getLockOwnerId());
        }
        dump.append('\n');
        for (StackTraceElement frame : threadInfo.getStackTrace()) {
            dump.append("\tat ")
                .append(frame)
                .append('\n');
        }
    }

}
