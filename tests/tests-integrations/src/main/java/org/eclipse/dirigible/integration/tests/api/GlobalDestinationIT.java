/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.api.messaging.TimeoutException;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.listeners.service.DestinationNameManager;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A destination marked global is the one destination a tenant does not rename.
 *
 * <p>
 * Every other destination a tenant touches is physically {@code <tenantId>###<name>}, which is what
 * keeps one tenant's messages out of another's — and what makes an integration queue two products
 * agreed on unreachable, since the other product neither knows the sending tenant nor should have
 * to. Both halves are asserted here against the platform's own producer and consumer, in a real
 * non-default tenant: the marked name arrives under the bare name a foreign consumer would bind to,
 * and the unmarked one demonstrably does not.
 *
 * <p>
 * The receiving side deliberately runs on a thread with no tenant context at all — the closest this
 * deployment can get to a consumer that has never heard of its tenants.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GlobalDestinationIT extends IntegrationTest {

    /** Named by the convention a global destination is documented with: {@code <vendor>.<purpose>}. */
    private static final String GLOBAL_QUEUE = "codbex.global-destination-it.orders";

    private static final String TENANT_SCOPED_QUEUE = "global-destination-it-orders";

    private static final String MESSAGE = "an order from another deployment";

    /** Generous: the message is already on the broker, so this only covers dispatch. */
    private static final long RECEIVE_TIMEOUT_MILLIS = 10_000;

    /** Short: this one is expected to expire, and the queue it draws from must stay empty. */
    private static final long EXPECTED_TO_EXPIRE_MILLIS = 2_000;

    /** The tenant both tests publish from; provisioned by the first one that needs it. */
    private static DirigibleTestTenant tenant;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    private Scheduler scheduler;

    @Test
    @Order(1)
    void aGlobalDestinationIsReachedByItsBareNameFromOutsideTheTenant() throws SchedulerException {
        provisionTenant();

        publish(DestinationNameManager.GLOBAL_MARKER + GLOBAL_QUEUE);

        assertEquals(MESSAGE, MessagingFacade.receiveFromQueue(GLOBAL_QUEUE, RECEIVE_TIMEOUT_MILLIS),
                "a global destination must resolve to the bare name for everyone bound to it");
    }

    @Test
    @Order(2)
    void aDestinationWithoutTheMarkerStaysScopedToItsTenant() throws SchedulerException {
        provisionTenant();

        publish(TENANT_SCOPED_QUEUE);

        assertThrows(TimeoutException.class, () -> MessagingFacade.receiveFromQueue(TENANT_SCOPED_QUEUE, EXPECTED_TO_EXPIRE_MILLIS),
                "without the marker the message stays on the tenant's own destination");

        String receivedInTheTenant =
                tenantContext.execute(tenant.getId(), () -> MessagingFacade.receiveFromQueue(TENANT_SCOPED_QUEUE, RECEIVE_TIMEOUT_MILLIS));
        assertEquals(MESSAGE, receivedInTheTenant, "the publishing tenant must still find its own message where it left it");
    }

    /** Publish from the non-default tenant, the context in which the rename applies. */
    private void publish(String destination) {
        tenantContext.execute(tenant.getId(), () -> {
            MessagingFacade.sendToQueue(destination, MESSAGE);
            return null;
        });
    }

    /**
     * Provision the shared tenant, once per class.
     *
     * <p>
     * The provisioning job is triggered explicitly rather than waited for: its schedule is
     * {@code DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS} wide, so a test that merely waited would
     * be racing the single firing at startup.
     */
    private void provisionTenant() throws SchedulerException {
        if (tenant != null) {
            return;
        }
        DirigibleTestTenant created = new DirigibleTestTenant("global-destination-it");
        createTenants(created);
        scheduler.triggerJob(JobKey.jobKey("TenantsProvisioningJob", "system"));
        waitForTenantProvisioning(created);
        tenant = created;
    }
}
