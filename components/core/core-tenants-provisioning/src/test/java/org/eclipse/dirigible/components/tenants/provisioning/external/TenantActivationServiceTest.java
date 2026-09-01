/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.components.base.synchronizer.MultitenantSynchronizers;
import org.eclipse.dirigible.components.base.tenant.TenantPostProvisioningStep;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Activation is two things that must happen in one order and one thing that must not happen at all
 * before the caller is answered.
 */
class TenantActivationServiceTest {

    private static final Set<String> MULTITENANT_TYPES = Set.of("table", "csvim");

    private final TenantService tenantService = mock(TenantService.class);
    private final TenantDataSourceRegistrationService dataSourceRegistrationService = mock(TenantDataSourceRegistrationService.class);
    private final DefinitionService definitionService = mock(DefinitionService.class);
    private final MultitenantSynchronizers multitenantSynchronizers = mock(MultitenantSynchronizers.class);
    private final TenantPostProvisioningStep postProvisioningStep = mock(TenantPostProvisioningStep.class);
    private final DeferredExecutor executor = new DeferredExecutor();

    private final TenantActivationService service = new TenantActivationService(tenantService, dataSourceRegistrationService,
            definitionService, multitenantSynchronizers, Set.of(postProvisioningStep), executor);

    @BeforeEach
    void wireDefaults() {
        when(multitenantSynchronizers.getArtefactTypes()).thenReturn(MULTITENANT_TYPES);
        when(dataSourceRegistrationService.isRegistered(any())).thenReturn(true);
        when(dataSourceRegistrationService.tenantDataSourceName(any())).thenReturn("acme_DefaultDB");
    }

    @Test
    void activationMakesTheTenantProvisioned() {
        Tenant tenant = tenant(TenantStatus.PENDING_ACTIVATION);

        service.activate(tenant);

        assertEquals(TenantStatus.PROVISIONED, tenant.getStatus());
        verify(tenantService).save(tenant);
    }

    /**
     * The per-tenant fan-out only visits provisioned tenants, so a materialization started before the
     * flip would skip the very tenant it is for.
     */
    @Test
    void theTenantIsProvisionedBeforeAnythingIsMarkedForReprocessing() {
        service.activate(tenant(TenantStatus.PENDING_ACTIVATION));

        InOrder order = inOrder(tenantService, definitionService);
        order.verify(tenantService)
             .save(any());
        order.verify(definitionService)
             .updateChecksums(eq(""), eq(MULTITENANT_TYPES));
    }

    /**
     * The point of doing the marking in the request thread: a caller that polls the instant it gets its
     * answer has to see work outstanding, not a status derived from state nothing has touched yet.
     */
    @Test
    void theWorkIsMarkedBeforeTheCallerIsAnswered() {
        service.activate(tenant(TenantStatus.PENDING_ACTIVATION));

        verify(definitionService).updateChecksums("", MULTITENANT_TYPES);
        verify(postProvisioningStep, never()).execute();
    }

    @Test
    void theInitializationRunsAfterwards() {
        service.activate(tenant(TenantStatus.PENDING_ACTIVATION));

        executor.runQueued();

        verify(postProvisioningStep).execute();
    }

    @Test
    void aTenantWithoutADataSourceCannotBeActivated() {
        when(dataSourceRegistrationService.isRegistered(any())).thenReturn(false);
        Tenant tenant = tenant(TenantStatus.PENDING_ACTIVATION);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.activate(tenant));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason()
                     .contains("acme_DefaultDB"),
                ex.getReason());
        assertEquals(TenantStatus.PENDING_ACTIVATION, tenant.getStatus());
        verify(tenantService, never()).save(any());
        verify(definitionService, never()).updateChecksums(any(), any());
    }

    /** Re-activating an active tenant is the documented way to repair a failed initialization. */
    @Test
    void reActivatingAnActiveTenantReInitializesIt() {
        Tenant tenant = tenant(TenantStatus.PROVISIONED);

        service.activate(tenant);

        verify(tenantService, never()).save(any());
        verify(definitionService).updateChecksums("", MULTITENANT_TYPES);
        executor.runQueued();
        verify(postProvisioningStep).execute();
    }

    /**
     * A queued pass has not started yet, so it will cover the second tenant as well - running two
     * global synchronizations back to back would only repeat the same work.
     */
    @Test
    void activationsThatArriveTogetherShareOneInitialization() {
        service.activate(tenant(TenantStatus.PENDING_ACTIVATION));
        service.activate(tenant(TenantStatus.PENDING_ACTIVATION));

        assertEquals(1, executor.queued(), "the second activation must not queue a second pass");
        executor.runQueued();
        verify(postProvisioningStep).execute();
        // both activations still marked the work, which is what makes them individually observable
        verify(definitionService, times(2)).updateChecksums("", MULTITENANT_TYPES);
    }

    /** An activation that arrives after the queued pass started gets a pass of its own. */
    @Test
    void anActivationAfterThePassStartedQueuesAnother() {
        service.activate(tenant(TenantStatus.PENDING_ACTIVATION));
        executor.runQueued();

        service.activate(tenant(TenantStatus.PENDING_ACTIVATION));

        assertEquals(1, executor.queued());
        executor.runQueued();
        verify(postProvisioningStep, times(2)).execute();
    }

    /** One failing step must not stop the others, and must not break the activation call. */
    @Test
    void aFailingStepDoesNotStopTheOthers() {
        TenantPostProvisioningStep failing = mock(TenantPostProvisioningStep.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
                           .when(failing)
                           .execute();
        TenantActivationService withFailingStep = new TenantActivationService(tenantService, dataSourceRegistrationService,
                definitionService, multitenantSynchronizers, Set.of(failing, postProvisioningStep), executor);

        withFailingStep.activate(tenant(TenantStatus.PENDING_ACTIVATION));
        executor.runQueued();

        verify(postProvisioningStep).execute();
    }

    private static Tenant tenant(TenantStatus status) {
        Tenant tenant = new Tenant("-", "Acme Ltd", "", "acme", status);
        tenant.setId("acme");
        return tenant;
    }

    /**
     * An executor that holds what was submitted until the test says to run it - which is what lets the
     * tests observe the state the caller of an activation sees, before the initialization starts.
     */
    private static final class DeferredExecutor extends AbstractExecutorService {

        private final List<Runnable> pending = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            pending.add(command);
        }

        int queued() {
            return pending.size();
        }

        void runQueued() {
            List<Runnable> toRun = new ArrayList<>(pending);
            pending.clear();
            toRun.forEach(Runnable::run);
        }

        @Override
        public void shutdown() {}

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }
}
