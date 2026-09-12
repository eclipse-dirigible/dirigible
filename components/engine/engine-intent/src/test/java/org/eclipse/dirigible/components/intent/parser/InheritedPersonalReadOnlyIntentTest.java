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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@code personalReadOnly: true} on the composition edge a child inherits its personal scope
 * through (dirigible #7340): the scope still comes from the parent, the writes do not. The key is
 * refused anywhere it would be carried nowhere, because a silently dropped access declaration reads
 * as a grant.
 */
class InheritedPersonalReadOnlyIntentTest {

    private static final String YAML = """
            name: hr
            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 200 }
                  - { name: email, type: string, required: true, unique: true, length: 320 }
              - name: VacationRequest
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }
              - name: VacationDay
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: day, type: date }
                relations:
                  - { name: Request, kind: manyToOne, to: VacationRequest, composition: true, required: true, personalReadOnly: true }
            """;

    @Test
    void onTheOwningCompositionItParses() {
        assertTrue(IntentParser.parse(YAML)
                               .getEntities()
                               .get(2)
                               .getRelations()
                               .get(0)
                               .isPersonalReadOnly());
    }

    /** The parent it inherits the scope from keeps its own writable personal surface. */
    @Test
    void theMastersOwnPersonalSurfaceIsUntouched() {
        assertFalse(IntentParser.parse(YAML)
                                .getEntities()
                                .get(1)
                                .getRelations()
                                .get(0)
                                .isPersonalReadOnly());
    }

    /** Without the key the child stays writable - an existing model parses byte-identically. */
    @Test
    void omittedParses() {
        assertFalse(IntentParser.parse(YAML.replace(", personalReadOnly: true", ""))
                                .getEntities()
                                .get(2)
                                .getRelations()
                                .get(0)
                                .isPersonalReadOnly());
    }

    /** A plain association carries no inherited scope, so the key there would lock nothing. */
    @Test
    void onAPlainAssociationItIsRejected() {
        assertIssue(YAML.replace("composition: true, ", ""), "declares personalReadOnly but neither personal: true nor composition: true");
    }

    /** Only the FIRST composition is the edge the scope travels; a later one is a plain association. */
    @Test
    void onASecondCompositionItIsRejected() {
        String yaml = YAML.replace(
                "- { name: Request, kind: manyToOne, to: VacationRequest, composition: true, required: true, personalReadOnly: true }",
                "- { name: Request, kind: manyToOne, to: VacationRequest, composition: true, required: true }\n"
                        + "      - { name: Batch, kind: manyToOne, to: VacationRequest, composition: true, personalReadOnly: true }");
        assertIssue(yaml, "the entity's owning composition is [Request]");
    }

    /** A master with no personal surface leaves nothing here to make see-only. */
    @Test
    void withoutAPersonalSurfaceToInheritItIsRejected() {
        assertIssue(YAML.replace(", personal: true", ""), "has no personal surface to inherit");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains(expected),
                "expected issue containing [" + expected + "] but got: " + ex.getMessage());
    }
}
