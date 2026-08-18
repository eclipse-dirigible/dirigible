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
import org.eclipse.dirigible.components.jobs.tenant.JobNameCreator;
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
 * A client-Java job must exist in a NON-DEFAULT tenant — including one provisioned after the class
 * was loaded.
 *
 * <p>
 * The per-tenant fan-out runs at class-load time, and a client-Java generation is JVM-wide and only
 * rebuilt when the Java synchronizer goes dirty on a publish ({@code JavaSynchronizer} is not
 * multitenant, so the synchronizer-retriggering post-provisioning step does not reach it either). A
 * tenant created afterwards therefore had no {@code Job} row and no Quartz trigger for any
 * client-Java job — no {@code JobHandler} bean, no {@code @Scheduled} method — and nothing pointed
 * at the cause: the Jobs perspective simply showed nothing to enable.
 *
 * <p>
 * Both phases go all the way to a real execution: the job's Quartz key is triggered by name and the
 * client bean's body has to publish into the tenant it fires for, which the test then draws back
 * inside that tenant's context. The Quartz name is computed through the platform's own
 * {@link JobNameCreator}, so a key the test and the registration merely agreed to spell the same
 * wrong way would fail; and the job body is only reached if the row, the {@code JobDetail} and the
 * tenant stamped into its job data are all there. Triggering by hand rather than waiting on a cron
 * is deliberate — the schedule is a "never" expression, so the job stays inert for the rest of the
 * suite instead of firing every second in the background.
 *
 * <p>
 * The phases differ only in the interleaving that is the actual hazard — a tenant appearing before
 * the class is loaded, and a tenant appearing after — and they share one tenant, because
 * provisioning a tenant is by far the most expensive thing here.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JavaJobTenantIT extends IntegrationTest {

    private static final String PROJECT = "java-job-tenant-it";

    /** The user-defined job group — the only one routed through the handler/engine dispatch. */
    private static final String JOB_GROUP = "defined";

    /** A cron that never fires: the test triggers the job itself, and nothing else should. */
    private static final String NEVER = "0 0 0 1 1 ? 2099";

    /** The tenant both phases use; provisioned by the first one that needs it. */
    private static DirigibleTestTenant tenant;

    /** Covers the compile + registration lag after a forced synchronization. */
    private static final int REGISTRATION_TIMEOUT_SECONDS = 90;

    /** Per attempt — short, because a failed attempt is retried with a fresh trigger. */
    private static final long RECEIVE_TIMEOUT_MILLIS = 2000;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private JobNameCreator jobNameCreator;

    @Test
    @Order(1)
    void aTenantProvisionedAfterTheClassWasLoadedGetsTheJobToo() throws SchedulerException {
        // The class is loaded while only the default tenant exists...
        publishJob("OnProvisionJob", "on-provision");

        // ...and the tenant appears afterwards. Nothing re-runs the load-time fan-out here, so only
        // the post-provisioning top-up can give this tenant the job.
        provisionTenant();

        assertJobRunsInTheTenant("OnProvisionJob", "on-provision");
    }

    @Test
    @Order(2)
    void aJobLoadedWhileATenantExistsIsRegisteredForIt() throws SchedulerException {
        // Reverse order: the tenant is already there when the class is loaded, so this is the
        // load-time fan-out's job.
        provisionTenant();

        publishJob("OnLoadJob", "on-load");

        assertJobRunsInTheTenant("OnLoadJob", "on-load");
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
        DirigibleTestTenant created = new DirigibleTestTenant(PROJECT);
        createTenants(created);
        scheduler.triggerJob(JobKey.jobKey("TenantsProvisioningJob", "system"));
        waitForTenantProvisioning(created);
        tenant = created;
    }

    /**
     * Trigger the job under the tenant's own Quartz key and draw back what its body published, retrying
     * the whole exchange: the registration lands some time after the forced synchronization returns,
     * and until it does there is no key to trigger.
     */
    private void assertJobRunsInTheTenant(String className, String destinationSuffix) {
        String queue = PROJECT + "-out-" + destinationSuffix;
        String jobName = "demo." + className;

        String[] received = new String[1];
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(REGISTRATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> tenantContext.execute(tenant.getId(), () -> {
                      JobKey jobKey = JobKey.jobKey(jobNameCreator.toTenantName(jobName), JOB_GROUP);
                      if (!scheduler.checkExists(jobKey)) {
                          return false;
                      }
                      scheduler.triggerJob(jobKey);
                      try {
                          received[0] = MessagingFacade.receiveFromQueue(queue, RECEIVE_TIMEOUT_MILLIS);
                          return true;
                      } catch (RuntimeException notYet) {
                          return false;
                      }
                  }));

        assertEquals("ran:" + destinationSuffix, received[0],
                "the job must run for the tenant it was triggered in, and its publish must come back to that tenant");
    }

    /** Write a client job source into the registry and reconcile it. */
    private void publishJob(String className, String destinationSuffix) {
        String source = """
                package demo;

                import org.eclipse.dirigible.sdk.component.Component;
                import org.eclipse.dirigible.sdk.job.JobHandler;
                import org.eclipse.dirigible.sdk.messaging.Producer;

                @Component
                public class %s implements JobHandler {

                    @Override
                    public String cron() {
                        return "%s";
                    }

                    @Override
                    public void run() {
                        Producer.sendToQueue("%s-out-%s", "ran:%s");
                    }
                }
                """.formatted(className, NEVER, PROJECT, destinationSuffix, destinationSuffix);

        repository.createResource(sourcePath(className), source.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private static String sourcePath(String className) {
        return IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/demo/" + className + ".java";
    }
}
