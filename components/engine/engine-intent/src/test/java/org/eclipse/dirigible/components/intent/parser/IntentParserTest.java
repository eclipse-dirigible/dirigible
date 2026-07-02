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

    /** A shop model with the two canonical Depends-On shapes: a cascade and a scalar auto-populate. */
    private static final String DEPENDS_ON_HEAD = """
            name: shop
            entities:
              - name: Country
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: City
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
                  - { name: Country, kind: manyToOne, to: Country }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
                  - { name: Country, kind: manyToOne, to: Country }
            """;

    @Test
    void dependsOnCascadeAndAutoPopulateParse() {
        // filterBy/valueFrom reference the target's properties by their AUTHORED names (a field by its
        // lower-camel name, a relation by its declared name) - City's FK to Country is the relation
        // named `Country`.
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: Country, filterBy: Country } }
                """;
        IntentModel model = IntentParser.parse(yaml);
        var dependsOn = model.getEntities()
                             .get(2)
                             .getRelations()
                             .get(1)
                             .getDependsOn();
        assertEquals("Country", dependsOn.getRelation());
        assertEquals("Country", dependsOn.getFilterBy());
    }

    @Test
    void dependsOnUnknownTriggerRelationIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: Region, filterBy: country } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("dependsOn relation [Region] is not a to-one relation")),
                "expected a dangling-trigger issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnSelfTriggerIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: City, filterBy: country } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("cannot reference itself")),
                "expected a self-trigger issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnUnknownFilterByOnOwnTargetIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: Country, filterBy: region } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("filterBy [region] is not a field or to-one relation of [City]")),
                "expected an unknown-filterBy issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnUnknownValueFromOnTriggerTargetIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: Country, valueFrom: iso, filterBy: country } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("valueFrom [iso] is not a field or to-one relation of [Country]")),
                "expected an unknown-valueFrom issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnRelationWithNeitherValueFromNorFilterByIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: Country } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("requires `valueFrom` and/or `filterBy`")),
                "expected a missing-valueFrom/filterBy issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnFieldRequiresValueFromAndForbidsFilterBy() {
        String yaml = """
                name: shop
                entities:
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: price, type: decimal }
                  - name: OrderItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: price, type: decimal, dependsOn: { relation: Product, filterBy: price } }
                    relations:
                      - { name: Product, kind: manyToOne, to: Product }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("requires `valueFrom`")),
                "expected a missing-valueFrom issue, got: " + ex.getIssues());
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("`filterBy` applies only to a relation")),
                "expected a filterBy-on-field issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnOnDocumentStatusRelationIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, documentStatus: true, dependsOn: { relation: Country, filterBy: country } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("documentStatus (a read-only pill) so it cannot declare dependsOn")),
                "expected a documentStatus-dependent issue, got: " + ex.getIssues());
    }
}
