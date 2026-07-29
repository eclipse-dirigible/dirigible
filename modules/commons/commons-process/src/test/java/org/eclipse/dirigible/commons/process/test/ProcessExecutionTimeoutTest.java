/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.process.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.dirigible.commons.process.execution.ProcessExecutionException;
import org.eclipse.dirigible.commons.process.execution.ProcessExecutionOptions;
import org.eclipse.dirigible.commons.process.execution.ProcessExecutor;
import org.eclipse.dirigible.commons.process.execution.output.OutputsPair;
import org.eclipse.dirigible.commons.process.execution.output.ProcessResult;
import org.junit.Test;

/**
 * Tests the configurable process timeout of {@link ProcessExecutionOptions}.
 */
public class ProcessExecutionTimeoutTest {

    /** How long the long-running command would run if it were left alone. */
    private static final int LONG_COMMAND_SECONDS = 20;

    /** The timeout the long-running command is expected to be destroyed after. */
    private static final long TIMEOUT_MILLIS = 500;

    private static final boolean WINDOWS = System.getProperty("os.name")
                                                 .toLowerCase()
                                                 .contains("win");

    /**
     * A process that outlives its timeout is destroyed, and the failure surfaces to the caller instead
     * of being reported as an ordinary non-zero exit code.
     */
    @Test
    public void testProcessExceedingTimeoutIsDestroyed() {
        ProcessExecutionOptions options = new ProcessExecutionOptions();
        options.setTimeoutMillis(TIMEOUT_MILLIS);

        long start = System.nanoTime();
        Future<ProcessResult<OutputsPair>> future = ProcessExecutor.create()
                                                                   .executeProcess(longRunningCommand(), null, options);

        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertTrue("Expected a ProcessExecutionException cause but got: " + exception.getCause(),
                exception.getCause() instanceof ProcessExecutionException);
        assertTrue("The process should have been destroyed long before it finished on its own, but it took " + elapsedMillis + "ms",
                elapsedMillis < LONG_COMMAND_SECONDS * 1000L / 2);
    }

    /**
     * A process that finishes within its timeout is unaffected by the watchdog.
     */
    @Test
    public void testProcessWithinTimeoutSucceeds() throws ExecutionException, InterruptedException {
        ProcessExecutionOptions options = new ProcessExecutionOptions();
        options.setTimeoutMillis(60_000L);

        ProcessResult<OutputsPair> result = ProcessExecutor.create()
                                                           .executeProcess(quickCommand(), null, options)
                                                           .get();

        assertEquals(0, result.getExitCode());
    }

    /**
     * Not configuring a timeout keeps the previous unbounded behaviour.
     */
    @Test
    public void testProcessWithoutTimeoutSucceeds() throws ExecutionException, InterruptedException {
        ProcessResult<OutputsPair> result = ProcessExecutor.create()
                                                           .executeProcess(quickCommand(), null, new ProcessExecutionOptions())
                                                           .get();

        assertEquals(0, result.getExitCode());
    }

    /**
     * A non-positive timeout is a caller mistake - it would destroy the process immediately - and is
     * rejected where it is configured rather than at execution time.
     */
    @Test
    public void testNonPositiveTimeoutIsRejected() {
        ProcessExecutionOptions options = new ProcessExecutionOptions();

        assertThrows(IllegalArgumentException.class, () -> options.setTimeoutMillis(0L));
        assertThrows(IllegalArgumentException.class, () -> options.setTimeoutMillis(-1L));
    }

    /**
     * A command that runs for {@value #LONG_COMMAND_SECONDS} seconds. Windows has no {@code sleep}
     * executable, so a loopback ping of one packet per second stands in for it.
     *
     * @return the command line
     */
    private static String longRunningCommand() {
        return WINDOWS ? "ping -n " + (LONG_COMMAND_SECONDS + 1) + " 127.0.0.1" : "sleep " + LONG_COMMAND_SECONDS;
    }

    /**
     * A command that exits successfully straight away.
     *
     * @return the command line
     */
    private static String quickCommand() {
        return WINDOWS ? "cmd /c echo ok" : "echo ok";
    }
}
