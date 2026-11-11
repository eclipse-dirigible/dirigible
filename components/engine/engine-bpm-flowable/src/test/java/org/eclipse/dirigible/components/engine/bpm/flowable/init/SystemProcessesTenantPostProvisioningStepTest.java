/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.init;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.bpm.SystemBpmProcess;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A tenant provisioned while the instance runs gets the system processes at once - they used to be
 * deployed only when the instance started, for the tenants that existed then.
 */
class SystemProcessesTenantPostProvisioningStepTest {

    private BpmService bpmService;
    private TenantContext tenantContext;
    private SystemProcessesTenantPostProvisioningStep step;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Throwable {
        bpmService = mock(BpmService.class);
        tenantContext = mock(TenantContext.class);
        when(tenantContext.execute(anyString(), any(CallableResultAndException.class))).thenAnswer(
                invocation -> invocation.<CallableResultAndException<Object, Throwable>>getArgument(1)
                                        .call());
        SystemBpmProcess process = mock(SystemBpmProcess.class);
        when(process.getDeploymentKey()).thenReturn("export-schema");
        // a resource the module really carries
        when(process.getResourcePath()).thenReturn("/system-processes/test-system-process.bpmn");
        step = new SystemProcessesTenantPostProvisioningStep(new SystemProcessesDeployer(Set.of(process), bpmService), tenantContext);
    }

    @Test
    @SuppressWarnings("unchecked")
    void everyNewTenantGetsTheSystemProcessesInItsOwnContext() throws Throwable {
        step.execute(Set.of("acme", "globex"));

        verify(tenantContext).execute(eq("acme"), any(CallableResultAndException.class));
        verify(tenantContext).execute(eq("globex"), any(CallableResultAndException.class));
        verify(bpmService, times(2)).deployProcess(eq("export-schema"), eq("/system-processes/test-system-process.bpmn"), anyString());
    }

    @Test
    void withoutTheTenantsThereIsNothingToDeploy() {
        step.execute();

        verifyNoInteractions(bpmService, tenantContext);
    }
}
