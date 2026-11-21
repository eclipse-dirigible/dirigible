/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessLabelKeys;
import org.eclipse.dirigible.repository.api.IRepository;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Where a process' display names are translated, read off its BPMN - against a real engine. The
 * Monitoring shell asks this of every definition its listings name (issue #8788), and its history
 * listing names definitions whose deployment is gone: such a definition declares nothing, rather
 * than failing the whole listing.
 */
class BpmProviderFlowableProcessLabelKeysTest {

    private static final String TENANT_ID = "label-keys-tenant";

    private static final String CATALOG = "sales:sales-model.processes";

    private static final String CATALOGUED = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://www.flowable.org/processdef">
              <process id="SalesInvoiceApproval" name="Sales Invoice Approval" isExecutable="true">
                <extensionElements>
                  <flowable:property name="taskLabelCatalog" value="%s"/>
                </extensionElements>
                <startEvent id="start"></startEvent>
                <endEvent id="end"></endEvent>
                <sequenceFlow id="f1" sourceRef="start" targetRef="end"></sequenceFlow>
              </process>
            </definitions>
            """.formatted(CATALOG);

    private static final String HAND_WRITTEN = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="http://www.flowable.org/processdef">
              <process id="handWritten" name="Hand written" isExecutable="true">
                <startEvent id="start"></startEvent>
                <endEvent id="end"></endEvent>
                <sequenceFlow id="f1" sourceRef="start" targetRef="end"></sequenceFlow>
              </process>
            </definitions>
            """;

    private static ProcessEngine engine;
    private static BpmProviderFlowable bpmProvider;
    private static String cataloguedDefinitionId;
    private static String handWrittenDefinitionId;
    private static String retiredDefinitionId;

    @BeforeAll
    static void startEngine() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:bpm-provider-label-keys-test;DB_CLOSE_DELAY=-1");
        engine = configuration.buildProcessEngine();
        deploy("catalogued", CATALOGUED);
        deploy("hand-written", HAND_WRITTEN);
        cataloguedDefinitionId = start("SalesInvoiceApproval").getProcessDefinitionId();
        handWrittenDefinitionId = start("handWritten").getProcessDefinitionId();
        // A finished instance outlives its deployment in the history: the definition it names is gone.
        Deployment retired = deploy("retired", CATALOGUED.replace("SalesInvoiceApproval", "Retired"));
        retiredDefinitionId = start("Retired").getProcessDefinitionId();
        engine.getRepositoryService()
              .deleteDeployment(retired.getId());
        bpmProvider = new BpmProviderFlowable(mock(IRepository.class), tenantContext(), mock(FlowableArtefactsValidator.class), engine);
    }

    @AfterAll
    static void stopEngine() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    void aGeneratedProcessNamesItsCatalogAndTheKeyOfItsName() {
        assertEquals(Optional.of(new ProcessLabelKeys(CATALOG, CATALOG + ".SalesInvoiceApproval")),
                bpmProvider.getProcessLabelKeys(cataloguedDefinitionId));
    }

    @Test
    void aHandWrittenProcessDeclaresNoKeys() {
        assertEquals(Optional.empty(), bpmProvider.getProcessLabelKeys(handWrittenDefinitionId));
    }

    @Test
    void aDefinitionWhoseDeploymentIsGoneDeclaresNoKeys() {
        assertEquals(Optional.empty(), bpmProvider.getProcessLabelKeys(retiredDefinitionId));
    }

    private static Deployment deploy(String name, String xml) {
        return engine.getRepositoryService()
                     .createDeployment()
                     .addString(name + ".bpmn20.xml", xml)
                     .tenantId(TENANT_ID)
                     .deploy();
    }

    private static ProcessInstance start(String processKey) {
        return engine.getRuntimeService()
                     .startProcessInstanceByKeyAndTenantId(processKey, TENANT_ID);
    }

    private static TenantContext tenantContext() {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(TENANT_ID);
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
        return tenantContext;
    }
}
