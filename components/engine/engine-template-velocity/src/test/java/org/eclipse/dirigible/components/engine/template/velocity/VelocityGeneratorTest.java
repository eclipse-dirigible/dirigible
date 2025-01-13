/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.template.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.dirigible.components.engine.template.TemplateEngine;
import org.junit.jupiter.api.Test;

/**
 * The Class VelocityGeneratorTest.
 */
public class VelocityGeneratorTest {

    /**
     * Generate.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void generate() throws IOException {
        TemplateEngine generationEngine = new VelocityGenerationEngine();
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("testParameter", "testValue");
        byte[] result = generationEngine.generate(parameters, "/location", "test $testParameter".getBytes(), null, null);
        assertEquals("test testValue", new String(result));
    }

}
