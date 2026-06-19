/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.job;

/**
 * Optional typed contract for a {@link Scheduled @Scheduled} class. Implementing this interface is
 * not required — the runtime still accepts any class that exposes a public no-arg {@code run()}
 * method via reflection — but implementations gain compile-time signature checking and the
 * scheduler dispatches them via a direct method call instead of a reflective {@code invoke}.
 *
 * <p>
 * The {@code @Scheduled} annotation remains the marker that turns a class into a scheduled job;
 * this interface only describes the run callback's shape.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Scheduled(expression = "0 0 * * * ?")
 * public class HourlyCleanup implements JobHandler {
 *     {@literal @}Override public void run() { ... }
 * }
 * </pre>
 */
public interface JobHandler {

    /** Fires at every cron tick declared on {@link Scheduled#expression() @Scheduled.expression}. */
    void run();
}
