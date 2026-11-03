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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.repository.api.IRepository;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A process delete that loses the optimistic lock to a task completion committing at the same
 * moment is retried against the instance's new state, until the instance is gone (issue #8108). One
 * attempt used to be all it got: the completion committed, the delete was logged and dropped, and
 * the next task was handed out for a record that no longer existed.
 */
class BpmProviderFlowableDeleteProcessRetryTest {

    private static final String INSTANCE = "instance-149";

    private final RuntimeService runtimeService = mock(RuntimeService.class);

    private final ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);

    private BpmProviderFlowable provider;

    @BeforeEach
    void provider() {
        ProcessEngine engine = mock(ProcessEngine.class);
        when(engine.getRuntimeService()).thenReturn(runtimeService);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId(anyString())).thenReturn(query);
        when(query.processInstanceTenantId(anyString())).thenReturn(query);

        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn("tenant");
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);

        provider = new BpmProviderFlowable(mock(IRepository.class), tenantContext, mock(FlowableArtefactsValidator.class), engine);
    }

    @Test
    void aDeleteThatLostTheLockToACompletionIsRetriedUntilTheInstanceIsGone() {
        when(query.singleResult()).thenReturn(mock(ProcessInstance.class));
        doThrow(lostLock()).doThrow(lostLock())
                           .doNothing()
                           .when(runtimeService)
                           .deleteProcessInstance(INSTANCE, "deleted");

        provider.deleteProcess(INSTANCE, "deleted");

        verify(runtimeService, times(3)).deleteProcessInstance(INSTANCE, "deleted");
    }

    @Test
    void anInstanceThatEndedWhileItWasBeingDeletedIsNotRetried() {
        ProcessInstance running = mock(ProcessInstance.class);
        // Read once for the log line before the delete, then gone when the lost lock is examined.
        when(query.singleResult()).thenReturn(running)
                                  .thenReturn(null);
        doThrow(lostLock()).when(runtimeService)
                           .deleteProcessInstance(INSTANCE, "deleted");

        provider.deleteProcess(INSTANCE, "deleted");

        verify(runtimeService, times(1)).deleteProcessInstance(INSTANCE, "deleted");
    }

    @Test
    void theRetriesAreBounded() {
        when(query.singleResult()).thenReturn(mock(ProcessInstance.class));
        doThrow(lostLock()).when(runtimeService)
                           .deleteProcessInstance(INSTANCE, "deleted");

        provider.deleteProcess(INSTANCE, "deleted");

        verify(runtimeService, times(BpmProviderFlowable.DELETE_PROCESS_ATTEMPTS)).deleteProcessInstance(INSTANCE, "deleted");
    }

    @Test
    void anyOtherFailureIsNotRetried() {
        when(query.singleResult()).thenReturn(mock(ProcessInstance.class));
        doThrow(new IllegalStateException("database unavailable")).when(runtimeService)
                                                                  .deleteProcessInstance(INSTANCE, "deleted");

        provider.deleteProcess(INSTANCE, "deleted");

        verify(runtimeService, times(1)).deleteProcessInstance(INSTANCE, "deleted");
    }

    private static FlowableOptimisticLockingException lostLock() {
        return new FlowableOptimisticLockingException("Execution[ id '149' ] was updated by another transaction concurrently");
    }
}
