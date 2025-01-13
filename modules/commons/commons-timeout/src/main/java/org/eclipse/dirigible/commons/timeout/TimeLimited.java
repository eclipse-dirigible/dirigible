/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.timeout;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.dirigible.commons.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class TimeLimited.
 */
public class TimeLimited {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TimeLimited.class);

    /** The Constant DIRIGIBLE_JOB_DEFAULT_TIMEOUT. */
    private static final String DIRIGIBLE_JOB_DEFAULT_TIMEOUT = "DIRIGIBLE_JOB_DEFAULT_TIMEOUT";

    /** The Constant DEFAULT_TIMEOUT. */
    private static final int DEFAULT_TIMEOUT = 3;

    /**
     * Run with timeout.
     *
     * @param runnable the runnable
     * @param timeout the timeout
     * @param timeUnit the time unit
     * @throws Exception the exception
     */
    public static synchronized void runWithTimeout(final Runnable runnable, long timeout, TimeUnit timeUnit) throws Exception {
        runWithTimeout(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                runnable.run();
                return null;
            }
        }, timeout, timeUnit);
    }

    /**
     * Run with timeout.
     *
     * @param <T> the generic type
     * @param callable the callable
     * @param timeout the timeout
     * @param timeUnit the time unit
     * @return the t
     * @throws Exception the exception
     */
    private static <T> T runWithTimeout(Callable<T> callable, long timeout, TimeUnit timeUnit) throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<T> future = executor.submit(callable);
        executor.shutdown();
        try {
            return future.get(timeout, timeUnit);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw new IllegalStateException(t);
            }
        }
    }

    /**
     * Gets the timeout.
     *
     * @return the timeout
     */
    public static final int getTimeout() {
        return Configuration.getAsInt(DIRIGIBLE_JOB_DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
    }

    /**
     * Gets the timeout in millis.
     *
     * @return the timeout in millis
     */
    public static final int getTimeoutInMillis() {
        return getTimeout() * 1000 * 60;
    }

}
