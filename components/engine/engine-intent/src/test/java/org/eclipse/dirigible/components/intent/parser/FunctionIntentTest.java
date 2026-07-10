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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * Coverage for the explicit {@code function} presentation role at entity / field / relation level,
 * its back-compat with the legacy {@code documentTitle} / {@code kind: setting} flags and the
 * {@code *Item} naming, and the hard rename of the status role: the pre-rename
 * {@code function: DocumentStatus} and {@code documentStatus: true} are rejected with a migration
 * message.
 */
class FunctionIntentTest {

    // A document declared entirely by `function` - the child is NOT named "*Item".
    private static final String DOC_BY_FUNCTION = """
            name: timesheets
            entities:
              - name: ProjectTimesheet
                function: Document
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, function: DocumentTitle }
                relations:
                  - { name: Status, kind: manyToOne, to: TimesheetStatus, function: EntityStatus, init: 1 }
              - name: EmployeeTimesheet
                function: DocumentItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: total, type: decimal }
                relations:
                  - { name: ProjectTimesheet, kind: manyToOne, to: ProjectTimesheet, composition: true, required: true }
              - name: TimesheetStatus
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            """;

    @Test
    void parsesFunctionAtAllLevelsAndRolesResolve() {
        IntentModel model = IntentParser.parse(DOC_BY_FUNCTION);
        EntityIntent master = model.getEntities()
                                   .get(0);
        EntityIntent items = model.getEntities()
                                  .get(1);
        assertTrue(master.isDocument(), "function: Document -> isDocument");
        assertTrue(items.isDocumentItem(), "function: DocumentItem -> isDocumentItem");
        assertTrue(master.getFields()
                         .get(1)
                         .isDocumentTitle(),
                "function: DocumentTitle -> isDocumentTitle");
        assertTrue(master.getRelations()
                         .get(0)
                         .isEntityStatus(),
                "function: EntityStatus -> isEntityStatus");
    }

    @Test
    void legacyDocumentTitleBooleanStillResolvesButTheStatusBooleanIsRejected() {
        String yaml = """
                name: sales
                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, documentTitle: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: SalesInvoiceStatus, function: EntityStatus }
                  - name: SalesInvoiceItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                  - name: SalesInvoiceStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertTrue(model.getEntities()
                        .get(0)
                        .getFields()
                        .get(1)
                        .isDocumentTitle());
        assertTrue(model.getEntities()
                        .get(0)
                        .getRelations()
                        .get(0)
                        .isEntityStatus());
        assertTrue(model.getEntities()
                        .get(2)
                        .isSetting());
        assertIssue(yaml.replace("function: EntityStatus", "documentStatus: true"),
                "uses documentStatus: true - the status role was renamed; use function: EntityStatus");
    }

    @Test
    void entityStatusIsValidOnANonDocumentEntityAndTheOldSpellingIsRejected() {
        String yaml = """
                name: vacations
                entities:
                  - name: VacationEntitlement
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: EntitlementStatus, function: EntityStatus, init: 1 }
                  - name: LegacyRequest
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: EntitlementStatus, function: DocumentStatus }
                  - name: EntitlementStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                """;
        assertIssue(yaml, "uses function: DocumentStatus - the status role was renamed; use function: EntityStatus");
        IntentModel model = IntentParser.parse(yaml.replace("function: DocumentStatus", "function: EntityStatus"));
        assertTrue(model.getEntities()
                        .get(0)
                        .getRelations()
                        .get(0)
                        .isEntityStatus(),
                "function: EntityStatus on a plain (non-document) entity");
        assertTrue(model.getEntities()
                        .get(1)
                        .getRelations()
                        .get(0)
                        .isEntityStatus(),
                "the flipped relation resolves the same role");
    }

    @Test
    void rejectsUnknownEntityFunction() {
        String yaml = base("    function: Bogus\n");
        assertIssue(yaml, "unknown function [Bogus]");
    }

    @Test
    void gatesReservedBoardFunction() {
        // Calendar graduated to a first-class role (the view: calendar alias - see
        // FunctionCalendarTest); the remaining reserved roles keep the clear gate.
        String yaml = base("    function: Board\n");
        assertIssue(yaml, "reserved for an upcoming template");
    }

    @Test
    void rejectsDocumentItemThatIsNotACompositionChild() {
        // A standalone entity (no composition parent) cannot be a DocumentItem.
        String yaml = """
                name: t
                entities:
                  - name: Loose
                    function: DocumentItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                """;
        assertIssue(yaml, "must be a composition child");
    }

    @Test
    void rejectsDocumentWithNoResolvableItemsChild() {
        // function: Document but two unflagged, non-*Item composition children -> ambiguous, no items.
        String yaml = """
                name: t
                entities:
                  - name: Doc
                    function: Document
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: LineA
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Doc, kind: manyToOne, to: Doc, composition: true, required: true }
                  - name: LineB
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Doc, kind: manyToOne, to: Doc, composition: true, required: true }
                """;
        assertIssue(yaml, "has no line-items child");
    }

    @Test
    void rejectsUnknownFieldAndRelationFunctions() {
        assertIssue("""
                name: t
                entities:
                  - name: E
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: n, type: string, function: Bogus }
                """, "field [n] has unknown function [Bogus]");
        assertIssue("""
                name: t
                entities:
                  - name: E
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: R, kind: manyToOne, to: E, function: Bogus }
                """, "relation [R] has unknown function [Bogus]");
    }

    private static String base(String extraEntityLine) {
        return """
                name: t
                entities:
                  - name: E
                """ + extraEntityLine + """
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                """;
    }

    private static void assertIssue(String yaml, String fragment) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(fragment)),
                "expected an issue containing [" + fragment + "], got: " + ex.getIssues());
    }
}
