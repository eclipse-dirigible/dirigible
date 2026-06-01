/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a client Java class as a scheduled job managed by the Dirigible runtime.
 *
 * <p>
 * The annotated class must expose a public no-arg {@code run()} method. Dirigible will instantiate
 * the class once and invoke {@code run()} on the configured Quartz cron schedule. Hot-reload
 * replaces the instance transparently: the old schedule is cancelled and a new one is registered
 * with the updated class.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Scheduled(expression = "0/30 * * * * ?")
 * public class CleanupJob {
 *     public void run() { ... }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Scheduled {

    /**
     * Quartz cron expression (six or seven fields). For example {@code "0/30 * * * * ?"} fires every 30
     * seconds.
     */
    String expression();

}
