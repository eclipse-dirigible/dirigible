/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.outbox;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.jobs.SystemJob;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Drains every tenant's event outbox of entries the in-process dispatch could not hand to the
 * broker — the recovery half of the transactional outbox. Each tick runs inside a tenant execution
 * scope, both because the outbox table lives in the tenant's schema and because the destination
 * name and the tenant stamped on the message are derived from the current tenant.
 *
 * <p>
 * Instantiated by Quartz; collaborators wired by {@code AutoWiringSpringBeanJobFactory} — the same
 * documented exception to the constructor-injection convention that applies to every
 * {@link SystemJob} subclass in this repo.
 */
@Component
class EventOutboxRelayJob extends SystemJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventOutboxRelayJob.class);

    @Autowired
    private EventOutbox outbox;

    @Autowired
    private TenantContext tenantContext;

    @Override
    protected String getJobKey() {
        return this.getClass()
                   .getSimpleName();
    }

    @Override
    protected String getTriggerKey() {
        return this.getClass()
                   .getSimpleName()
                + "Trigger";
    }

    @Override
    protected SimpleScheduleBuilder getSchedule() {
        int seconds = DirigibleConfig.EVENT_OUTBOX_RELAY_INTERVAL_SECONDS.getIntValue();
        return simpleSchedule().withIntervalInSeconds(seconds)
                               .repeatForever()
                               .withMisfireHandlingInstructionNextWithExistingCount();
    }

    @Override
    public void execute(JobExecutionContext context) {
        tenantContext.executeForEachTenant(() -> {
            try {
                outbox.relay();
            } catch (RuntimeException ex) {
                LOGGER.error("Event outbox relay failed for the current tenant; other tenants are unaffected.", ex);
            }
            return null;
        });
    }

}
