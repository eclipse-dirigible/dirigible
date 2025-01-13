/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.config;

import org.apache.camel.Consumer;
import org.apache.camel.component.platform.http.HttpEndpointModel;
import org.eclipse.dirigible.components.engine.camel.processor.DirigibleHttpEndpointModel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * The Class DirigibleHttpEndpointModelTest.
 */
class DirigibleHttpEndpointModelTest {

    /**
     * Test create from input model.
     *
     * @param inputUri the input uri
     * @param expectedTransformedUri the expected transformed uri
     */
    @ParameterizedTest
    @MethodSource("provideParameters")
    public void testCreateFromInputModel(String inputUri, String expectedTransformedUri) {
        String verbs = "GET";
        Consumer consumer = mock(Consumer.class);
        HttpEndpointModel inputModel = new HttpEndpointModel(inputUri, verbs, consumer);

        var outputModel = DirigibleHttpEndpointModel.from(inputModel);
        assertEquals(expectedTransformedUri, outputModel.getUri(), "Unexpected URI");
        assertEquals(verbs, outputModel.getVerbs(), "Unexpected Verbs");
        assertEquals(consumer, outputModel.getConsumer(), "Unexpected Consumer");
    }

    /**
     * Provide parameters.
     *
     * @return the stream
     */
    private static Stream<Arguments> provideParameters() {
        return Stream.of(Arguments.of("/test-uri", "/services/integrations/test-uri"),
                Arguments.of("test-uri", "/services/integrations/test-uri"), Arguments.of("", "/services/integrations"),
                Arguments.of(null, "/services/integrations"));
    }
}
