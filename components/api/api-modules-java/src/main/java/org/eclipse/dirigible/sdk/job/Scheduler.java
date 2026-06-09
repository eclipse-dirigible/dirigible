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

import org.eclipse.dirigible.components.api.job.JobFacade;

/**
 * Returns a JSON listing of every Quartz job the platform knows about — name, group, cron, next
 * fire time, owner. Useful for admin endpoints and health-check pages that need to display the
 * scheduler state.
 * <p>
 * Job registration itself is declarative: either drop a {@code .job} JSON descriptor under a
 * project (picked up by the Job synchronizer) or annotate a class with
 * {@link org.eclipse.dirigible.sdk.job.Scheduled @Scheduled}. Programmatic schedule / unschedule is
 * not exposed through this class on purpose — it would bypass the synchronizer contract and leave
 * the live state out of sync with the declarative source.
 */
public final class Scheduler {

    private Scheduler() {}

    public static String list() {
        return JobFacade.getJobs();
    }
}
