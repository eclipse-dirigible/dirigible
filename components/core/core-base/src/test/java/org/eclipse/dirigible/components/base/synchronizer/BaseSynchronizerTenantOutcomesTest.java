/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.synchronizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantArtefactLedger;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantResult;
import org.eclipse.dirigible.components.open.telemetry.OpenTelemetryProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import io.opentelemetry.api.OpenTelemetry;

/**
 * A per-tenant artefact completes once per tenant on one shared object, so the state the shared row
 * ends up with is whatever the LAST tenant registered. Each tenant's outcome therefore goes to the
 * {@link TenantArtefactLedger}, and a failure is never hidden behind a later tenant's success
 * (#7776, #7799).
 */
class BaseSynchronizerTenantOutcomesTest {

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final TenantArtefactLedger ledger = mock(TenantArtefactLedger.class);
    private final ApplicationContext applicationContext = mock(ApplicationContext.class);
    private final RecordingSynchronizer synchronizer = new RecordingSynchronizer();
    private final TestArtefact artefact = new TestArtefact();

    /** What each tenant's completion does, in the order the fan-out visits the tenants. */
    private final Map<String, Behaviour> tenants = new LinkedHashMap<>();
    private String currentTenant;

    private MockedStatic<OpenTelemetryProvider> telemetry;

    @BeforeEach
    void wire() throws Throwable {
        telemetry = mockStatic(OpenTelemetryProvider.class);
        telemetry.when(OpenTelemetryProvider::get)
                 .thenReturn(OpenTelemetry.noop());
        when(applicationContext.getBean(TenantContext.class)).thenReturn(tenantContext);
        when(applicationContext.getBean(TenantArtefactLedger.class)).thenReturn(ledger);
        new BeanProvider().setApplicationContext(applicationContext);
        when(tenantContext.executeForEachTenant(any())).thenAnswer(invocation -> {
            CallableResultAndException<?, ?> callable = invocation.getArgument(0);
            List<TenantResult<?>> results = new ArrayList<>();
            for (String tenantId : tenants.keySet()) {
                currentTenant = tenantId;
                results.add(result(tenantId, callable.call()));
            }
            return results;
        });
        artefact.setLifecycle(ArtefactLifecycle.NEW);
    }

    @AfterEach
    void unwire() {
        new BeanProvider().setApplicationContext(null);
        telemetry.close();
    }

    /** The shape of #7776: the broken tenant first, a healthy one after it. */
    @Test
    void aFailureIsRecordedForItsTenantAndNotHiddenByALaterSuccess() {
        tenants.put("t-a", new Behaviour(ArtefactLifecycle.FAILED, "permission denied for schema t_a", false));
        tenants.put("t-b", new Behaviour(ArtefactLifecycle.CREATED, "", true));

        boolean completed = complete();

        assertFalse(completed);
        verify(ledger).record(artefact, "t-a", ArtefactLifecycle.FAILED, "permission denied for schema t_a");
        verify(ledger).record(artefact, "t-b", ArtefactLifecycle.CREATED, null);
        assertEquals(ArtefactLifecycle.FAILED, synchronizer.sharedLifecycle);
        assertTrue(synchronizer.sharedError.contains("t-a: permission denied for schema t_a"), synchronizer.sharedError);
    }

    @Test
    void everyTenantsSuccessIsRecordedAndTheSharedRowIsLeftToTheSynchronizer() {
        tenants.put("t-a", new Behaviour(ArtefactLifecycle.CREATED, "", true));
        tenants.put("t-b", new Behaviour(ArtefactLifecycle.CREATED, "", true));

        assertTrue(complete());

        verify(ledger).record(artefact, "t-a", ArtefactLifecycle.CREATED, null);
        verify(ledger).record(artefact, "t-b", ArtefactLifecycle.CREATED, null);
        assertNull(synchronizer.sharedLifecycle);
    }

    /**
     * A phase that does not apply to the artefact's lifecycle changes nothing for the tenant, and must
     * not overwrite what the phase that did the work recorded.
     */
    @Test
    void aPhaseThatChangesNothingRecordsNothing() {
        artefact.setLifecycle(ArtefactLifecycle.CREATED);
        tenants.put("t-a", new Behaviour(null, null, true));

        assertTrue(complete());

        verifyNoInteractions(ledger);
    }

    /** Not completing without registering a state is a failure all the same. */
    @Test
    void notCompletingWithoutAStateIsAFailureOfTheTenant() {
        tenants.put("t-a", new Behaviour(null, null, false));

        assertFalse(complete());

        verify(ledger).record(artefact, "t-a", ArtefactLifecycle.FAILED, "Not completed in phase [CREATE]");
    }

    /** Each tenant starts from the state the phase started with, not from the previous tenant's. */
    @Test
    void eachTenantStartsFromTheSameState() {
        tenants.put("t-a", new Behaviour(ArtefactLifecycle.FAILED, "boom", false));
        tenants.put("t-b", new Behaviour(null, null, true));

        complete();

        assertEquals(List.of(ArtefactLifecycle.NEW, ArtefactLifecycle.NEW), synchronizer.lifecyclesSeen);
        verify(ledger, never()).record(any(), eq("t-b"), any(), any());
    }

    /** A context without the synchronization processor has no ledger - and nothing to record into. */
    @Test
    void withoutALedgerTheArtefactStillCompletes() {
        when(applicationContext.getBean(TenantArtefactLedger.class)).thenThrow(
                new NoSuchBeanDefinitionException(TenantArtefactLedger.class));
        tenants.put("t-a", new Behaviour(ArtefactLifecycle.CREATED, "", true));

        assertTrue(complete());
    }

    private boolean complete() {
        return synchronizer.complete(new TopologyWrapper<>(artefact, new HashMap<>(), synchronizer), ArtefactPhase.CREATE);
    }

    private static TenantResult<?> result(String tenantId, Object value) {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(tenantId);
        return new TenantResult<>() {

            @Override
            public Tenant getTenant() {
                return tenant;
            }

            @Override
            public Object getResult() {
                return value;
            }
        };
    }

    /**
     * What one tenant's completion does.
     *
     * @param lifecycle the state it registers, or null for none
     * @param error the error it registers with it
     * @param completed what it returns
     */
    private record Behaviour(ArtefactLifecycle lifecycle, String error, boolean completed) {
    }

    private final class RecordingSynchronizer extends MultitenantBaseSynchronizer<TestArtefact, Long> {

        private final List<ArtefactLifecycle> lifecyclesSeen = new ArrayList<>();
        private ArtefactLifecycle sharedLifecycle;
        private String sharedError;

        @Override
        protected boolean completeImpl(TopologyWrapper<TestArtefact> wrapper, ArtefactPhase flow) {
            TestArtefact completing = wrapper.getArtefact();
            lifecyclesSeen.add(completing.getLifecycle());
            Behaviour behaviour = tenants.get(currentTenant);
            if (behaviour.lifecycle() != null) {
                completing.setLifecycle(behaviour.lifecycle());
                completing.setError(behaviour.error());
            }
            return behaviour.completed();
        }

        @Override
        public void setStatus(TestArtefact artefact, ArtefactLifecycle lifecycle, String message) {
            artefact.setLifecycle(lifecycle);
            artefact.setError(message);
            sharedLifecycle = lifecycle;
            sharedError = message;
        }

        @Override
        protected List<TestArtefact> parseImpl(String location, byte[] content) {
            return List.of();
        }

        @Override
        public ArtefactService<TestArtefact, Long> getService() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAccepted(String type) {
            return true;
        }

        @Override
        public List<TestArtefact> retrieve(String location) {
            return List.of();
        }

        @Override
        protected void cleanupImpl(TestArtefact artefact) {
            // not exercised
        }

        @Override
        public void setCallback(SynchronizerCallback callback) {
            // not exercised
        }

        @Override
        public String getFileExtension() {
            return ".test";
        }

        @Override
        public String getArtefactType() {
            return "test";
        }
    }

    private static final class TestArtefact extends Artefact {

        TestArtefact() {
            super("/project/orders.test", "orders", "test", "", null);
        }
    }
}
