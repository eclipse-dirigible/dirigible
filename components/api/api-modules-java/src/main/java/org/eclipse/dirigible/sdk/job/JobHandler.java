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
 * Self-describing contract for a scheduled job — the strong-interface style. A
 * {@link org.eclipse.dirigible.sdk.component.Component @Component} bean that implements this
 * interface IS a scheduled job: it supplies its own cron schedule via {@link #cron()} and its work
 * via {@link #run()}, with no {@code @Scheduled} annotation. This mirrors implementing
 * {@code org.quartz.Job} in Spring — the implementation carries everything.
 *
 * <p>
 * The alternative, method-level style is {@code @Scheduled} on a {@code @Component} method. A
 * single class uses one style or the other, never both.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Component
 * public class HourlyCleanup implements JobHandler {
 *     public String cron() { return "0 0 * * * ?"; }
 *     public void run()    { ... }
 * }
 * </pre>
 */
public interface JobHandler {

    /**
     * The Quartz cron expression (six or seven fields) at which {@link #run()} fires.
     *
     * @return the cron expression, e.g. {@code "0/30 * * * * ?"}
     */
    String cron();

    /** Fires at every cron tick declared by {@link #cron()}. */
    void run();
}
