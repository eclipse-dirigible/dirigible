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

/**
 * Schedules a public no-arg method of a {@link org.eclipse.dirigible.sdk.component.Component
 * &#64;Component} bean on a Quartz cron expression — the method-level style, like Spring's
 * {@code @Scheduled}-on-a-method. One bean can host several scheduled methods and use injected
 * collaborators.
 *
 * <p>
 * The alternative, strong-interface style is a {@code @Component} bean implementing
 * {@link JobHandler} (which supplies its own {@code cron()}). A class uses one style or the other,
 * never both.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Component
 * public class Maintenance {
 *     {@literal @}Scheduled(expression = "0 0 2 * * ?")
 *     public void nightlyRollup() { ... }
 * }
 * </pre>
 *
 * <p>
 * Hot-reload replaces the schedule transparently: the old trigger is cancelled and a new one
 * registered.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Scheduled {

    /**
     * Quartz cron expression (six or seven fields). For example {@code "0/30 * * * * ?"} fires every 30
     * seconds.
     */
    String expression();

}
