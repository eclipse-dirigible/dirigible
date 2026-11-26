/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.engine.bpm.flowable.config.BpmProviderFlowable;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.HistoricProcessInstanceData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessInstanceData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessLabelKeys;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The Monitoring shell's process listings carry the i18n key of each process' name, as the Inbox's
 * tasks do (issue #8788): a generated process names the key its catalog declares, a hand-written
 * one none - and the BPMN is read once per process definition in a response, never once per row.
 */
class BpmFlowableEndpointProcessNameKeyTest {

    private static final String CATALOG = "sales:sales-model.processes";

    private static final String GENERATED = "SalesInvoiceApproval:1:7";

    private static final String HAND_WRITTEN = "handWritten:1:9";

    private final BpmService bpmService = mock(BpmService.class);

    private final BpmFlowableEndpoint endpoint = new BpmFlowableEndpoint(mock(BpmProviderFlowable.class), bpmService);

    @BeforeEach
    void catalogs() {
        when(bpmService.getProcessLabelKeys(GENERATED)).thenReturn(
                Optional.of(new ProcessLabelKeys(CATALOG, CATALOG + ".SalesInvoiceApproval")));
        when(bpmService.getProcessLabelKeys(HAND_WRITTEN)).thenReturn(Optional.empty());
    }

    @Test
    void theRunningInstancesNameTheKeyOfTheirProcessName() {
        when(bpmService.getProcessInstances(any(), any(), any())).thenReturn(
                List.of(running(GENERATED), running(HAND_WRITTEN), running(GENERATED), running(HAND_WRITTEN)));

        List<ProcessInstanceData> instances = endpoint.getProcessesInstances(Optional.empty(), Optional.empty(), null)
                                                      .getBody();

        assertEquals(CATALOG + ".SalesInvoiceApproval", instances.get(0)
                                                                 .getProcessDefinitionNameKey());
        assertNull(instances.get(1)
                            .getProcessDefinitionNameKey(),
                "a hand-written process declares no catalog");
        assertEquals(CATALOG + ".SalesInvoiceApproval", instances.get(2)
                                                                 .getProcessDefinitionNameKey());
        assertNull(instances.get(3)
                            .getProcessDefinitionNameKey());
        assertEachDefinitionReadOnce();
    }

    @Test
    void theFinishedInstancesNameTheKeyOfTheirProcessName() {
        HistoricProcessInstance generated = finished(GENERATED);
        List<HistoricProcessInstance> listing = List.of(generated, finished(HAND_WRITTEN), finished(GENERATED), finished(HAND_WRITTEN));
        when(bpmService.getCompletedProcessInstances(any(), any(), any())).thenReturn(listing);

        List<HistoricProcessInstanceData> instances = endpoint.getHistoricProcessesInstances(Optional.empty(), Optional.empty(), null)
                                                              .getBody();

        assertSame(generated, instances.get(0)
                                       .getInstance(),
                "the engine's record travels unchanged");
        assertEquals(CATALOG + ".SalesInvoiceApproval", instances.get(0)
                                                                 .getProcessDefinitionNameKey());
        assertNull(instances.get(1)
                            .getProcessDefinitionNameKey());
        assertEquals(CATALOG + ".SalesInvoiceApproval", instances.get(2)
                                                                 .getProcessDefinitionNameKey());
        assertNull(instances.get(3)
                            .getProcessDefinitionNameKey());
        assertEachDefinitionReadOnce();
    }

    @Test
    void theInstanceDetailNamesTheKeyOfItsProcessName() {
        when(bpmService.getProcessInstanceById("instance-1")).thenReturn(running(GENERATED));

        assertEquals(CATALOG + ".SalesInvoiceApproval", endpoint.getProcessInstance("instance-1")
                                                                .getBody()
                                                                .getProcessDefinitionNameKey());
    }

    private void assertEachDefinitionReadOnce() {
        verify(bpmService, times(1)).getProcessLabelKeys(GENERATED);
        verify(bpmService, times(1)).getProcessLabelKeys(HAND_WRITTEN);
    }

    private static ProcessInstanceData running(String processDefinitionId) {
        ProcessInstanceData instance = new ProcessInstanceData();
        instance.setProcessDefinitionId(processDefinitionId);
        return instance;
    }

    private static HistoricProcessInstance finished(String processDefinitionId) {
        HistoricProcessInstance instance = mock(HistoricProcessInstance.class);
        when(instance.getProcessDefinitionId()).thenReturn(processDefinitionId);
        return instance;
    }
}
