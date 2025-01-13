/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonAsserter {

    public static void assertEquals(String expectedJson, String actualJson) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            assertThat(mapper.readTree(actualJson)).isEqualTo(mapper.readTree(expectedJson))
                                                   .withFailMessage("Unexpected JSON");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unexpected JSON", e);
        }
    }
}
