/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.synchronizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.script.ScriptEngineUnavailableException;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.engine.camel.components.DirigibleJavaScriptInvoker;
import org.eclipse.dirigible.components.engine.camel.domain.Camel;
import org.eclipse.dirigible.components.engine.camel.processor.CamelProcessor;
import org.eclipse.dirigible.components.engine.camel.service.CamelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * A route whose step is a script is refused where the script engine is not deployed - FAILED,
 * naming the missing engine - instead of started to fail every exchange (#8174).
 */
class CamelSynchronizerWithoutScriptEngineTest {

    /** A step calling a script through the {@code dirigible-java-script} endpoint. */
    private static final String SCRIPT_ENDPOINT_ROUTE = """
            - route:
                id: http-route
                from:
                  uri: platform-http
                  parameters:
                    path: AsyncStepRoute
                  steps:
                    - to:
                        description: Call async-step.ts
                        uri: dirigible-java-script
                        parameters:
                          javaScriptPath: AsyncStep/async-step.ts
            """;

    /** A step calling a script through the {@code Invoker} bean and the {@code resource} property. */
    private static final String INVOKER_ROUTE = """
            - route:
                id: orders-sync-route
                from:
                  uri: cron
                  parameters:
                    schedule: 0/10 * * * * ?
                  steps:
                    - setProperty:
                        name: resource
                        expression:
                          simple:
                            expression: Etl/sync/get-db-type.ts
                    - to:
                        uri: class
                        parameters:
                          beanName: org.eclipse.dirigible.components.engine.camel.invoke.Invoker
            """;

    /** A route that runs client Java only. */
    private static final String JAVA_ROUTE = """
            - route:
                id: java-route
                from:
                  uri: platform-http
                  parameters:
                    path: JavaRoute
                  steps:
                    - to:
                        uri: dirigible-java
                        parameters:
                          className: app.Handler
            """;

    private final CamelProcessor camelProcessor = mock(CamelProcessor.class);
    private final SynchronizerCallback callback = mock(SynchronizerCallback.class);
    private final ObjectProvider<DirigibleJavaScriptInvoker> invoker = mockProvider();
    private CamelSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        CamelService camelService = mock(CamelService.class);
        when(camelService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        synchronizer = new CamelSynchronizer(camelService, camelProcessor, invoker);
        synchronizer.setCallback(callback);
    }

    @Test
    void aRouteCallingAScriptEndpointIsFailedNamingTheMissingEngine() {
        assertRefused(SCRIPT_ENDPOINT_ROUTE, "AsyncStep/async-step.ts");
    }

    @Test
    void aRouteCallingAScriptThroughTheInvokerIsFailedNamingTheMissingEngine() {
        assertRefused(INVOKER_ROUTE, "Etl/sync/get-db-type.ts");
    }

    @Test
    void aJavaRouteStillStarts() {
        TopologyWrapper<Camel> wrapper = wrapper(JAVA_ROUTE);

        assertTrue(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE));

        verify(camelProcessor).onCreateOrUpdate(wrapper.getArtefact());
        verify(callback).registerState(synchronizer, wrapper, ArtefactLifecycle.CREATED);
    }

    @Test
    void aScriptRouteStartsWhereTheEngineIsDeployed() {
        when(invoker.getIfAvailable()).thenReturn(mock(DirigibleJavaScriptInvoker.class));
        TopologyWrapper<Camel> wrapper = wrapper(SCRIPT_ENDPOINT_ROUTE);

        assertTrue(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE));

        verify(camelProcessor).onCreateOrUpdate(wrapper.getArtefact());
    }

    @Test
    void aRouteRunningNoScriptIsNotReportedAsOne() {
        assertEquals(Optional.empty(), CamelScriptHandlers.find("/p/java.camel", JAVA_ROUTE));
    }

    private void assertRefused(String route, String handler) {
        TopologyWrapper<Camel> wrapper = wrapper(route);

        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE));

        ArgumentCaptor<Throwable> cause = ArgumentCaptor.forClass(Throwable.class);
        verify(callback).registerState(eq(synchronizer), eq(wrapper), eq(ArtefactLifecycle.FAILED), cause.capture());
        assertInstanceOf(ScriptEngineUnavailableException.class, cause.getValue());
        assertEquals(new ScriptEngineUnavailableException(handler).getMessage(), cause.getValue()
                                                                                      .getMessage());
        verify(camelProcessor, never()).onCreateOrUpdate(any());
    }

    private TopologyWrapper<Camel> wrapper(String content) {
        Camel camel = new Camel();
        camel.setLocation("/p/route.camel");
        camel.setName("route.camel");
        camel.setType(Camel.ARTEFACT_TYPE);
        camel.updateKey();
        camel.setContent(content);
        camel.setLifecycle(ArtefactLifecycle.NEW);
        return new TopologyWrapper<>(camel, new HashMap<>(), synchronizer);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DirigibleJavaScriptInvoker> mockProvider() {
        return mock(ObjectProvider.class);
    }
}
