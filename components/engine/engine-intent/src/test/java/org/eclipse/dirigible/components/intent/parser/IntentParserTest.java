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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

class IntentParserTest {

    private static final String HEAD = """
            name: lib
            entities:
              - name: Member
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string }
              - name: Loan
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: member, kind: manyToOne, to: Member }
            notifications:
              - name: loanUpdated
                event: { onUpdate: Loan }
                subject: "x"
            """;

    @Test
    void braceRecipientReportsACleanIssueInsteadOfA500() {
        // `to: {member.email}` is YAML flow-mapping (an object), not a string. The typed mapping used
        // to throw a raw Gson JsonSyntaxException (-> 500); it must now be a clean validation issue.
        String yaml = HEAD.stripTrailing() + "\n    to: {member.email}\n";
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("wrong type")),
                "a wrong-typed scalar should be reported as a validation issue, got: " + ex.getIssues());
    }

    @Test
    void bareOneHopRelationFieldRecipientParses() {
        // `to: member.email` (bare, no braces) is the supported one-hop relation.field recipient.
        String yaml = HEAD.stripTrailing() + "\n    to: member.email\n";
        IntentModel model = IntentParser.parse(yaml);
        assertEquals("member.email", model.getNotifications()
                                          .get(0)
                                          .getTo());
    }

    @Test
    void crossModelRelationParsesWhenModelIsDeclaredInUses() {
        String yaml = """
                name: customers
                uses:
                  - { model: countries }
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Country, kind: manyToOne, to: Country, model: countries }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals("countries", model.getEntities()
                                       .get(0)
                                       .getRelations()
                                       .get(0)
                                       .getModel());
        assertTrue(model.getEntities()
                        .get(0)
                        .getRelations()
                        .get(0)
                        .isCrossModel());
    }

    @Test
    void crossModelRelationToUndeclaredModelIsRejected() {
        String yaml = """
                name: customers
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Country, kind: manyToOne, to: Country, model: countries }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("undeclared model")),
                "expected an undeclared-model issue, got: " + ex.getIssues());
    }

    @Test
    void crossModelCompositionIsRejected() {
        String yaml = """
                name: sales
                uses:
                  - { model: customers }
                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer, model: customers, composition: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("cannot be a composition")),
                "expected a cross-model composition issue, got: " + ex.getIssues());
    }

    @Test
    void intraModelDanglingRelationTargetStillRejected() {
        String yaml = """
                name: lib
                entities:
                  - name: Loan
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: book, kind: manyToOne, to: Book }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("unknown entity")),
                "expected an unknown-entity issue, got: " + ex.getIssues());
    }

    @Test
    void uniqueAndCalculatedFieldAttributesParse() {
        String yaml = """
                name: sales
                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: uuid, type: uuid, unique: true }
                      - { name: number, type: string, calculatedOnCreate: "java.util.UUID.randomUUID().toString()" }
                """;
        IntentModel model = IntentParser.parse(yaml);
        var fields = model.getEntities()
                          .get(0)
                          .getFields();
        assertTrue(fields.get(1)
                         .isUnique());
        assertTrue(fields.get(2)
                         .isCalculated());
    }
}
