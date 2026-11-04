/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.service;

import java.util.List;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.listeners.domain.ListenerKind;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * The Class BackgroundListenersManagerTest.
 */
@ExtendWith(MockitoExtension.class)
class ListenersManagerTest {

    /** The Constant HANDLER. */
    private static final String HANDLER = "test-handler";

    /** The Constant LISTNER_LOCATION. */
    private static final String LISTNER_LOCATION = "my-listener";

    /** The listeners manager. */
    @InjectMocks
    private ListenersManager listenersManager;

    /** The repository. */
    @Mock
    private IRepository repository;

    /** The message listener manager factory. */
    @Mock
    private ListenerManagerFactory messageListenerManagerFactory;

    /** The listenerEntity. */
    @Mock
    private org.eclipse.dirigible.components.listeners.domain.Listener listenerEntity;

    /** The resource. */
    @Mock
    private IResource resource;

    /** The listener creator. */
    @Mock
    private ListenerCreator listenerCreator;

    /** The connection the artefacts share. */
    @Mock
    private SharedListenerConnection sharedConnection;

    /** The listener manager. */
    @Mock
    private ListenerManager listenerManager;

    /** The listener manager 2. */
    @Mock
    private ListenerManager listenerManager2;

    /** The listener descriptor. */
    @Mock
    private ListenerDescriptor listenerDescriptor;

    /** The listener descriptor 2. */
    @Mock
    private ListenerDescriptor listenerDescriptor2;

    /** The listener artefacts. */
    @Mock
    private ListenerService listenerService;

    /** The tenant scope a tenant's listeners are started in. */
    @Mock
    private TenantContext tenantContext;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        ListenersManager.LISTENERS.clear();
        lenient().when(listenerCreator.fromEntity(listenerEntity))
                 .thenReturn(listenerDescriptor);
    }

    /**
     * Test start listener on try to start the listner again.
     */
    @Test
    void testStartListenerOnTryToStartTheListenerAgain() {
        testStartListenerOnExistingListener();

        listenersManager.startListener(listenerEntity);

        verifyNoMoreInteractions(listenerManager);
    }

    /**
     * Test start listener on existing listener.
     */
    @Test
    void testStartListenerOnExistingListener() {
        when(listenerDescriptor.getHandlerPath()).thenReturn(HANDLER);
        when(repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + IRepositoryStructure.SEPARATOR + HANDLER)).thenReturn(
                resource);
        when(resource.exists()).thenReturn(true);
        when(messageListenerManagerFactory.create(listenerDescriptor)).thenReturn(listenerManager);

        listenersManager.startListener(listenerEntity);

        verify(listenerManager).startListener();
    }

    /**
     * Test start listener on missing listener.
     */
    @Test
    void testStartListenerOnMissingListener() {
        when(listenerDescriptor.getHandlerPath()).thenReturn(HANDLER);
        when(repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + IRepositoryStructure.SEPARATOR + HANDLER)).thenReturn(
                resource);
        when(resource.exists()).thenReturn(false);

        // A missing handler is a failure the synchronizer must see, not a normal return that reads
        // as CREATED and running with nothing subscribed (#7248).
        assertThrows(IllegalStateException.class, () -> listenersManager.startListener(listenerEntity));

        verifyNoInteractions(messageListenerManagerFactory);
    }

    /**
     * Test stop listener.
     */
    @Test
    void testStopListener() {
        ListenersManager.LISTENERS.put(listenerDescriptor, listenerManager);

        listenersManager.stopListener(listenerEntity);
        listenersManager.stopListener(listenerEntity);

        // stop is called only once
        verify(listenerManager).stopListener();
    }

    /**
     * Test stop listeners.
     */
    @Test
    void testStopListeners() {
        ListenersManager.LISTENERS.put(listenerDescriptor, listenerManager);
        ListenersManager.LISTENERS.put(listenerDescriptor2, listenerManager2);

        listenersManager.stopListeners();
        listenersManager.stopListeners();

        // stop is called only once
        verify(listenerManager).stopListener();
        verify(listenerManager2).stopListener();
        // and the connection they shared goes with them (#7875)
        verify(sharedConnection, times(2)).close();
    }

    /** Deprovisioning stops the tenant's listeners only, and forgets them. */
    @Test
    void stopTenantListenersStopsOnlyTheTenantsOwn() {
        ListenerDescriptor acme = new ListenerDescriptor(ListenerType.QUEUE, "acme###orders", HANDLER);
        ListenerDescriptor globex = new ListenerDescriptor(ListenerType.QUEUE, "globex###orders", HANDLER);
        ListenersManager.LISTENERS.put(acme, listenerManager);
        ListenersManager.LISTENERS.put(globex, listenerManager2);

        org.junit.jupiter.api.Assertions.assertEquals(1, listenersManager.stopTenantListeners("acme"));

        verify(listenerManager).stopListener();
        verify(listenerManager2, never()).stopListener();
        org.junit.jupiter.api.Assertions.assertFalse(ListenersManager.LISTENERS.containsKey(acme));
        org.junit.jupiter.api.Assertions.assertTrue(ListenersManager.LISTENERS.containsKey(globex));
        org.junit.jupiter.api.Assertions.assertEquals(0, listenersManager.stopTenantListeners("acme"), "a second call finds nothing");
    }

    /**
     * Resuming a suspended tenant starts its consumers of every listener artefact, computed in that
     * tenant's scope - and nothing else: not one already running, not one bound to a global destination
     * (its subscription serves the deployment and was never stopped), not one the synchronizer holds
     * failed (#8477).
     */
    @Test
    void startTenantListenersStartsOnlyTheTenantsOwn() throws Throwable {
        String[] scope = {null};
        when(tenantContext.execute(eq("acme"), any())).thenAnswer(invocation -> {
            scope[0] = "acme";
            try {
                return invocation.<CallableResultAndException<?, ?>>getArgument(1)
                                 .call();
            } finally {
                scope[0] = null;
            }
        });
        when(listenerCreator.fromEntity(any())).thenAnswer(invocation -> {
            org.eclipse.dirigible.components.listeners.domain.Listener entity = invocation.getArgument(0);
            return new ListenerDescriptor(ListenerType.QUEUE, scope[0] + "###" + entity.getName(), HANDLER);
        });
        org.eclipse.dirigible.components.listeners.domain.Listener orders = listener("orders", ArtefactLifecycle.CREATED);
        org.eclipse.dirigible.components.listeners.domain.Listener running = listener("running", ArtefactLifecycle.CREATED);
        org.eclipse.dirigible.components.listeners.domain.Listener failed = listener("failed", ArtefactLifecycle.FAILED);
        org.eclipse.dirigible.components.listeners.domain.Listener global = listener("global:events", ArtefactLifecycle.CREATED);
        when(listenerService.getAll()).thenReturn(List.of(orders, running, failed, global));
        ListenersManager.LISTENERS.put(new ListenerDescriptor(ListenerType.QUEUE, "acme###running", HANDLER), listenerManager2);
        when(repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + IRepositoryStructure.SEPARATOR + HANDLER)).thenReturn(
                resource);
        when(resource.exists()).thenReturn(true);
        ListenerDescriptor acmeOrders = new ListenerDescriptor(ListenerType.QUEUE, "acme###orders", HANDLER);
        when(messageListenerManagerFactory.create(acmeOrders)).thenReturn(listenerManager);

        org.junit.jupiter.api.Assertions.assertEquals(1, listenersManager.startTenantListeners("acme"));

        verify(listenerManager).startListener();
        verify(listenerManager2, never()).startListener();
        verify(messageListenerManagerFactory).create(acmeOrders);
        verifyNoMoreInteractions(messageListenerManagerFactory);
        org.junit.jupiter.api.Assertions.assertTrue(ListenersManager.LISTENERS.containsKey(acmeOrders));
        org.junit.jupiter.api.Assertions.assertEquals(0, listenersManager.startTenantListeners("acme"), "a second call starts nothing");
    }

    /** A listener that cannot be started fails the resume, naming it, once the others are started. */
    @Test
    void startTenantListenersReportsTheListenersItCouldNotStart() throws Throwable {
        when(tenantContext.execute(eq("acme"), any())).thenAnswer(invocation -> invocation.<CallableResultAndException<?, ?>>getArgument(1)
                                                                                          .call());
        when(listenerCreator.fromEntity(any())).thenAnswer(invocation -> new ListenerDescriptor(ListenerType.QUEUE,
                "acme###" + invocation.<org.eclipse.dirigible.components.listeners.domain.Listener>getArgument(0)
                                      .getName(),
                HANDLER));
        when(listenerService.getAll()).thenReturn(List.of(listener("orders", ArtefactLifecycle.CREATED)));
        when(repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + IRepositoryStructure.SEPARATOR + HANDLER)).thenReturn(
                resource);
        when(resource.exists()).thenReturn(false);

        IllegalStateException refusal = assertThrows(IllegalStateException.class, () -> listenersManager.startTenantListeners("acme"));

        org.junit.jupiter.api.Assertions.assertTrue(refusal.getMessage()
                                                           .contains("orders"),
                refusal.getMessage());
    }

    private static org.eclipse.dirigible.components.listeners.domain.Listener listener(String name, ArtefactLifecycle lifecycle) {
        org.eclipse.dirigible.components.listeners.domain.Listener listener =
                new org.eclipse.dirigible.components.listeners.domain.Listener("/acme/" + name + ".listener", name, "", HANDLER,
                        ListenerKind.QUEUE);
        listener.setLifecycle(lifecycle);
        return listener;
    }
}
