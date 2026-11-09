/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.bpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantConfigurationScope;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.bpm.flowable.config.BpmProviderFlowable;
import org.flowable.common.engine.impl.cfg.TransactionContext;
import org.flowable.common.engine.impl.cfg.TransactionListener;
import org.flowable.common.engine.impl.cfg.TransactionState;
import org.flowable.common.engine.impl.context.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * {@code Process.executeAfterCommit} runs its action in the tenant scope it was registered in, with
 * that tenant's configuration overrides (#8550).
 *
 * <p>
 * The sequence under test is the async executor's: a step registers the action inside its tenant
 * scope, the scope closes when the step returns, and only then does the job's transaction commit
 * and fire the listener - on a thread with no tenant. The Flowable transaction is a double whose
 * registered listener the test fires itself, after leaving the scope.
 */
class BpmFacadeExecuteAfterCommitTest {

    private static final String TENANT_ID = "acme";

    private final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    private final ThreadLocal<Boolean> overridesApplied = ThreadLocal.withInitial(() -> false);

    private final List<String> observedScopes = new ArrayList<>();

    private TenantContext tenantContext;

    private TransactionContext transactionContext;

    @BeforeEach
    void setUp() {
        tenantContext = recordingTenantContext();
        new BpmFacade(mock(BpmProviderFlowable.class), tenantContext, overridesProvider()).afterPropertiesSet();
        transactionContext = mock(TransactionContext.class);
        Context.setTransactionContext(transactionContext);
    }

    @AfterEach
    void tearDown() {
        if (Context.getTransactionContext() != null) {
            Context.removeTransactionContext();
        }
    }

    @Test
    void anActionRegisteredInATenantRunsInThatTenantAfterItsScopeHasClosed() {
        tenantContext.execute(TENANT_ID, () -> {
            BpmFacade.executeAfterCommit(this::recordScope);
            return null;
        });

        commit();

        assertEquals(List.of(TENANT_ID + "+overrides"), observedScopes,
                "the action must run in the tenant it was registered in, with its overrides, although the commit came after that scope closed");
    }

    @Test
    void anActionRegisteredWithoutATenantRunsAsItIs() {
        BpmFacade.executeAfterCommit(this::recordScope);

        commit();

        assertEquals(List.of("no tenant"), observedScopes);
        verify(tenantContext, never()).execute(anyString(), any());
    }

    @Test
    void outsideAFlowableCommandTheActionRunsImmediately() {
        Context.removeTransactionContext();

        tenantContext.execute(TENANT_ID, () -> {
            BpmFacade.executeAfterCommit(this::recordScope);
            return null;
        });

        assertEquals(List.of(TENANT_ID), observedScopes, "the action runs at once, inside the caller's own scope");
    }

    private void recordScope() {
        String tenant = currentTenant.get();
        observedScopes.add(tenant == null ? "no tenant" : tenant + (overridesApplied.get() ? "+overrides" : ""));
    }

    /**
     * Fire what the step registered for COMMITTED, as the job's commit does - outside any tenant scope.
     */
    private void commit() {
        ArgumentCaptor<TransactionListener> listener = ArgumentCaptor.forClass(TransactionListener.class);
        verify(transactionContext).addTransactionListener(eq(TransactionState.COMMITTED), listener.capture());
        assertEquals(null, currentTenant.get(), "the commit must fire outside the registering scope for the test to mean anything");
        listener.getValue()
                .execute(null);
    }

    @SuppressWarnings("unchecked")
    private TenantContext recordingTenantContext() {
        TenantContext context = mock(TenantContext.class);
        when(context.isNotInitialized()).thenAnswer(invocation -> currentTenant.get() == null);
        when(context.getCurrentTenant()).thenAnswer(invocation -> {
            Tenant tenant = mock(Tenant.class);
            when(tenant.getId()).thenReturn(currentTenant.get());
            return tenant;
        });
        when(context.execute(anyString(), any(CallableResultAndException.class))).thenAnswer(invocation -> {
            String previous = currentTenant.get();
            currentTenant.set(invocation.getArgument(0));
            try {
                return invocation.<CallableResultAndException<Object, Throwable>>getArgument(1)
                                 .call();
            } finally {
                currentTenant.set(previous);
            }
        });
        return context;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<TenantConfigurationScope> overridesProvider() {
        TenantConfigurationScope scope = mock(TenantConfigurationScope.class);
        when(scope.executeWithTenantOverrides(any(CallableResultAndException.class))).thenAnswer(invocation -> {
            overridesApplied.set(true);
            try {
                return invocation.<CallableResultAndException<Object, Throwable>>getArgument(0)
                                 .call();
            } finally {
                overridesApplied.set(false);
            }
        });
        ObjectProvider<TenantConfigurationScope> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(scope);
        return provider;
    }
}
