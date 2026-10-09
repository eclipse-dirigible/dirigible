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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
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

import org.eclipse.dirigible.components.base.tenant.TenantPostProvisioningStep;
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
 * before the caller is answered - and the initialization it starts is the activated tenant's alone.
 */
class TenantActivationServiceTest {

    private final TenantService tenantService = mock(TenantService.class);
    private final TenantDataSourceRegistrationService dataSourceRegistrationService = mock(TenantDataSourceRegistrationService.class);
    private final TenantInitializationService initializationService = mock(TenantInitializationService.class);
    private final TenantPostProvisioningStep postProvisioningStep = mock(TenantPostProvisioningStep.class);
    private final DeferredExecutor executor = new DeferredExecutor();

    private final TenantActivationService service = new TenantActivationService(tenantService, dataSourceRegistrationService,
            initializationService, Set.of(postProvisioningStep), executor);

    @BeforeEach
    void wireDefaults() {
        when(dataSourceRegistrationService.isRegistered(any())).thenReturn(true);
        when(dataSourceRegistrationService.tenantDataSourceName(any())).thenReturn("acme_DefaultDB");
    }

    @Test
    void activationMakesTheTenantProvisioned() {
        Tenant tenant = tenant("acme", TenantStatus.PENDING_ACTIVATION);

        service.activate(tenant);

        assertEquals(TenantStatus.PROVISIONED, tenant.getStatus());
        verify(tenantService).save(tenant);
    }

    /**
     * The per-tenant fan-out only visits provisioned tenants, so an initialization requested before the
     * flip would skip the very tenant it is for.
     */
    @Test
    void theTenantIsProvisionedBeforeItsInitializationIsRequested() {
        service.activate(tenant("acme", TenantStatus.PENDING_ACTIVATION));

        InOrder order = inOrder(tenantService, initializationService);
        order.verify(tenantService)
             .save(any());
        order.verify(initializationService)
             .request("acme");
    }

    /**
     * The point of requesting the initialization in the request thread: a caller that polls the instant
     * it gets its answer has to see work outstanding, not a status derived from state nothing has
     * touched yet.
     */
    @Test
    void theInitializationIsRequestedBeforeTheCallerIsAnswered() {
        service.activate(tenant("acme", TenantStatus.PENDING_ACTIVATION));

        verify(initializationService).request("acme");
        verify(initializationService, never()).start(anyString());
        verify(postProvisioningStep, never()).execute(anySet());
    }

    /** The steps initialize the activated tenant and no other (#7799). */
    @Test
    void theInitializationRunsAfterwardsForTheActivatedTenantOnly() {
        service.activate(tenant("acme", TenantStatus.PENDING_ACTIVATION));

        executor.runQueued();

        InOrder order = inOrder(initializationService, postProvisioningStep);
        order.verify(initializationService)
             .start("acme");
        order.verify(postProvisioningStep)
             .execute(Set.of("acme"));
        order.verify(initializationService)
             .finish(eq("acme"), isNull());
        verify(postProvisioningStep, never()).execute();
    }

    @Test
    void aTenantWithoutADataSourceCannotBeActivated() {
        when(dataSourceRegistrationService.isRegistered(any())).thenReturn(false);
        Tenant tenant = tenant("acme", TenantStatus.PENDING_ACTIVATION);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.activate(tenant));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason()
                     .contains("acme_DefaultDB"),
                ex.getReason());
        assertEquals(TenantStatus.PENDING_ACTIVATION, tenant.getStatus());
        verify(tenantService, never()).save(any());
        verify(initializationService, never()).request(anyString());
    }

    /** Re-activating an active tenant is the documented way to repair a failed initialization. */
    @Test
    void reActivatingAnActiveTenantReInitializesIt() {
        Tenant tenant = tenant("acme", TenantStatus.PROVISIONED);

        service.activate(tenant);

        verify(tenantService, never()).save(any());
        verify(initializationService).request("acme");
        executor.runQueued();
        verify(postProvisioningStep).execute(Set.of("acme"));
    }

    /** Two activations at about the same time each initialize their own tenant. */
    @Test
    void tenantsActivatedTogetherAreEachInitializedOnTheirOwn() {
        service.activate(tenant("acme", TenantStatus.PENDING_ACTIVATION));
        service.activate(tenant("globex", TenantStatus.PENDING_ACTIVATION));

        executor.runQueued();

        verify(postProvisioningStep).execute(Set.of("acme"));
        verify(postProvisioningStep).execute(Set.of("globex"));
        verify(initializationService).finish(eq("acme"), isNull());
        verify(initializationService).finish(eq("globex"), isNull());
    }

    /**
     * A queued initialization has not started yet, so it covers a repeated activation of its tenant.
     */
    @Test
    void aRepeatedActivationOfAQueuedTenantSharesItsInitialization() {
        service.activate(tenant("acme", TenantStatus.PENDING_ACTIVATION));
        service.activate(tenant("acme", TenantStatus.PROVISIONED));

        assertEquals(1, executor.queued(), "the second activation must not queue a second initialization");
        executor.runQueued();
        verify(postProvisioningStep).execute(Set.of("acme"));
    }

    /** An activation that arrives after the queued initialization started gets one of its own. */
    @Test
    void anActivationAfterTheInitializationStartedQueuesAnother() {
        service.activate(tenant("acme", TenantStatus.PENDING_ACTIVATION));
        executor.runQueued();

        service.activate(tenant("acme", TenantStatus.PROVISIONED));

        assertEquals(1, executor.queued());
        executor.runQueued();
        verify(postProvisioningStep, times(2)).execute(Set.of("acme"));
    }

    /**
     * One failing step must not stop the others, and must not break the activation call - its failure
     * is the failure of the tenant's initialization.
     */
    @Test
    void aFailingStepDoesNotStopTheOthersAndFailsTheInitialization() {
        TenantPostProvisioningStep failing = mock(TenantPostProvisioningStep.class);
        doThrow(new IllegalStateException("boom")).when(failing)
                                                  .execute(anySet());
        TenantActivationService withFailingStep = new TenantActivationService(tenantService, dataSourceRegistrationService,
                initializationService, Set.of(failing, postProvisioningStep), executor);

        withFailingStep.activate(tenant("acme", TenantStatus.PENDING_ACTIVATION));
        executor.runQueued();

        verify(postProvisioningStep).execute(Set.of("acme"));
        verify(initializationService).finish(eq("acme"), argThat(error -> error != null && error.contains("boom")));
    }

    /**
     * An initialization a restart interrupted is picked up again, or its tenant reads in progress for
     * good.
     */
    @Test
    void anInterruptedInitializationIsResumedWhenTheApplicationIsReady() {
        when(initializationService.findOpen()).thenReturn(List.of(new TenantInitialization("acme")));

        service.onApplicationEvent(null);
        executor.runQueued();

        verify(postProvisioningStep).execute(Set.of("acme"));
        verify(initializationService).finish(eq("acme"), isNull());
    }

    private static Tenant tenant(String id, TenantStatus status) {
        Tenant tenant = new Tenant("-", id, "", id, status);
        tenant.setId(id);
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
