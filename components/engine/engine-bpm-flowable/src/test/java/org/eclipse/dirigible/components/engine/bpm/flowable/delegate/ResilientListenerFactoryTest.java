/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultListenerFactory;
import org.flowable.engine.impl.bpmn.parser.factory.ListenerFactory;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.Test;

/**
 * A {@code flowable:class} execution or task listener must reach the same client-bean seam a
 * {@code flowable:class} service task does (#7222). Flowable builds its own
 * {@link DefaultListenerFactory} carrying a stock {@code DefaultClassDelegateFactory}, so without
 * the registration {@code BpmFlowableConfig} makes, a client listener is instantiated reflectively
 * and its collaborators read {@code null} - the #7058 symptom, one artefact type over.
 *
 * <p>
 * This pins the mechanism against a real engine: that a pre-set listener factory survives the
 * engine's own {@code initListenerFactory} (and gets its expression manager), and that both
 * listener kinds then come out as {@link ResilientClassDelegate}s, whose
 * {@code instantiateDelegate} is the seam. The defect case is pinned alongside, so a dropped
 * registration fails here. The end-to-end proof that the collaborators are really injected is
 * {@code JavaDelegateInjectionIT}.
 */
class ResilientListenerFactoryTest {

    /** A client-shaped listener class; only the type Flowable creates for it is under test here. */
    public static class SampleListener implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            // Never invoked: the factory call under test only creates the ClassDelegate.
        }
    }

    @Test
    void aClassListenerIsCreatedThroughTheClientBeanSeam() {
        withEngine(true, configuration -> {
            ListenerFactory factory = configuration.getListenerFactory();

            assertInstanceOf(ResilientClassDelegate.class, factory.createClassDelegateExecutionListener(classListener()),
                    "a flowable:class execution listener must be built by the resilient delegate, which wires the client bean container");
            assertInstanceOf(ResilientClassDelegate.class, factory.createClassDelegateTaskListener(classListener()),
                    "a flowable:class task listener must be built by the resilient delegate, which wires the client bean container");
        });
    }

    @Test
    void theConfiguredListenerFactoryIsKeptAndGetsTheExpressionManager() {
        withEngine(true, configuration -> {
            assertInstanceOf(DefaultListenerFactory.class, configuration.getListenerFactory(),
                    "the engine must keep the configured listener factory instead of building its own");
            assertNotNull(((DefaultListenerFactory) configuration.getListenerFactory()).getExpressionManager(),
                    "the engine injects the expression manager into a pre-set listener factory - an expression listener needs it");
        });
    }

    @Test
    void withoutTheRegistrationTheListenerIsAStockClassDelegate() {
        withEngine(false, configuration -> assertTrue(!(configuration.getListenerFactory()
                                                                     .createClassDelegateExecutionListener(
                                                                             classListener()) instanceof ResilientClassDelegate),
                "the defect this pins: Flowable's own listener factory bypasses the client bean seam"));
    }

    private static FlowableListener classListener() {
        FlowableListener listener = new FlowableListener();
        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        listener.setImplementation(SampleListener.class.getName());
        return listener;
    }

    private static void withEngine(boolean registerListenerFactory,
            java.util.function.Consumer<ProcessEngineConfigurationImpl> assertions) {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:listener-factory-test-" + registerListenerFactory + ";DB_CLOSE_DELAY=1000");
        if (registerListenerFactory) {
            configuration.setListenerFactory(new DefaultListenerFactory(new ResilientClassDelegateFactory()));
        }
        ProcessEngine engine = configuration.buildProcessEngine();
        try {
            assertions.accept((ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration());
        } finally {
            engine.close();
        }
    }
}
