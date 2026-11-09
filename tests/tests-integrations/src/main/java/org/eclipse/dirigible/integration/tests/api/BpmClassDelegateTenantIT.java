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
import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An async {@code flowable:class} service task of a non-default tenant's process runs in that
 * tenant's scope (#8549).
 *
 * <p>
 * The step runs on the async executor, whose threads carry no tenant. Until the class delegate
 * opened the scope of the tenant stamped on its execution, as {@code ${JavaTask}} always did, its
 * repositories, data sources and messaging resolved to the default tenant: a lookup of the tenant's
 * row found nothing and the process took the wrong branch with no error logged. Here the delegate
 * publishes to a queue - a tenant-scoped resource - and the test draws the message back inside the
 * tenant it started the process in, so a step that ran in the default tenant leaves nothing to
 * receive.
 */
class BpmClassDelegateTenantIT extends IntegrationTest {

    private static final String PROJECT = "bpm-class-delegate-tenant-it";

    private static final String PROCESS_KEY = "class-delegate-tenant-scope";

    private static final String QUEUE = PROJECT + "-out";

    private static final String PROCESS_TEMPLATE =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="%s" name="Class delegate tenant scope" isExecutable="true">
                        <startEvent id="start"/>
                        <sequenceFlow id="f1" sourceRef="start" targetRef="probe"/>
                        <serviceTask id="probe" name="Probe" flowable:async="true" flowable:class="demo.TenantProbe"/>
                        <sequenceFlow id="f2" sourceRef="probe" targetRef="end"/>
                        <endEvent id="end"/>
                      </process>
                    </definitions>
                    """;

    /** Covers the deployment and the async executor's acquisition after the forced synchronization. */
    private static final int TIMEOUT_SECONDS = 90;

    /** Per attempt - short, because a failed attempt is simply polled again. */
    private static final long RECEIVE_TIMEOUT_MILLIS = 2000;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    private ProcessEngine processEngine;

    @Test
    void anAsyncClassDelegateRunsInItsProcessInstancesTenant() {
        DirigibleTestTenant tenant = new DirigibleTestTenant(PROJECT);
        createTenants(tenant);
        waitForTenantProvisioning(tenant);

        publishFixture();

        startProcessOnceDeployed(tenant.getId());

        String[] received = new String[1];
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> tenantContext.execute(tenant.getId(), () -> {
                      try {
                          received[0] = MessagingFacade.receiveFromQueue(QUEUE, RECEIVE_TIMEOUT_MILLIS);
                          return true;
                      } catch (RuntimeException notYet) {
                          return false;
                      }
                  }));

        assertEquals("ran", received[0], "the async class step must publish into the tenant its process instance belongs to");
    }

    /** The process is deployed to the tenant some time after the forced synchronization returns. */
    private void startProcessOnceDeployed(String tenantId) {
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .ignoreExceptions()
                  .until(() -> processEngine.getRuntimeService()
                                            .startProcessInstanceByKeyAndTenantId(PROCESS_KEY, tenantId) != null);
    }

    private void publishFixture() {
        String delegate = """
                package demo;

                import org.eclipse.dirigible.sdk.messaging.Producer;
                import org.flowable.engine.delegate.DelegateExecution;
                import org.flowable.engine.delegate.JavaDelegate;

                public class TenantProbe implements JavaDelegate {

                    @Override
                    public void execute(DelegateExecution execution) {
                        Producer.sendToQueue("%s", "ran");
                    }
                }
                """.formatted(QUEUE);
        String process = PROCESS_TEMPLATE.formatted(PROCESS_KEY);

        String root = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT;
        repository.createResource(root + "/demo/TenantProbe.java", delegate.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        repository.createResource(root + "/tenant-scope.bpmn", process.getBytes(StandardCharsets.UTF_8), false, "application/xml", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
