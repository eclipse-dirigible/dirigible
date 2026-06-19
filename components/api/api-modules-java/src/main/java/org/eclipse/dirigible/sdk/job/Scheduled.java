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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.dirigible.sdk.component.Component;

/**
 * Schedules a client task on a Quartz cron expression. Two styles are supported, like Spring:
 *
 * <p>
 * <b>Class level</b> — annotate a class that either implements {@link JobHandler} or exposes a
 * public no-arg {@code run()} method:
 *
 * <pre>
 * {@literal @}Scheduled(expression = "0/30 * * * * ?")
 * public class CleanupJob implements JobHandler {
 *     public void run() { ... }
 * }
 * </pre>
 *
 * <p>
 * <b>Method level</b> — annotate a public no-arg method on a
 * {@link org.eclipse.dirigible.sdk.component.Component
 *
 * @Component} bean (Spring's {@code @Scheduled}-on-a-method style); the bean can host several such
 *             methods and use injected collaborators:
 *
 *             <pre>
 * {@literal @}Component
 * public class Maintenance {
 *     {@literal @}Scheduled(expression = "0 0 2 * * ?")
 *     public void nightlyRollup() { ... }
 * }
 * </pre>
 *
 *             <p>
 *             Hot-reload replaces the schedule transparently: the old trigger is cancelled and a
 *             new one registered with the updated class/method.
 */
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Scheduled {

    /**
     * Quartz cron expression (six or seven fields). For example {@code "0/30 * * * * ?"} fires every 30
     * seconds.
     */
    String expression();

}
