/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import org.eclipse.dirigible.engine.java.runtime.ClientClassLoaderHolder;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Makes a recompiled {@code flowable:class} service-task delegate take effect without a server
 * restart by evicting Flowable's parsed-process cache on every client Java rebuild.
 *
 * <p>
 * {@link ClientAwareClassLoader} already resolves the delegate <em>class</em> against the current
 * client generation on each call, but Flowable caches the instantiated delegate on the parsed
 * service task ({@code ClassDelegate.activityBehaviorInstance}), which lives in the
 * process-definition cache; an unchanged {@code .bpmn} is not redeployed, so that cached instance —
 * of the previous class version — would keep running. Clearing the cache forces a re-parse and a
 * fresh delegate instance (of the recompiled class) on the next process start.
 */
@Component
class FlowableClientClassLoaderRefresher {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableClientClassLoaderRefresher.class);

    private final ProcessEngine processEngine;
    private final ClientClassLoaderHolder clientClassLoaderHolder;

    FlowableClientClassLoaderRefresher(ProcessEngine processEngine, ClientClassLoaderHolder clientClassLoaderHolder) {
        this.processEngine = processEngine;
        this.clientClassLoaderHolder = clientClassLoaderHolder;
    }

    @PostConstruct
    void register() {
        clientClassLoaderHolder.addSwapListener(this::onClientRebuild);
    }

    private void onClientRebuild() {
        ProcessEngineConfiguration configuration = processEngine.getProcessEngineConfiguration();
        if (configuration instanceof ProcessEngineConfigurationImpl impl) {
            impl.getProcessDefinitionCache()
                .clear();
            LOGGER.debug("Evicted the Flowable process-definition cache after a client Java rebuild.");
        }
    }

}
