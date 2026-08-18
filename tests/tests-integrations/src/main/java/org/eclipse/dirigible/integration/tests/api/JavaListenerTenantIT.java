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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
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
 * The producer and the client-Java subscriber must meet on the same physical destination in a
 * NON-DEFAULT tenant.
 *
 * <p>
 * This is the symmetry the whole glue layer of a generated intent application rests on: the
 * generated repository publishes through {@code Producer.sendToTopic}, and every process trigger,
 * notification, roll-up, transition and posting subscribes on the client-Java
 * {@code MessageHandler} / {@code @Listener} path. The producer resolves a tenant-prefixed
 * destination per call while the subscriber used to bind the raw name once for the whole JVM, so
 * outside the default tenant the two never met and no reaction ever fired — invisible in
 * single-tenant deployments, in ordinary development, and in every other IT, all of which run on
 * the default tenant.
 *
 * <p>
 * Both phases round-trip a real message: a client listener on a topic echoes onto a queue, and the
 * test draws that echo back inside the tenant's context. Both legs go through the platform's own
 * naming, so neither would pass on a prefix that the producer and the consumer merely agreed to
 * spell the same wrong way. They differ only in the order of the two events whose interleaving is
 * the actual hazard — a tenant appearing before the class is loaded, and a tenant appearing after —
 * and they run in that order against one shared tenant, because provisioning a tenant is by far the
 * most expensive thing here.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JavaListenerTenantIT extends IntegrationTest {

    private static final String PROJECT = "java-listener-tenant-it";

    /** The tenant both phases use; provisioned by the first one that needs it. */
    private static DirigibleTestTenant tenant;

    /** How long a round trip may take, covering the compile + subscribe lag after a forced sync. */
    private static final int ROUND_TRIP_TIMEOUT_SECONDS = 90;

    /** Per attempt — short, because a failed attempt is retried with a fresh publish. */
    private static final long RECEIVE_TIMEOUT_MILLIS = 2000;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    private Scheduler scheduler;

    @Test
    @Order(1)
    void aTenantProvisionedAfterTheClassWasLoadedGetsSubscribedToo() throws SchedulerException {
        // The class is loaded while only the default tenant exists...
        publishEcho("OnProvision", "on-provision");

        // ...and the tenant appears afterwards. A client-Java generation is JVM-wide and is only
        // rebuilt on publish, so nothing re-runs the load-time fan-out here — only the
        // post-provisioning top-up can make this round trip succeed.
        provisionTenant();

        assertRoundTrip("on-provision");
    }

    @Test
    @Order(2)
    void aMessagePublishedInANonDefaultTenantReachesItsClientJavaListener() throws SchedulerException {
        // Reverse order: the tenant is already there when the class is loaded, so this is the
        // load-time fan-out's job.
        provisionTenant();

        publishEcho("OnLoad", "on-load");

        assertRoundTrip("on-load");
    }

    /**
     * Provision the shared tenant, once per class.
     *
     * <p>
     * The provisioning job is triggered explicitly rather than waited for: its schedule is
     * {@code DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS} wide (15 minutes by default) and the
     * only prompt firing is the one at startup, so a test that merely waited would be racing that boot
     * firing — which is exactly how a second tenant in the same class silently timed out.
     */
    private void provisionTenant() throws SchedulerException {
        if (tenant != null) {
            return;
        }
        DirigibleTestTenant created = new DirigibleTestTenant("java-listener-tenant-it");
        createTenants(created);
        scheduler.triggerJob(JobKey.jobKey("TenantsProvisioningJob", "system"));
        waitForTenantProvisioning(created);
        tenant = created;
    }

    /**
     * Publish into the tenant's topic and draw the listener's echo back off its queue, retrying the
     * whole exchange: a topic does not retain messages, so a send that lands before the subscription is
     * open is simply lost and has to be repeated.
     */
    private void assertRoundTrip(String destinationSuffix) {
        String topic = PROJECT + "-in-" + destinationSuffix;
        String queue = PROJECT + "-out-" + destinationSuffix;
        String message = "ping-" + destinationSuffix;

        String[] received = new String[1];
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(ROUND_TRIP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> tenantContext.execute(tenant.getId(), () -> {
                      MessagingFacade.sendToTopic(topic, message);
                      try {
                          received[0] = MessagingFacade.receiveFromQueue(queue, RECEIVE_TIMEOUT_MILLIS);
                          return true;
                      } catch (RuntimeException notYet) {
                          return false;
                      }
                  }));

        assertEquals("echo:" + message, received[0],
                "the listener must receive what a producer in the same tenant published, and its own publish must come back to that tenant");
    }

    /** Write a client listener source into the registry and reconcile it. */
    private void publishEcho(String className, String destinationSuffix) {
        String source = """
                package demo;

                import org.eclipse.dirigible.sdk.component.Component;
                import org.eclipse.dirigible.sdk.messaging.ListenerKind;
                import org.eclipse.dirigible.sdk.messaging.MessageHandler;
                import org.eclipse.dirigible.sdk.messaging.Producer;

                @Component
                public class %s implements MessageHandler {

                    @Override
                    public String destination() {
                        return "%s-in-%s";
                    }

                    @Override
                    public ListenerKind kind() {
                        return ListenerKind.TOPIC;
                    }

                    @Override
                    public void onMessage(String message) {
                        Producer.sendToQueue("%s-out-%s", "echo:" + message);
                    }
                }
                """.formatted(className, PROJECT, destinationSuffix, PROJECT, destinationSuffix);

        repository.createResource(sourcePath(className), source.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private static String sourcePath(String className) {
        return IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/demo/" + className + ".java";
    }
}
