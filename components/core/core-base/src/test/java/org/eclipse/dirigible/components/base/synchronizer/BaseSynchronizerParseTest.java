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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.text.ParseException;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.open.telemetry.OpenTelemetryProvider;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.opentelemetry.api.OpenTelemetry;

/**
 * Guards how {@link BaseSynchronizer#parse(String, byte[])} contains a failure of the parser it
 * delegates to. The unit under test is the boundary, not any concrete synchronizer.
 */
class BaseSynchronizerParseTest {

    private static final String LOCATION = "/project/Big.java";

    /**
     * A {@link StackOverflowError} from a parser is a property of the one definition being analysed. If
     * it propagates it ends the whole synchronization run - and because the same definition is parsed
     * again on every later pass, no pass ever completes again while the server keeps answering 200. It
     * must arrive as the ordinary "this definition is broken" signal the processor already handles.
     */
    @Test
    void a_stack_overflow_from_the_parser_is_reported_as_a_broken_definition() {
        ParseException thrown = withNoopTelemetry(() -> assertThrows(ParseException.class, () -> synchronizerFailingWith(() -> {
            throw new StackOverflowError();
        }).parse(LOCATION, new byte[0])));

        assertTrue(thrown.getMessage()
                         .contains(LOCATION),
                "the failing definition must be named: " + thrown.getMessage());
    }

    /** Everything else keeps its existing contract - the containment is not a blanket catch. */
    @Test
    void a_runtime_exception_from_the_parser_still_propagates() {
        IllegalStateException thrown =
                withNoopTelemetry(() -> assertThrows(IllegalStateException.class, () -> synchronizerFailingWith(() -> {
                    throw new IllegalStateException("boom");
                }).parse(LOCATION, new byte[0])));

        assertEquals("boom", thrown.getMessage());
    }

    /** {@code OpenTelemetryProvider} resolves its tracer from the Spring context, which no test has. */
    private static <T> T withNoopTelemetry(Supplier<T> body) {
        try (MockedStatic<OpenTelemetryProvider> telemetry = mockStatic(OpenTelemetryProvider.class)) {
            telemetry.when(OpenTelemetryProvider::get)
                     .thenReturn(OpenTelemetry.noop());
            return body.get();
        }
    }

    private static BaseSynchronizer<TestArtefact, Long> synchronizerFailingWith(Runnable failure) {
        return new BaseSynchronizer<>() {

            @Override
            protected List<TestArtefact> parseImpl(String location, byte[] content) {
                failure.run();
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
            public void setStatus(TestArtefact artefact, ArtefactLifecycle lifecycle, String message) {
                // Not exercised by the parse boundary.
            }

            @Override
            protected boolean completeImpl(TopologyWrapper<TestArtefact> wrapper, ArtefactPhase flow) {
                return true;
            }

            @Override
            protected void cleanupImpl(TestArtefact artefact) {
                // Not exercised by the parse boundary.
            }

            @Override
            public void setCallback(SynchronizerCallback callback) {
                // Not exercised by the parse boundary.
            }

            @Override
            public String getFileExtension() {
                return ".test";
            }

            @Override
            public String getArtefactType() {
                return "test";
            }
        };
    }

    private static final class TestArtefact extends Artefact {
    }

}
