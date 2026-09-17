/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Validation of a composition's {@code whenMasterDeleted: cascade | refuse} construct (dirigible
 * #7100) - what a DELETE of the MASTER does to the children the relation owns.
 */
class WhenMasterDeletedIntentTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: SalesOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
              - name: SalesOrderItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
                relations:
                  - { name: order, kind: manyToOne, to: SalesOrder, composition: true, required: true, whenMasterDeleted: refuse }
            """;

    @Test
    void refuseParses() {
        assertEquals("refuse", IntentParser.parse(YAML)
                                           .getEntities()
                                           .get(1)
                                           .getRelations()
                                           .get(0)
                                           .getWhenMasterDeleted());
    }

    @Test
    void cascadeParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML.replace("whenMasterDeleted: refuse", "whenMasterDeleted: cascade")));
    }

    /** Omitted is the default - cascade - and needs no key at all. */
    @Test
    void omittedParsesAndDoesNotRefuse() {
        assertFalse(IntentParser.parse(YAML.replace(", whenMasterDeleted: refuse", ""))
                                .getEntities()
                                .get(1)
                                .getRelations()
                                .get(0)
                                .isMasterDeleteRefused());
    }

    @Test
    void anUnknownValueIsRejected() {
        assertIssue(YAML.replace("whenMasterDeleted: refuse", "whenMasterDeleted: orphan"), "whenMasterDeleted [orphan] must be `cascade`");
    }

    /** Only an owning edge has children whose fate a delete of the master decides. */
    @Test
    void onAPlainAssociationItIsRejected() {
        assertIssue(YAML.replace("composition: true, ", ""), "declares whenMasterDeleted but only a composition owns children");
    }

    /**
     * A second composition is emitted as a plain association, so the key there would ask for a cascade
     * nothing would run.
     */
    @Test
    void onASecondCompositionItIsRejected() {
        String yaml = YAML.replace(
                "- { name: order, kind: manyToOne, to: SalesOrder, composition: true, required: true," + " whenMasterDeleted: refuse }",
                "- { name: order, kind: manyToOne, to: SalesOrder, composition: true, required: true }\n"
                        + "      - { name: batch, kind: manyToOne, to: SalesOrder, composition: true, whenMasterDeleted: refuse }");
        assertIssue(yaml, "the entity's owning composition is [order]");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains(expected),
                "expected issue containing [" + expected + "] but got: " + ex.getMessage());
    }
}
