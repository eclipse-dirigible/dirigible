/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import com.google.gson.Gson;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the JSON literals a model value is written into a generated artefact as.
 */
class JsonLiteralsTest {

    private static final Gson GSON = new Gson();

    @Test
    void leavesAnOrdinaryValueAlone() {
        assertEquals("\"DRAFT\"", JsonLiterals.stringLiteral("DRAFT"));
        assertEquals("\"6 mm\"", JsonLiterals.stringLiteral("6 mm"));
        assertEquals("\"Ц\"", JsonLiterals.stringLiteral("Ц"));
        assertEquals("\"\"", JsonLiterals.stringLiteral(""));
    }

    /**
     * The defect: the quote used to end the string it was being written into, so one authored inch mark
     * left the whole schema artefact unparseable and every table of the project uncreated (#7206).
     */
    @Test
    void escapesTheCharactersThatWouldEndTheString() {
        assertEquals("\"6\\\"\"", JsonLiterals.stringLiteral("6\""));
        assertEquals("\"C:\\\\tmp\"", JsonLiterals.stringLiteral("C:\\tmp"));
        assertEquals("\"\\\\\\\"\"", JsonLiterals.stringLiteral("\\\""));
    }

    /**
     * A raw control character is not admitted in a JSON string at all, whatever the quotes around it.
     */
    @Test
    void escapesTheControlCharactersJsonDoesNotAdmit() {
        assertEquals("\"a\\nb\"", JsonLiterals.stringLiteral("a\nb"));
        assertEquals("\"a\\rb\"", JsonLiterals.stringLiteral("a\rb"));
        assertEquals("\"a\\tb\"", JsonLiterals.stringLiteral("a\tb"));
        assertEquals("\"a\\bb\"", JsonLiterals.stringLiteral("a\bb"));
        assertEquals("\"a\\fb\"", JsonLiterals.stringLiteral("a\fb"));
        assertEquals("\"a\\u0000b\"", JsonLiterals.stringLiteral("a\u0000b"));
        assertEquals("\"a\\u001bb\"", JsonLiterals.stringLiteral("a\u001bb"));
    }

    /**
     * The DEFAULT reaches the DDL from the parsed schema, so the literal has to read back as the value
     * the author wrote - escaping it must not change the column's default, only keep it from taking the
     * artefact with it.
     */
    @Test
    void readsBackAsTheAuthoredValue() {
        for (String value : new String[] {"DRAFT", "'DRAFT'", "6\"", "C:\\tmp", "a\nb", "a\u0000b", "\\\"", "Ц"}) {
            assertEquals(value, GSON.fromJson(JsonLiterals.stringLiteral(value), JsonPrimitive.class)
                                    .getAsString(),
                    "the escaped literal must parse back to the authored value");
        }
    }
}
