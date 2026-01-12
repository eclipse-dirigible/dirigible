/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.synchronizer;

import org.eclipse.dirigible.components.base.ApplicationListenersOrder.ApplicationReadyEventListeners;
import org.eclipse.dirigible.components.initializers.classpath.ClasspathExpander;
import org.eclipse.dirigible.components.registry.watcher.ExternalRegistryWatcher;
import org.eclipse.dirigible.components.registry.watcher.RecursiveFolderWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The Class SynchronizersInitializer.
 */
@Order(ApplicationReadyEventListeners.SYNCHRONIZATION_INTIALIZER)
@Component
@Scope("singleton")
public class SynchronizationInitializer implements ApplicationListener<ApplicationReadyEvent> {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizationInitializer.class);

    /** The synchronization processor. */
    private final SynchronizationProcessor synchronizationProcessor;

    /** The classpath expander. */
    private final ClasspathExpander classpathExpander;

    private final ExternalRegistryWatcher externalRegistryWatcher;

    /**
     * Instantiates a new synchronizers initializer.
     *
     * @param synchronizationProcessor the synchronization processor
     * @param classpathExpander the classpath expander
     */
    public SynchronizationInitializer(SynchronizationProcessor synchronizationProcessor, ClasspathExpander classpathExpander,
            ExternalRegistryWatcher externalRegistryWatcher) {
        this.synchronizationProcessor = synchronizationProcessor;
        this.classpathExpander = classpathExpander;
        this.externalRegistryWatcher = externalRegistryWatcher;
    }

    /**
     * On application event.
     *
     * @param event the event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOGGER.info("Executing...");

        synchronizationProcessor.prepareSynchronizers();
        classpathExpander.expandContent();
        synchronizationProcessor.processSynchronizers();
        externalRegistryWatcher.initialize();

        LOGGER.info("Completed.");

    }

}
