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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The Class ListenersManager.
 */
@Component("ListenersManager")
public class ListenersManager {

    /** The listeners. */
    static final Map<ListenerDescriptor, ListenerManager> LISTENERS = Collections.synchronizedMap(new HashMap<>());

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenersManager.class);

    /** The repository. */
    private final IRepository repository;

    /** The message listener manager factory. */
    private final ListenerManagerFactory messageListenerManagerFactory;

    /** The listener creator. */
    private final ListenerCreator listenerCreator;

    /**
     * Instantiates a new listeners manager.
     *
     * @param repository the repository
     * @param messageListenerManagerFactory the message listener manager factory
     * @param listenerCreator the listener creator
     */
    @Autowired
    public ListenersManager(IRepository repository, ListenerManagerFactory messageListenerManagerFactory, ListenerCreator listenerCreator) {
        this.repository = repository;
        this.messageListenerManagerFactory = messageListenerManagerFactory;
        this.listenerCreator = listenerCreator;
    }

    /**
     * Start listener.
     *
     * @param listenerEntity the listener
     */
    public void startListener(org.eclipse.dirigible.components.listeners.domain.Listener listenerEntity) {
        ListenerDescriptor listenerDescriptor = listenerCreator.fromEntity(listenerEntity);
        if (LISTENERS.containsKey(listenerDescriptor)) {
            LOGGER.warn("Message consumer for listener [{}] already running!", listenerDescriptor);
            return;

        }
        if (isMissingHandler(listenerDescriptor)) {
            LOGGER.error("Listener {} cannot be started, because the handler does not exist!", listenerDescriptor);
            return;
        }
        ListenerManager listenerManager = messageListenerManagerFactory.create(listenerDescriptor);
        listenerManager.startListener();

        LISTENERS.put(listenerDescriptor, listenerManager);
        LOGGER.info("Listener [{}] started.", listenerDescriptor);
    }

    /**
     * Checks if is missing handler.
     *
     * @param listenerDescriptor the listener
     * @return true, if is missing handler
     */
    private boolean isMissingHandler(ListenerDescriptor listenerDescriptor) {
        IResource resource = repository.getResource(
                IRepositoryStructure.PATH_REGISTRY_PUBLIC + IRepositoryStructure.SEPARATOR + listenerDescriptor.getHandlerPath());
        return !resource.exists();
    }

    /**
     * Stop listeners.
     */
    public synchronized void stopListeners() {
        LOGGER.info("Stopping all background listeners...");

        LISTENERS.forEach((l, m) -> {
            if (null != m) {
                m.stopListener();
            }
        });
        LISTENERS.clear();
    }

    /**
     * Stop listener.
     *
     * @param listenerEntity the listener
     */
    public void stopListener(org.eclipse.dirigible.components.listeners.domain.Listener listenerEntity) {
        ListenerDescriptor listenerDescriptor = listenerCreator.fromEntity(listenerEntity);
        ListenerManager listenerManager = LISTENERS.get(listenerDescriptor);
        if (listenerManager != null) {
            listenerManager.stopListener();
            LISTENERS.remove(listenerDescriptor);
        } else {
            LOGGER.warn("There is NO configured listener for [{}]", listenerDescriptor);
        }
    }

}
