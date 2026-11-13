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
 * What an async step of a non-default tenant's process hands to {@code Process.executeAfterCommit}
 * runs in that tenant (#8550).
 *
 * <p>
 * The generated status transitions publish their {@code -transitioned} event this way. A
 * {@code ${JavaTask}} step is scoped to its tenant while it executes, but on the async executor its
 * job's transaction commits after that scope has closed, so the callback used to run with no tenant
 * and publish into the default tenant, where none of the tenant's listeners could see it. Here the
 * callback sends to a queue - a tenant-scoped resource - and the test draws the message back inside
 * the tenant that started the process, then checks that the default tenant's queue stayed empty.
 */
class BpmAfterCommitTenantIT extends IntegrationTest {

    private static final String PROJECT = "bpm-after-commit-tenant-it";

    private static final String PROCESS_KEY = "after-commit-tenant-scope";

    private static final String QUEUE = PROJECT + "-out";

    private static final String PROCESS_TEMPLATE =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="%s" name="After-commit tenant scope" isExecutable="true">
                        <startEvent id="start"/>
                        <sequenceFlow id="f1" sourceRef="start" targetRef="probe"/>
                        <serviceTask id="probe" name="Probe" flowable:async="true" flowable:delegateExpression="${JavaTask}">
                          <extensionElements>
                            <flowable:field name="handler">
                              <flowable:string><![CDATA[demo.AfterCommitProbe]]></flowable:string>
                            </flowable:field>
                          </extensionElements>
                        </serviceTask>
                        <sequenceFlow id="f2" sourceRef="probe" targetRef="end"/>
                        <endEvent id="end"/>
                      </process>
                    </definitions>
                    """;

    private static final String HANDLER_TEMPLATE = """
            package demo;

            import org.eclipse.dirigible.sdk.bpm.Process;
            import org.eclipse.dirigible.sdk.messaging.Producer;
            import org.flowable.engine.delegate.DelegateExecution;
            import org.flowable.engine.delegate.JavaDelegate;

            public class AfterCommitProbe implements JavaDelegate {

                @Override
                public void execute(DelegateExecution execution) {
                    Process.executeAfterCommit(() -> Producer.sendToQueue("%s", "committed"));
                }
            }
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
    void anAfterCommitCallbackOfAnAsyncStepRunsInItsProcessInstancesTenant() {
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
        assertEquals("committed", received[0], "the after-commit callback must publish into the tenant its process instance belongs to");

        String defaultTenantId = DirigibleTestTenant.createDefaultTenant()
                                                    .getId();
        assertThrows(RuntimeException.class,
                () -> tenantContext.execute(defaultTenantId, () -> MessagingFacade.receiveFromQueue(QUEUE, RECEIVE_TIMEOUT_MILLIS)),
                "nothing of the tenant's transition may reach the default tenant");
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
        String root = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT;
        repository.createResource(root + "/demo/AfterCommitProbe.java", HANDLER_TEMPLATE.formatted(QUEUE)
                                                                                        .getBytes(StandardCharsets.UTF_8),
                false, "text/x-java", true);
        repository.createResource(root + "/after-commit.bpmn", PROCESS_TEMPLATE.formatted(PROCESS_KEY)
                                                                               .getBytes(StandardCharsets.UTF_8),
                false, "application/xml", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
