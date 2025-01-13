/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.service;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        listenersManager.startListener(listenerEntity);

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
    }
}
