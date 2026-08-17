/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.synchronizer;

import org.eclipse.dirigible.components.base.dependencies.DependenciesChangedEvent;
import org.eclipse.dirigible.engine.java.runtime.ClassPathIndex;
import org.eclipse.dirigible.engine.java.runtime.CompiledModuleClassProvider;
import org.eclipse.dirigible.engine.java.runtime.ModulesClassLoaderHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The Java engine's reaction to a dependency-layer swap: invalidate the compile classpath so client
 * sources compile against the new JAR set, rediscover AOT compiled modules through the new modules
 * classloader (registering arrived module classes and unregistering removed ones), and rebuild the
 * client sources so the new {@code ClientClassLoader} generation parents on the new modules
 * classloader. Runs synchronously on the swapping thread - the swap pipeline's success includes
 * this reaction.
 */
@Component
class JavaDependenciesChangedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaDependenciesChangedListener.class);

    /** The class path index. */
    private final ClassPathIndex classPathIndex;

    /** The compiled module class provider. */
    private final CompiledModuleClassProvider compiledModuleClassProvider;

    /** The modules loader holder. */
    private final ModulesClassLoaderHolder modulesLoaderHolder;

    /** The java synchronizer. */
    private final JavaSynchronizer javaSynchronizer;

    /**
     * Instantiates a new listener.
     *
     * @param classPathIndex the class path index
     * @param compiledModuleClassProvider the compiled module class provider
     * @param modulesLoaderHolder the modules loader holder
     * @param javaSynchronizer the java synchronizer
     */
    JavaDependenciesChangedListener(ClassPathIndex classPathIndex, CompiledModuleClassProvider compiledModuleClassProvider,
            ModulesClassLoaderHolder modulesLoaderHolder, JavaSynchronizer javaSynchronizer) {
        this.classPathIndex = classPathIndex;
        this.compiledModuleClassProvider = compiledModuleClassProvider;
        this.modulesLoaderHolder = modulesLoaderHolder;
        this.javaSynchronizer = javaSynchronizer;
    }

    /**
     * On dependencies changed.
     *
     * @param event the event
     */
    @EventListener
    void onDependenciesChanged(DependenciesChangedEvent event) {
        LOGGER.info("Dependency layer swapped to generation [{}] (added {}, removed {}) - rediscovering AOT modules and rebuilding "
                + "the client sources", event.getGeneration(), event.getAdded(), event.getRemoved());
        classPathIndex.invalidate();
        compiledModuleClassProvider.rediscover(modulesLoaderHolder.current());
        javaSynchronizer.rebuildOnDependenciesChanged();
    }

}
