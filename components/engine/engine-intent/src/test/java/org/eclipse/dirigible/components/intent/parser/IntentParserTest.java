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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.NumberIntent;
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
    void monthAndWeekAreAcceptedFieldTypesWhileAnUnknownTypeIsRejected() {
        String ok = """
                name: planning
                entities:
                  - name: Plan
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: period, type: month }
                      - { name: sprint, type: week }
                """;
        IntentModel model = IntentParser.parse(ok);
        assertEquals("month", model.getEntities()
                                   .get(0)
                                   .getFields()
                                   .get(1)
                                   .getType());
        assertEquals("week", model.getEntities()
                                  .get(0)
                                  .getFields()
                                  .get(2)
                                  .getType());

        String bad = ok.replace("type: month", "type: quarter");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(bad));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("unknown type")),
                "an unknown field type should still be rejected, got: " + ex.getIssues());
    }

    @Test
    void firstClassNumberingParsesAndValidates() {
        String ok = """
                name: billing
                entities:
                  - name: Company
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: date, type: date }
                      - { name: number, type: string, number: { series: Sales Invoice, per: Company, stampOn: issue } }
                    relations:
                      - { name: Company, kind: manyToOne, to: Company }
                """;
        NumberIntent number = IntentParser.parse(ok)
                                          .getEntities()
                                          .get(1)
                                          .getFields()
                                          .get(2)
                                          .getNumber();
        // The intent references a series and (optionally) what partitions it - never how it looks.
        assertEquals("Sales Invoice", number.getSeries());
        assertEquals("Company", number.getPer());
        assertEquals("issue", number.getStampOn());

        // number on a non-string field is rejected.
        String onDate = ok.replace("- { name: number, type: string, number:", "- { name: bad, type: date, number:");
        assertTrue(assertThrows(IntentValidationException.class, () -> IntentParser.parse(onDate)).getIssues()
                                                                                                  .stream()
                                                                                                  .anyMatch(i -> i.contains(
                                                                                                          "only a string field")),
                "number on a date field must be rejected");

        // an unknown stampOn is rejected.
        String badStamp = ok.replace("stampOn: issue", "stampOn: whenever");
        assertTrue(assertThrows(IntentValidationException.class, () -> IntentParser.parse(badStamp)).getIssues()
                                                                                                    .stream()
                                                                                                    .anyMatch(i -> i.contains("stampOn")),
                "an unknown stampOn must be rejected");

        // `per` must name a to-one RELATION: the partition identifies the record that owes the range
        // (typically the company), and a scalar would silently change partition when someone edits it.
        String badPer = ok.replace("per: Company", "per: date");
        assertTrue(assertThrows(IntentValidationException.class, () -> IntentParser.parse(badPer)).getIssues()
                                                                                                  .stream()
                                                                                                  .anyMatch(i -> i.contains(
                                                                                                          "number `per` [date] is not a to-one relation")),
                "a `per` that is not a to-one relation must be rejected");
    }

    /**
     * The removed number keys must fail LOUDLY on the raw YAML: the typed Gson mapping has no fields
     * for them, so without the raw-tree check an intent still carrying {@code format:} would parse
     * "successfully" and silently lose the author's shape.
     */
    @Test
    void removedNumberKeysAreRejectedLoudlyNotSilentlyDropped() {
        String template = """
                name: billing
                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, number: { series: Sales Invoice%s } }
                """;

        String withFormat = template.formatted(", format: \"SI{seq:07}\"");
        assertTrue(assertThrows(IntentValidationException.class, () -> IntentParser.parse(withFormat)).getIssues()
                                                                                                      .stream()
                                                                                                      .anyMatch(i -> i.contains("`format`")
                                                                                                              && i.contains(".numbers")),
                "number `format` must be rejected pointing at the .numbers artefact");

        String withScope = template.formatted(", scope: { company: Company }");
        assertTrue(assertThrows(IntentValidationException.class, () -> IntentParser.parse(withScope)).getIssues()
                                                                                                     .stream()
                                                                                                     .anyMatch(i -> i.contains("`scope`")
                                                                                                             && i.contains("per:")),
                "number `scope` must be rejected pointing at `per:`");

        String withResetOn = template.formatted(", resetOn: year");
        assertTrue(assertThrows(IntentValidationException.class, () -> IntentParser.parse(withResetOn)).getIssues()
                                                                                                       .stream()
                                                                                                       .anyMatch(i -> i.contains(
                                                                                                               "`resetOn`")
                                                                                                               && i.contains("continuous")),
                "number `resetOn` must be rejected - sequences are continuous");
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

    /**
     * A calculated action on a to-one RELATION binds onto the model. This is a typed-mapping test on
     * purpose: the typed mapping is Gson, which drops an unknown key in silence, so before the
     * attribute existed on {@link org.eclipse.dirigible.components.intent.model.RelationIntent} an
     * intent could author this and lose it with every pipeline step green.
     */
    @Test
    void calculatedActionOnAToOneRelationParses() {
        String yaml = """
                name: shop
                entities:
                  - name: Company
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                    relations:
                      - { name: Company, kind: manyToOne, to: Company, calculatedActionOnCreate: OrderCompanyAction }
                """;
        IntentModel model = IntentParser.parse(yaml);
        var relation = model.getEntities()
                            .get(1)
                            .getRelations()
                            .get(0);
        assertEquals("OrderCompanyAction", relation.getCalculatedActionOnCreate());
        assertTrue(relation.isCalculated());
    }

    @Test
    void calculatedActionOnACollectionRelationIsRejected() {
        String yaml = """
                name: shop
                entities:
                  - name: Company
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                    relations:
                      - { name: Companies, kind: oneToMany, to: Company, calculatedActionOnCreate: OrderCompanyAction }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("FK column to assign")),
                "expected a to-one-only issue, got: " + ex.getIssues());
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

    private static final String CONDITIONAL_DEPENDS_ON = """
            name: shop
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: priceLevel, type: integer }
              - name: Product
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: wholesalePrice, type: decimal }
                  - { name: retailPrice, type: decimal }
              - name: SalesOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: SalesOrderItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: price
                    type: decimal
                    dependsOn:
                      relation: Product
                      valueFrom:
                        by: SalesOrder.Customer.priceLevel
                        cases: { 1: wholesalePrice, 2: retailPrice }
                        default: retailPrice
                relations:
                  - { name: SalesOrder, kind: manyToOne, to: SalesOrder, composition: true, required: true }
                  - { name: Product, kind: manyToOne, to: Product }
            """;

    /**
     * The header-mediated trigger form (#6358): the line's discount defaults from a record the open
     * DOCUMENT points at (its customer), not from a relation of the line itself.
     */
    private static final String HEADER_MEDIATED_DEPENDS_ON = """
            name: shop
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: standardDiscount, type: decimal }
              - name: Product
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: SalesOrderItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: discount, type: decimal, dependsOn: { relation: SalesOrder.Customer, valueFrom: standardDiscount } }
                relations:
                  - { name: SalesOrder, kind: manyToOne, to: SalesOrder, composition: true, required: true }
                  - { name: Product, kind: manyToOne, to: Product }
            """;

    @Test
    void headerMediatedDependsOnParses() {
        IntentModel model = IntentParser.parse(HEADER_MEDIATED_DEPENDS_ON);
        var dependsOn = model.getEntities()
                             .get(3)
                             .getFields()
                             .get(1)
                             .getDependsOn();
        assertEquals("SalesOrder.Customer", dependsOn.getRelation());
        assertEquals("standardDiscount", dependsOn.getValueFrom());
    }

    @Test
    void headerMediatedDependsOnRequiresTheCompositionParentAsFirstSegment() {
        // Product is a plain to-one of the item, not the composition parent (the open document).
        String yaml = HEADER_MEDIATED_DEPENDS_ON.replace("relation: SalesOrder.Customer", "relation: Product.Customer");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("[Product] is not the composition parent relation of [SalesOrderItem]")),
                "expected a non-composition first segment issue, got: " + ex.getIssues());
    }

    @Test
    void headerMediatedDependsOnRejectsUnknownHeaderRelation() {
        String yaml = HEADER_MEDIATED_DEPENDS_ON.replace("relation: SalesOrder.Customer", "relation: SalesOrder.Supplier");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("[Supplier] is not a to-one relation of [SalesOrder]")),
                "expected an unknown header relation issue, got: " + ex.getIssues());
    }

    @Test
    void headerMediatedDependsOnValidatesValueFromAgainstTheHeaderRelationTarget() {
        String yaml = HEADER_MEDIATED_DEPENDS_ON.replace("valueFrom: standardDiscount", "valueFrom: nonsuchDiscount");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("valueFrom [nonsuchDiscount] is not a field or to-one relation of [Customer]")),
                "expected valueFrom to be resolved against the header relation's target, got: " + ex.getIssues());
    }

    @Test
    void headerMediatedDependsOnIsRejectedOnARelation() {
        // A header selection has no business filtering a line's own dropdown - fields only.
        String yaml = HEADER_MEDIATED_DEPENDS_ON.replace("- { name: Product, kind: manyToOne, to: Product }",
                "- { name: Product, kind: manyToOne, to: Product, dependsOn: { relation: SalesOrder.Customer, valueFrom: id } }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("is supported on a field (auto-populate), not on a relation")),
                "expected a fields-only issue, got: " + ex.getIssues());
    }

    /**
     * The conditional valueFrom form (#6358): the copied trigger-target property is picked by a
     * classifier - here the open document header's customer price level (a header-started by-path).
     */
    @Test
    void conditionalDependsOnParses() {
        IntentModel model = IntentParser.parse(CONDITIONAL_DEPENDS_ON);
        var dependsOn = model.getEntities()
                             .get(3)
                             .getFields()
                             .get(1)
                             .getDependsOn();
        assertEquals(null, dependsOn.getValueFrom(), "the conditional form has no simple valueFrom string");
        assertEquals("SalesOrder.Customer.priceLevel", dependsOn.getValueFromConditional()
                                                                .get("by"));
    }

    @Test
    void conditionalDependsOnRejectsBadShapesAndPaths() {
        // missing cases + a foreign key in the map + a dangling by-path property + a bad case property
        String yaml = CONDITIONAL_DEPENDS_ON.replace("cases: { 1: wholesalePrice, 2: retailPrice }", "cases: { 1: nonsuchPrice }")
                                            .replace("by: SalesOrder.Customer.priceLevel", "by: SalesOrder.Customer.nonsuchLevel");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("cases [nonsuchPrice]")),
                "expected a bad case-property issue, got: " + ex.getIssues());
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("[nonsuchLevel] is not a field or to-one relation of [Customer]")),
                "expected a bad by-path issue, got: " + ex.getIssues());

        String unknownKey = CONDITIONAL_DEPENDS_ON.replace("default: retailPrice", "fallback: retailPrice");
        IntentValidationException ex2 = assertThrows(IntentValidationException.class, () -> IntentParser.parse(unknownKey));
        assertTrue(ex2.getIssues()
                      .stream()
                      .anyMatch(i -> i.contains("got [fallback]")),
                "expected an unknown-key issue, got: " + ex2.getIssues());
    }

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
    void immutableWhenParsesAndValidates() {
        String yaml = """
                name: ledger
                entities:
                  - name: EntryStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: JournalEntry
                    immutableWhen: "Status == 2 || Status == 3"
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals("Status == 2 || Status == 3", model.getEntities()
                                                        .get(1)
                                                        .getImmutableWhen());

        String noStatus = """
                name: ledger
                entities:
                  - name: JournalEntry
                    immutableWhen: "Status == 2"
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(noStatus));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("requires a `function: EntityStatus` relation")),
                "expected a missing-status issue, got: " + ex.getIssues());

        String wrongRelation = """
                name: ledger
                entities:
                  - name: EntryStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: JournalEntry
                    immutableWhen: "State == 2"
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                """;
        ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(wrongRelation));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must reference the EntityStatus relation [Status]")),
                "expected a wrong-relation issue, got: " + ex.getIssues());
    }

    @Test
    void immutableInIsRejectedWithAMigrationMessage() {
        String yaml = """
                name: ledger
                entities:
                  - name: JournalEntry
                    immutableIn: [2]
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("renamed") && i.contains("immutableWhen")),
                "expected the immutableIn migration message, got: " + ex.getIssues());
    }

    @Test
    void immutableTrueIsAppendOnlyAndExcludesImmutableWhen() {
        String yaml = """
                name: ledger
                entities:
                  - name: InvoiceSnapshot
                    immutable: true
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: payload, type: text }
                """;
        assertEquals(Boolean.TRUE, IntentParser.parse(yaml)
                                               .getEntities()
                                               .get(0)
                                               .getImmutable());

        String both = """
                name: ledger
                entities:
                  - name: EntryStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: JournalEntry
                    immutable: true
                    immutableWhen: "Status == 2"
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(both));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("always-immutable subsumes")),
                "expected the mutual-exclusion issue, got: " + ex.getIssues());
    }

    @Test
    void checksParseAndValidate() {
        String yaml = """
                name: ledger
                entities:
                  - name: EntryStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: JournalEntry
                    checks:
                      - { kind: itemsSumEqual, over: [debit, credit], status: 2, message: "Must balance" }
                      - { kind: itemsMin, count: 1, status: 2, message: "Needs a line" }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                  - name: JournalEntryItem
                    checks:
                      - { kind: exactlyOne, fields: [debit, credit], message: "Debit or credit" }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                      - { name: credit, type: decimal }
                    relations:
                      - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals(2, model.getEntities()
                             .get(1)
                             .getChecks()
                             .size());

        // A document check without a status gate would forbid drafting - rejected.
        String noGate = yaml.replace(", status: 2, message: \"Must balance\"", ", message: \"Must balance\"");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(noGate));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("requires a `status` gate")),
                "expected a gate issue, got: " + ex.getIssues());
    }

    @Test
    void hierarchyAndLeafOnlyParse() {
        String yaml = """
                name: ledger
                entities:
                  - name: Account
                    hierarchy: Parent
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, required: true, length: 10 }
                    relations:
                      - { name: Parent, kind: manyToOne, to: Account }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                    relations:
                      - { name: Account, kind: manyToOne, to: Account, required: true, leafOnly: true }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals("Parent", model.getEntities()
                                    .get(0)
                                    .getHierarchy());
        assertTrue(model.getEntities()
                        .get(1)
                        .getRelations()
                        .get(0)
                        .isLeafOnly());
    }

    @Test
    void hierarchyMustNameAnOptionalSelfRelation() {
        String yaml = """
                name: ledger
                entities:
                  - name: Category
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Account
                    hierarchy: Category
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Category, kind: manyToOne, to: Category }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must target the entity itself")),
                "expected a self-relation issue, got: " + ex.getIssues());
    }

    @Test
    void leafOnlyRequiresAHierarchicalTarget() {
        String yaml = """
                name: ledger
                entities:
                  - name: Account
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Account, kind: manyToOne, to: Account, leafOnly: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("declares no hierarchy")),
                "expected a no-hierarchy issue, got: " + ex.getIssues());
    }

    @Test
    void postingRequiresGuardAndItemsAndKnownReferences() {
        String yaml = """
                name: ledger
                uses:
                  - { model: sales-invoices }
                entities:
                  - name: JournalEntry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, model: sales-invoices }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                    relations:
                      - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                postings:
                  - name: broken
                    event: { onTransition: SalesInvoice, model: sales-invoices, when: "whenever" }
                    creates: JournalEntry
                    backReference: SalesInvoice
                    items:
                      - { nonsuch: "Net" }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("event requires `when:")),
                "expected a when-guard issue, got: " + ex.getIssues());
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("item [nonsuch]")),
                "expected an unknown-item-field issue, got: " + ex.getIssues());
    }

    /**
     * A source with no status lifecycle (a booked payment) posts on its INSERT: {@code onCreate} is a
     * valid trigger and needs no {@code when} status guard.
     */
    @Test
    void postingOnCreateAcceptsALifecycleLessSource() {
        String yaml = """
                name: ledger
                entities:
                  - name: Payment
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal }
                  - name: JournalEntry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Payment, kind: manyToOne, to: Payment }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                    relations:
                      - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                postings:
                  - name: paymentPosting
                    event: { onCreate: Payment }
                    creates: JournalEntry
                    backReference: Payment
                    items:
                      - { debit: "Amount" }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals(1, model.getPostings()
                             .size());
    }

    /**
     * A payment posting whose account column is chosen by a source classifier - #6534. The single
     * {@code items} row is supplied per test.
     */
    private static String conditionalRulePosting(String itemRow) {
        return """
                name: ledger
                entities:
                  - name: Account
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                  - name: PaymentMethodType
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: PostingRule
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: documentType, type: string }
                    relations:
                      - { name: BankAccount, kind: manyToOne, to: Account }
                      - { name: CashAccount, kind: manyToOne, to: Account }
                      - { name: SuspenseAccount, kind: manyToOne, to: Account }
                  - name: Payment
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: Method, kind: manyToOne, to: PaymentMethodType, required: true }
                  - name: JournalEntry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Payment, kind: manyToOne, to: Payment }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                      - { name: Account, kind: manyToOne, to: Account, required: true }
                postings:
                  - name: paymentPosting
                    event: { onCreate: Payment }
                    creates: JournalEntry
                    backReference: Payment
                    rule: { entity: PostingRule, match: { documentType: "Payment" } }
                    items:
                      - %s
                """.formatted(itemRow);
    }

    @Test
    void conditionalRuleColumnParses() {
        IntentParser.parse(conditionalRulePosting(
                "{ Account: \"rule(by: Method, cases: { 1: BankAccount, 2: CashAccount }, default: SuspenseAccount)\", debit: \"Amount\" }"));
    }

    @Test
    void conditionalRuleUnknownCaseColumnIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(
                conditionalRulePosting("{ Account: \"rule(by: Method, cases: { 1: Nonsuch })\", debit: \"Amount\" }")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("case column [Nonsuch] is not a field or to-one relation of [PostingRule]")),
                "expected an unknown case-column issue, got: " + ex.getIssues());
    }

    @Test
    void conditionalRuleWithAWhenGuardIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(conditionalRulePosting(
                "{ Account: \"rule(by: Method, cases: { 1: BankAccount })\", debit: \"Amount\", when: \"Amount == 0\" }")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("combines a conditional rule(by: ...) with a when: guard")),
                "expected a conditional+when issue, got: " + ex.getIssues());
    }

    @Test
    void conditionalRuleNonNumericCaseKeyIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(
                conditionalRulePosting("{ Account: \"rule(by: Method, cases: { bank: BankAccount })\", debit: \"Amount\" }")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("case key [bank] must be a number")),
                "expected a non-numeric case-key issue, got: " + ex.getIssues());
    }

    @Test
    void conditionalRuleUnknownClassifierIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(
                conditionalRulePosting("{ Account: \"rule(by: Nonsuch, cases: { 1: BankAccount })\", debit: \"Amount\" }")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("rule(by: Nonsuch) is not a field or to-one relation of the source [Payment]")),
                "expected an unknown-classifier issue, got: " + ex.getIssues());
    }

    /** The event declares exactly one trigger - onTransition XOR onCreate. */
    @Test
    void postingEventDeclaresExactlyOneTrigger() {
        String yaml = """
                name: ledger
                entities:
                  - name: Payment
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: JournalEntry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Payment, kind: manyToOne, to: Payment }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                    relations:
                      - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                postings:
                  - name: doubled
                    event: { onCreate: Payment, onTransition: Payment, when: "Status == 2" }
                    creates: JournalEntry
                    backReference: Payment
                    items:
                      - { debit: "Amount" }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("exactly one trigger")),
                "expected an exactly-one-trigger issue, got: " + ex.getIssues());
    }

    /** An onCreate guard stays optional, but a malformed one is rejected, not silently dropped. */
    @Test
    void postingOnCreateRejectsAMalformedGuard() {
        String yaml = """
                name: ledger
                entities:
                  - name: Payment
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: JournalEntry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Payment, kind: manyToOne, to: Payment }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                    relations:
                      - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                postings:
                  - name: paymentPosting
                    event: { onCreate: Payment, when: "whenever" }
                    creates: JournalEntry
                    backReference: Payment
                    items:
                      - { debit: "Amount" }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must be `<Property> == <numeric value>`")),
                "expected a malformed-guard issue, got: " + ex.getIssues());
    }

    @Test
    void whereStaticOptionFilterParses() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: HomeCity, kind: manyToOne, to: City, where: { Country: 1 } }
                """;
        IntentModel model = IntentParser.parse(yaml);
        var where = model.getEntities()
                         .get(2)
                         .getRelations()
                         .get(1)
                         .getWhere();
        assertEquals(1, where.size());
        assertEquals(1L, where.get("Country"));
    }

    @Test
    void whereWithUnknownTargetPropertyIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: HomeCity, kind: manyToOne, to: City, where: { region: 1 } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("where [region] is not a field or to-one relation")),
                "expected an unknown-property issue, got: " + ex.getIssues());
    }

    @Test
    void whereWithMultipleConditionsIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: HomeCity, kind: manyToOne, to: City, where: { Country: 1, name: Sofia } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("where must be a single")),
                "expected a single-pair issue, got: " + ex.getIssues());
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

    /** A multilingual UoM setting with a Bulgarian translation seed. */
    private static final String MULTILINGUAL_HEAD = """
            name: uoms
            languages: [en, bg]
            entities:
              - name: UoM
                kind: setting
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
                  - { name: numerator, type: decimal }
            seeds:
              - name: uoms-bg
                entity: UoM
                language: bg
                rows:
                  - { id: 1, name: "Килограм" }
            """;

    @Test
    void multilingualEntityAndLanguageSeedParse() {
        IntentModel model = IntentParser.parse(MULTILINGUAL_HEAD);
        assertTrue(model.getEntities()
                        .get(0)
                        .isMultilingual());
        assertEquals("bg", model.getSeeds()
                                .get(0)
                                .getLanguage());
        assertEquals(java.util.List.of("en", "bg"), model.getLanguages());
    }

    @Test
    void languageSeedOnNonMultilingualEntityIsRejected() {
        String yaml = MULTILINGUAL_HEAD.replace("    multilingual: true\n", "");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("is not multilingual - add `multilingual: true`")),
                "expected a not-multilingual issue, got: " + ex.getIssues());
    }

    @Test
    void languageSeedWithNonTranslatableRowKeyIsRejected() {
        String yaml = MULTILINGUAL_HEAD.replace("name: \"Килограм\"", "name: \"Килограм\", numerator: 5");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("[numerator] which is not the id or a translatable")),
                "expected a non-translatable row-key issue, got: " + ex.getIssues());
    }

    @Test
    void malformedLanguageCodesAreRejected() {
        String yaml = MULTILINGUAL_HEAD.replace("languages: [en, bg]", "languages: [en, Bulgarian]")
                                       .replace("language: bg", "language: BG");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("languages entry [Bulgarian]")),
                "expected a languages-entry issue, got: " + ex.getIssues());
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("language [BG] must be a short lowercase language code")),
                "expected a seed-language issue, got: " + ex.getIssues());
    }

    @Test
    void fileSeedParsesAndRootLevelOrAbsolutePathsAreRejected() {
        String head = """
                name: countries
                entities:
                  - name: Country
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, required: true }
                seeds:
                  - name: countries
                    entity: Country
                """;
        // A subfolder path with no inline rows is the valid shape for a large authored data set.
        IntentModel model = IntentParser.parse(head.stripTrailing() + "\n    file: data/countries.csv\n");
        assertEquals("data/countries.csv", model.getSeeds()
                                                .get(0)
                                                .getFile());

        // A root-level file would be scrubbed by the intent regeneration - rejected with guidance.
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(head.stripTrailing() + "\n    file: countries.csv\n"));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must live in a subfolder")),
                "expected a subfolder issue, got: " + ex.getIssues());

        // Absolute / escaping paths are rejected.
        ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(head.stripTrailing() + "\n    file: /countries/data.csv\n"));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must be a project-relative path")),
                "expected a relative-path issue, got: " + ex.getIssues());

        // file and inline rows are mutually exclusive.
        ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(head.stripTrailing() + """

                    file: data/countries.csv
                    rows:
                      - { id: 1, name: Afghanistan }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("declares both `file` and inline `rows`")),
                "expected a mutual-exclusion issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnOnEntityStatusRelationIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing()
                + """

                              - { name: City, kind: manyToOne, to: City, function: EntityStatus, dependsOn: { relation: Country, filterBy: country } }
                        """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("EntityStatus (a read-only badge) so it cannot declare dependsOn")),
                "expected an EntityStatus-dependent issue, got: " + ex.getIssues());
    }

    private static final String WIDGET_HEAD = """
            name: sales
            entities:
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: issuedOn, type: date }
                  - { name: total, type: decimal }
            reports:
              - name: RevenueByMonth
                source: Invoice
                dimensions: ["month(issuedOn)"]
                measures: ["sum(total)"]
                widget:
                  value: "sum(total)"
                  at: { "month(issuedOn)": now }
            """;

    @Test
    void reportWidgetParses() {
        IntentModel model = IntentParser.parse(WIDGET_HEAD);
        assertEquals("sum(total)", model.getReports()
                                        .get(0)
                                        .getWidget()
                                        .getValue());
        assertEquals("now", model.getReports()
                                 .get(0)
                                 .getWidget()
                                 .getAt()
                                 .get("month(issuedOn)"));
    }

    @Test
    void widgetValueMustNameADeclaredMeasure() {
        String yaml = WIDGET_HEAD.replace("value: \"sum(total)\"", "value: \"avg(total)\"");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("does not name a declared measure")),
                "expected an unknown-measure issue, got: " + ex.getIssues());
    }

    @Test
    void widgetPinMustNameADeclaredDimension() {
        String yaml = WIDGET_HEAD.replace("at: { \"month(issuedOn)\": now }", "at: { \"year(issuedOn)\": now }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("pins unknown dimension [year(issuedOn)]")),
                "expected an unknown-dimension issue, got: " + ex.getIssues());
    }

    @Test
    void widgetUnknownKindIsRejected() {
        String yaml = WIDGET_HEAD.replace("value: \"sum(total)\"", "kind: chart");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("unknown kind [chart]")),
                "expected an unknown-kind issue, got: " + ex.getIssues());
    }

    @Test
    void widgetLimitIsListOnly() {
        String yaml = WIDGET_HEAD.stripTrailing() + "\n      limit: 5\n";
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must not declare `limit`")),
                "expected a limit-misuse issue, got: " + ex.getIssues());
    }

    @Test
    void widgetValueOnCountKindIsRejected() {
        String yaml = WIDGET_HEAD.replace("widget:", "widget:\n      kind: count");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must not declare `value`")),
                "expected a value-on-count issue, got: " + ex.getIssues());
    }

    private static final String CUSTOM_WIDGET_HEAD = """
            name: sales
            entities:
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
            widgets:
              - name: SystemHealth
                kind: kpi
                url: /services/js/sales/custom/health.js
                label: System Health
                icon: activity
              - name: SalesFunnel
                kind: page
                url: /services/web/sales/custom/funnel/index.html
            """;

    @Test
    void customWidgetsParse() {
        IntentModel model = IntentParser.parse(CUSTOM_WIDGET_HEAD);
        assertEquals(2, model.getWidgets()
                             .size());
        assertEquals("kpi", model.getWidgets()
                                 .get(0)
                                 .getKind());
        assertEquals("/services/web/sales/custom/funnel/index.html", model.getWidgets()
                                                                          .get(1)
                                                                          .getUrl());
    }

    @Test
    void customWidgetWithoutUrlIsRejected() {
        String yaml = CUSTOM_WIDGET_HEAD.replace("    url: /services/js/sales/custom/health.js\n", "");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("widget [SystemHealth] has no url")),
                "expected a missing-url issue, got: " + ex.getIssues());
    }

    @Test
    void customWidgetWithCrossOriginUrlIsRejected() {
        String yaml = CUSTOM_WIDGET_HEAD.replace("/services/js/sales/custom/health.js", "https://example.com/kpi");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("url must be a same-origin path")),
                "expected a same-origin issue, got: " + ex.getIssues());
    }

    @Test
    void customWidgetWithUnknownKindIsRejected() {
        String yaml = CUSTOM_WIDGET_HEAD.replace("kind: kpi", "kind: chart");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("has unknown kind [chart]")),
                "expected an unknown-kind issue, got: " + ex.getIssues());
    }

    private static final String SCHEDULE_GEN_HEAD = """
            name: hr
            entities:
              - name: Employee
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: status, type: string }
              - name: EmployeeTimesheet
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee }
            schedules:
              - name: monthly-timesheets
                cron: "0 0 1 1 * ?"
                entity: Employee
            """;

    @Test
    void scheduleGenerateParsesWithoutIssues() {
        String yaml = SCHEDULE_GEN_HEAD + """
                    generate:
                      to: EmployeeTimesheet
                      map:
                        Employee: id
                """;
        // A well-formed scheduled generation validates cleanly (no exception).
        IntentParser.parse(yaml);
    }

    @Test
    void scheduleWithBothNotifyAndGenerateIsRejected() {
        String yaml = SCHEDULE_GEN_HEAD + """
                    notify:
                      to: status
                      subject: "x"
                      body: "y"
                    generate:
                      to: EmployeeTimesheet
                      map:
                        Employee: id
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("has both notify and generate")),
                "expected a both-actions issue, got: " + ex.getIssues());
    }

    @Test
    void scheduleWithNoActionIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(SCHEDULE_GEN_HEAD));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("has no action")),
                "expected a no-action issue, got: " + ex.getIssues());
    }

    @Test
    void scheduleGenerateWithUnknownTargetIsRejected() {
        String yaml = SCHEDULE_GEN_HEAD + """
                    generate:
                      to: Nonexistent
                      map:
                        Employee: id
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("generate to references unknown entity [Nonexistent]")),
                "expected an unknown-target issue, got: " + ex.getIssues());
    }

    @Test
    void scheduleGenerateWithItemsIsRejected() {
        String yaml = SCHEDULE_GEN_HEAD + """
                    generate:
                      to: EmployeeTimesheet
                      map:
                        Employee: id
                      items:
                        from: Employee
                        to: EmployeeTimesheet
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("item cloning is not supported for a scheduled generation")),
                "expected an items-not-supported issue, got: " + ex.getIssues());
    }

    @Test
    void scheduleGenerateWithBadMapSourceIsRejected() {
        String yaml = SCHEDULE_GEN_HEAD + """
                    generate:
                      to: EmployeeTimesheet
                      map:
                        Employee: nonexistentField
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("generate map source [nonexistentField] is not a field or to-one relation of [Employee]")),
                "expected a bad-map-source issue, got: " + ex.getIssues());
    }

    /**
     * A consumer model that owns the created rows and reaches a source entity ({@code Project}) in a
     * declared {@code uses:} model. The generate block is appended per test.
     */
    private static final String CROSS_SCHEDULE_HEAD = """
            name: timesheets
            uses:
              - { model: projects }
            entities:
              - name: ProjectTimesheet
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: period, type: date }
              - name: EmployeeTimesheet
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: ProjectTimesheet, kind: manyToOne, to: ProjectTimesheet }
            schedules:
              - name: monthly-project-timesheets
                cron: "0 0 2 1 * ?"
                entity: Project
                model: projects
                where:
                  - { field: Status, op: eq, value: 2 }
            """;

    @Test
    void crossModelScheduleSourceParsesWhenModelIsDeclared() {
        // The source Project is not a local entity, but its model is a declared uses: alias - so the
        // local entity check is skipped and its where/map fields validate at generation time, not here.
        String yaml = CROSS_SCHEDULE_HEAD + """
                    generate:
                      to: ProjectTimesheet
                      map:
                        Period: now
                      children:
                        - to: EmployeeTimesheet
                          parent: ProjectTimesheet
                          forEach:
                            entity: EmployeeProjectAssignment
                            model: projects
                            match: { Project: id }
                          map: { Employee: Employee }
                """;
        IntentParser.parse(yaml);
    }

    @Test
    void crossModelScheduleSourceToUndeclaredModelIsRejected() {
        String yaml = """
                name: timesheets
                entities:
                  - name: ProjectTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: period, type: date }
                schedules:
                  - name: monthly-project-timesheets
                    cron: "0 0 2 1 * ?"
                    entity: Project
                    model: projects
                    generate:
                      to: ProjectTimesheet
                      map: { Period: now }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("source model [projects] is not a declared uses: alias")),
                "expected an undeclared-source-model issue, got: " + ex.getIssues());
    }

    @Test
    void crossModelScheduleSourceWithNotifyIsRejected() {
        String yaml = """
                name: timesheets
                uses:
                  - { model: projects }
                entities:
                  - name: ProjectTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                schedules:
                  - name: nudge
                    cron: "0 0 2 1 * ?"
                    entity: Project
                    model: projects
                    notify:
                      to: contactEmail
                      subject: "x"
                      body: "y"
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("notify needs the source's relation metadata")),
                "expected a cross-model-notify-unsupported issue, got: " + ex.getIssues());
    }

    @Test
    void crossModelForEachToUndeclaredModelIsRejected() {
        String yaml = CROSS_SCHEDULE_HEAD + """
                    generate:
                      to: ProjectTimesheet
                      map: { Period: now }
                      children:
                        - to: EmployeeTimesheet
                          parent: ProjectTimesheet
                          forEach:
                            entity: EmployeeProjectAssignment
                            model: staffing
                            match: { Project: id }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("forEach model [staffing] is not a declared uses: alias")),
                "expected an undeclared-forEach-model issue, got: " + ex.getIssues());
    }

    /** A complete personalized model built line by line - no text-block margin surprises. */
    private static String personalYaml(String employeeExtra, String requestFields, String requestRelations) {
        return "name: hr\n" //
                + "entities:\n" //
                + "  - name: Employee\n" //
                + employeeExtra //
                + "    fields:\n" //
                + "      - { name: id, type: integer, primaryKey: true, generated: true }\n" //
                + "      - { name: name, type: string, required: true, length: 200 }\n" //
                + "      - { name: email, type: string, required: true, unique: true, length: 320 }\n" //
                + "  - name: VacationRequest\n" //
                + "    fields:\n" //
                + "      - { name: id, type: integer, primaryKey: true, generated: true }\n" //
                + requestFields //
                + requestRelations;
    }

    private static final String OWNER_RELATION =
            "    relations:\n" + "      - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }\n";

    @Test
    void identityAndPersonalParseWithoutIssues() {
        IntentParser.parse(
                personalYaml("    identity: email\n", "      - { name: dailyRate, type: decimal, sensitive: true }\n", OWNER_RELATION));
    }

    /**
     * A keyed aggregate over a sensitive source field materialises that hidden figure into ANOTHER
     * entity, so the target field is auto-marked sensitive when the target itself is personal-surfaced
     * - the same leak class the rollup / {@code aggregate: true} propagation closes, one entity further
     * out (the {@code aggregates:} keyword arrived after that pass and was not covered by it).
     */
    private static String aggregateSensitiveYaml(String targetRelations) {
        return "name: hr\n" //
                + "entities:\n" //
                + "  - name: Employee\n" //
                + "    identity: email\n" //
                + "    fields:\n" //
                + "      - { name: id, type: integer, primaryKey: true, generated: true }\n" //
                + "      - { name: email, type: string, required: true, unique: true, length: 320 }\n" //
                + "  - name: Payout\n" //
                + "    fields:\n" //
                + "      - { name: id, type: integer, primaryKey: true, generated: true }\n" //
                + "      - { name: amount, type: decimal, sensitive: true }\n" //
                + "    relations:\n" //
                + "      - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }\n" //
                + "  - name: PayoutTotal\n" //
                + "    fields:\n" //
                + "      - { name: id, type: integer, primaryKey: true, generated: true }\n" //
                + "      - { name: total, type: decimal }\n" //
                + targetRelations //
                + "aggregates:\n" //
                + "  - name: payoutTotal\n" //
                + "    of: Payout\n" //
                + "    op: sum\n" //
                + "    sum: amount\n" //
                + "    by: [Employee]\n" //
                + "    into: PayoutTotal\n" //
                + "    field: total\n";
    }

    private static boolean isSensitive(IntentModel model, String entity, String field) {
        return model.getEntities()
                    .stream()
                    .filter(e -> entity.equals(e.getName()))
                    .flatMap(e -> e.getFields()
                                   .stream())
                    .filter(f -> field.equals(f.getName()))
                    .anyMatch(org.eclipse.dirigible.components.intent.model.FieldIntent::isSensitive);
    }

    @Test
    void aggregateTargetOfASensitiveSourceIsScrubbedOnAPersonalTarget() {
        IntentModel model = IntentParser.parse(aggregateSensitiveYaml(
                "    relations:\n" + "      - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }\n"));
        assertTrue(isSensitive(model, "PayoutTotal", "total"),
                "an aggregates: target summing a sensitive source field must be auto-marked sensitive when the target is personal-surfaced");
    }

    @Test
    void aggregateTargetWithoutAPersonalSurfaceKeepsTheAuthoredVisibility() {
        IntentModel model = IntentParser.parse(
                aggregateSensitiveYaml("    relations:\n" + "      - { name: Employee, kind: manyToOne, to: Employee }\n"));
        assertTrue(!isSensitive(model, "PayoutTotal", "total"),
                "a target with no personal surface has nothing to leak - the authored visibility stands");
    }

    @Test
    void identityMustNameAnOwnStringField() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(personalYaml("    identity: mail\n", "", OWNER_RELATION)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("does not name a field")),
                "expected an unknown-field issue, got: " + ex.getIssues());

        IntentValidationException ex2 = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(personalYaml("    identity: id\n", "", OWNER_RELATION)));
        assertTrue(ex2.getIssues()
                      .stream()
                      .anyMatch(i -> i.contains("must be a string field")),
                "expected a non-string issue, got: " + ex2.getIssues());
    }

    @Test
    void personalRequiresAnIdentityOnItsTarget() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(personalYaml("", "", OWNER_RELATION)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("declares no identity")),
                "expected a no-identity issue, got: " + ex.getIssues());
    }

    @Test
    void sensitiveIsRejectedOnThePrimaryKeyAndTheIdentityField() {
        String pk = personalYaml("    identity: email\n", "", OWNER_RELATION).replace("primaryKey: true, generated: true }",
                "primaryKey: true, generated: true, sensitive: true }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(pk));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("primary key so it cannot be sensitive")),
                "expected a pk-sensitive issue, got: " + ex.getIssues());

        String idf = personalYaml("    identity: email\n", "", OWNER_RELATION).replace("unique: true, length: 320 }",
                "unique: true, length: 320, sensitive: true }");
        IntentValidationException ex2 = assertThrows(IntentValidationException.class, () -> IntentParser.parse(idf));
        assertTrue(ex2.getIssues()
                      .stream()
                      .anyMatch(i -> i.contains("identity field so it cannot be sensitive")),
                "expected an identity-sensitive issue, got: " + ex2.getIssues());
    }

    @Test
    void onlyOnePersonalRelationIsAllowed() {
        String relations = "    relations:\n" + "      - { name: Employee, kind: manyToOne, to: Employee, personal: true }\n"
                + "      - { name: Substitute, kind: manyToOne, to: Employee, personal: true }\n";
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(personalYaml("    identity: email\n", "", relations)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("exactly one owner is allowed")),
                "expected a single-owner issue, got: " + ex.getIssues());
    }

    @Test
    void labelParsesAndComposesButRejectsBadTokens() {
        // A valid label with a literal, a field, a formatted field, and a one-hop relation token.
        IntentParser.parse(personalYaml("    identity: email\n", "      - { name: fromDate, type: date }\n",
                "    label: \"{fromDate|yyyy MMMM} - {Employee.name}\"\n" + OWNER_RELATION));

        // An unknown own-field token is rejected.
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(personalYaml("    identity: email\n", "", "    label: \"{missing}\"\n" + OWNER_RELATION)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("does not name a field")),
                "expected an unknown-token issue, got: " + ex.getIssues());

        // A label next to an authored name field is redundant.
        IntentValidationException ex2 =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(personalYaml("    identity: email\n",
                        "      - { name: name, type: string, length: 100 }\n", "    label: \"{name}\"\n" + OWNER_RELATION)));
        assertTrue(ex2.getIssues()
                      .stream()
                      .anyMatch(i -> i.contains("redundant")),
                "expected a redundant-label issue, got: " + ex2.getIssues());

        // A sensitive field must never leak into the label.
        IntentValidationException ex3 =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(personalYaml("    identity: email\n",
                        "      - { name: rate, type: decimal, sensitive: true }\n", "    label: \"{rate}\"\n" + OWNER_RELATION)));
        assertTrue(ex3.getIssues()
                      .stream()
                      .anyMatch(i -> i.contains("sensitive")),
                "expected a sensitive-token issue, got: " + ex3.getIssues());

        // Two hops are rejected with the compose hint.
        IntentValidationException ex4 = assertThrows(IntentValidationException.class, () -> IntentParser.parse(
                personalYaml("    identity: email\n", "", "    label: \"{Employee.manager.name}\"\n" + OWNER_RELATION)));
        assertTrue(ex4.getIssues()
                      .stream()
                      .anyMatch(i -> i.contains("deeper than one relation hop")),
                "expected a depth issue, got: " + ex4.getIssues());
    }

    @Test
    void scheduleGenerateChildrenValidate() {
        String head = "name: hr\n" //
                + "entities:\n" //
                + "  - name: Person\n" //
                + "    fields:\n" //
                + "      - { name: id, type: integer, primaryKey: true, generated: true }\n" //
                + "      - { name: name, type: string, required: true, length: 200 }\n" //
                + "  - name: Claim\n" //
                + "    fields:\n" //
                + "      - { name: id, type: integer, primaryKey: true, generated: true }\n" //
                + "  - name: ClaimLine\n" //
                + "    fields:\n" //
                + "      - { name: id, type: integer, primaryKey: true, generated: true }\n" //
                + "      - { name: day, type: date }\n" //
                + "schedules:\n" //
                + "  - name: monthly\n" //
                + "    cron: \"0 0 4 1 * *\"\n" //
                + "    entity: Person\n" //
                + "    generate:\n" //
                + "      to: Claim\n" //
                + "      map: { Person: id }\n" //
                + "      children:\n";
        // A well-formed days child validates cleanly.
        IntentParser.parse(head //
                + "        - to: ClaimLine\n" //
                + "          parent: Claim\n" //
                + "          forEach: { days: workingDays }\n" //
                + "          dayField: day\n");
        // days without a dayField is rejected.
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(head //
                + "        - to: ClaimLine\n" //
                + "          parent: Claim\n" //
                + "          forEach: { days: workingDays }\n"));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("no dayField")),
                "expected a dayField issue, got: " + ex.getIssues());
        // an entity forEach without match is rejected.
        IntentValidationException ex2 = assertThrows(IntentValidationException.class, () -> IntentParser.parse(head //
                + "        - to: ClaimLine\n" //
                + "          parent: Claim\n" //
                + "          forEach: { entity: Person }\n"));
        assertTrue(ex2.getIssues()
                      .stream()
                      .anyMatch(i -> i.contains("requires a match")),
                "expected a match issue, got: " + ex2.getIssues());
    }

    private static final String CHAT_HEAD = """
            name: services
            entities:
              - name: Case
                function: Document
                documentItemsLayout: chat
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string }
              - name: CaseMessage
                function: DocumentItem
            """;

    @Test
    void chatDocumentWithABodyFieldAndAuditParses() {
        String yaml = CHAT_HEAD + """
                    audit: true
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: body, type: text, messageBody: true }
                    relations:
                      - { name: Case, kind: manyToOne, to: Case, composition: true, required: true }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals("chat", model.getEntities()
                                  .get(0)
                                  .getDocumentItemsLayout());
    }

    @Test
    void chatDocumentRejectsUnknownLayoutAndMissingBodyOrAudit() {
        // Unknown layout value.
        String badLayout = CHAT_HEAD.replace("documentItemsLayout: chat", "documentItemsLayout: timeline") + """
                    audit: true
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: body, type: text, messageBody: true }
                    relations:
                      - { name: Case, kind: manyToOne, to: Case, composition: true, required: true }
                """;
        assertTrue(assertThrows(IntentValidationException.class, () -> IntentParser.parse(badLayout)).getIssues()
                                                                                                     .stream()
                                                                                                     .anyMatch(i -> i.contains(
                                                                                                             "unknown documentItemsLayout")));

        // No messageBody field + no audit on the items child.
        String noBody = CHAT_HEAD + """
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: body, type: text }
                    relations:
                      - { name: Case, kind: manyToOne, to: Case, composition: true, required: true }
                """;
        List<String> issues = assertThrows(IntentValidationException.class, () -> IntentParser.parse(noBody)).getIssues();
        assertTrue(issues.stream()
                         .anyMatch(i -> i.contains("messageBody")),
                "expected a messageBody issue, got: " + issues);
        assertTrue(issues.stream()
                         .anyMatch(i -> i.contains("audit: true")),
                "expected an audit issue, got: " + issues);
    }

    @Test
    void chatDocumentRejectsAnItemsChildThatIsAlsoACalendar() {
        // Both render the line items (#6482), so declaring both leaves it undecidable which pane wins.
        String yaml = CHAT_HEAD + """
                    audit: true
                    view: calendar
                    calendar: { start: sentOn }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: body, type: text, messageBody: true }
                      - { name: sentOn, type: date }
                    relations:
                      - { name: Case, kind: manyToOne, to: Case, composition: true, required: true }
                """;
        List<String> issues = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml)).getIssues();
        assertTrue(issues.stream()
                         .anyMatch(i -> i.contains("both render the line items")),
                "expected a chat-vs-calendar issue, got: " + issues);
    }

    /**
     * A guard's non-blocking outcomes each need their own companion key, and a companion belonging to
     * another outcome is an authoring mistake worth failing on: the write would look guarded and do
     * nothing.
     */
    private static String guardYaml(String guardBody) {
        return "name: sales\n" //
                + "entities:\n" //
                + "  - name: Customer\n" //
                + "    fields:\n" //
                + "      - { name: id, type: integer, primaryKey: true, generated: true }\n" //
                + "  - name: Order\n" //
                + "    fields:\n" //
                + "      - { name: id,           type: integer, primaryKey: true, generated: true }\n" //
                + "      - { name: amount,       type: decimal }\n" //
                + "      - { name: withinCredit, type: boolean }\n" //
                + "      - { name: note,         type: string, length: 40 }\n" //
                + "    relations:\n" //
                + "      - { name: Customer, kind: manyToOne, to: Customer }\n" //
                + "    checks:\n" //
                + "      - kind: guard\n" //
                + "        aggregate: exposure\n" //
                + "        minimum: 0\n" //
                + "        message: Over the limit\n" //
                + guardBody //
                + "  - name: Exposure\n" //
                + "    fields:\n" //
                + "      - { name: id,    type: integer, primaryKey: true, generated: true }\n" //
                + "      - { name: total, type: decimal }\n" //
                + "    relations:\n" //
                + "      - { name: Customer, kind: manyToOne, to: Customer }\n" //
                + "aggregates:\n" //
                + "  - name: exposure\n" //
                + "    of: Order\n" //
                + "    op: sum\n" //
                + "    sum: amount\n" //
                + "    by: [Customer]\n" //
                + "    into: Exposure\n" //
                + "    field: total\n";
    }

    private static List<String> guardIssues(String guardBody) {
        return assertThrows(IntentValidationException.class, () -> IntentParser.parse(guardYaml(guardBody))).getIssues();
    }

    @Test
    void guardOutcomeTaskRequiresABooleanMarker() {
        // A well-formed task guard parses.
        IntentParser.parse(guardYaml("        outcome: task\n" + "        marker: withinCredit\n"));

        assertTrue(guardIssues("        outcome: task\n").stream()
                                                         .anyMatch(i -> i.contains("requires `marker`")),
                "outcome: task without a marker must be rejected");
        assertTrue(guardIssues("        outcome: task\n" + "        marker: nope\n").stream()
                                                                                    .anyMatch(i -> i.contains("does not name a field")),
                "an unknown marker field must be rejected");
        assertTrue(guardIssues("        outcome: task\n" + "        marker: note\n").stream()
                                                                                    .anyMatch(i -> i.contains("must be a boolean field")),
                "a non-boolean marker must be rejected");
    }

    @Test
    void guardOutcomeRejectRequiresAStatusIdAndAnEntityStatusRelation() {
        // Order declares no EntityStatus relation, so reject has nowhere to write.
        assertTrue(guardIssues("        outcome: reject\n" + "        setStatus: 3\n").stream()
                                                                                      .anyMatch(i -> i.contains(
                                                                                              "requires a `function: EntityStatus` relation")),
                "reject without an EntityStatus relation must be rejected");
        assertTrue(guardIssues("        outcome: reject\n").stream()
                                                           .anyMatch(i -> i.contains("requires `setStatus`")),
                "reject without setStatus must be rejected");
    }

    @Test
    void guardRejectsAnUnknownOutcomeAndAMisplacedCompanionKey() {
        assertTrue(guardIssues("        outcome: whenever\n").stream()
                                                             .anyMatch(i -> i.contains("unknown `outcome`")),
                "an unknown outcome must be rejected");
        assertTrue(guardIssues("        marker: withinCredit\n").stream()
                                                                .anyMatch(i -> i.contains("marker` but its outcome is [block]")),
                "a marker on the default block outcome must be rejected");
        assertTrue(guardIssues("        outcome: task\n" + "        marker: withinCredit\n" + "        setStatus: 3\n").stream()
                                                                                                                       .anyMatch(
                                                                                                                               i -> i.contains(
                                                                                                                                       "setStatus` but its outcome is [task]")),
                "a setStatus on a task outcome must be rejected");
    }

    private static final String FIELD_PATTERN = """
            name: banking
            entities:
              - name: Account
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: iban, type: string, length: 34, pattern: '^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$' }
                  - { name: balance, type: decimal }
            """;

    /** A field's input-format regex (#6336). */
    @Test
    void fieldPatternParses() {
        IntentModel model = IntentParser.parse(FIELD_PATTERN);
        assertEquals("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$", model.getEntities()
                                                               .get(0)
                                                               .getFields()
                                                               .get(1)
                                                               .getPattern());
    }

    @Test
    void fieldPatternIsRejectedOnANonStringField() {
        // On a numeric property widgetPattern is the DISPLAY format, so a regex there would corrupt it.
        String yaml =
                FIELD_PATTERN.replace("- { name: balance, type: decimal }", "- { name: balance, type: decimal, pattern: '^[0-9]+$' }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("`pattern` applies to a string/text field")),
                "expected a type restriction issue, got: " + ex.getIssues());
    }

    @Test
    void fieldPatternIsRejectedWhenItIsNotAValidRegex() {
        String yaml = FIELD_PATTERN.replace("'^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$'", "'^[A-Z(unclosed'");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("`pattern` is not a valid regular expression")),
                "expected an invalid-regex issue, got: " + ex.getIssues());
    }

    private static final String FIELD_FORMAT = """
            name: crm
            entities:
              - name: Contact
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string, length: 320, format: email }
                  - { name: age, type: integer }
            """;

    /** A field's named `format` (#6463) - a preset over `pattern`. */
    @Test
    void fieldFormatParses() {
        IntentModel model = IntentParser.parse(FIELD_FORMAT);
        assertEquals("email", model.getEntities()
                                   .get(0)
                                   .getFields()
                                   .get(1)
                                   .getFormat());
    }

    @Test
    void unknownFieldFormatIsRejected() {
        String yaml = FIELD_FORMAT.replace("format: email", "format: iban");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("unknown `format` [iban]")),
                "expected an unknown-format issue, got: " + ex.getIssues());
    }

    @Test
    void fieldFormatIsRejectedOnANonStringField() {
        String yaml = FIELD_FORMAT.replace("- { name: age, type: integer }", "- { name: age, type: integer, format: email }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("`format` applies to a string field")),
                "expected a type restriction issue, got: " + ex.getIssues());
    }

    @Test
    void declaringBothFormatAndPatternIsRejected() {
        // Both land on widgetPattern, so which one wins would be invisible to the author.
        String yaml = FIELD_FORMAT.replace("format: email", "format: email, pattern: '^x$'");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("declares both `format` and `pattern`")),
                "expected a both-declared issue, got: " + ex.getIssues());
    }

    /**
     * A process with a parallel step (#6556), assembled by concatenation so each per-test {@code steps}
     * fragment keeps exact six-space indentation - a multi-line value interpolated into a text block is
     * not re-indented and breaks the YAML.
     */
    private static String parallelProcess(String steps) {
        return "name: pp\n" //
                + "entities:\n" //
                + "  - name: OrderStatus\n" //
                + "    function: Setting\n" //
                + "    fields:\n" //
                + "      - { name: id, type: integer, primaryKey: true, generated: true }\n" //
                + "      - { name: name, type: string }\n" //
                + "  - name: SalesOrder\n" //
                + "    fields:\n" //
                + "      - { name: id, type: integer, primaryKey: true, generated: true }\n" //
                + "    relations:\n" //
                + "      - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }\n" //
                + "processes:\n" //
                + "  - name: Review\n" //
                + "    trigger: { onCreate: SalesOrder }\n" //
                + "    steps:\n" //
                + steps //
                + "forms:\n" //
                + "  - { name: ReviewOrder, forEntity: SalesOrder, fields: [Status], actions: [approve] }\n";
    }

    private static final String P_FORK =
            "      - { name: reviews, kind: parallel, args: { branches: [techReview, commercialReview], next: consolidate } }\n";
    private static final String P_TECH = "      - { name: techReview, kind: userTask, args: { assignee: manager, form: ReviewOrder } }\n";
    private static final String P_COMMERCIAL =
            "      - { name: commercialReview, kind: userTask, args: { assignee: manager, form: ReviewOrder } }\n";
    private static final String P_CONSOLIDATE =
            "      - { name: consolidate, kind: serviceTask, args: { setRelationField: Status, value: 2 } }\n";

    @Test
    void parallelStepParses() {
        IntentParser.parse(parallelProcess(P_FORK + P_TECH + P_COMMERCIAL + P_CONSOLIDATE + "      - { name: done, kind: end }\n"));
    }

    @Test
    void parallelWithFewerThanTwoBranchesIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(
                        parallelProcess("      - { name: reviews, kind: parallel, args: { branches: [techReview], next: consolidate } }\n"
                                + P_TECH + P_CONSOLIDATE)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("needs a `branches` list of at least two")),
                "expected a too-few-branches issue, got: " + ex.getIssues());
    }

    @Test
    void parallelUnknownBranchIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(parallelProcess(
                        "      - { name: reviews, kind: parallel, args: { branches: [techReview, nope], next: consolidate } }\n" + P_TECH
                                + P_CONSOLIDATE)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("branch [nope] is not a declared step")),
                "expected an unknown-branch issue, got: " + ex.getIssues());
    }

    /** A branch that is a two-step chain, and a branch that is itself a nested fork (#6568). */
    @Test
    void parallelBranchChainsAndNestedForksParse() {
        IntentParser.parse(parallelProcess(
                P_FORK + "      - { name: techReview, kind: userTask, args: { assignee: manager, form: ReviewOrder, next: techSignoff } }\n"
                        + "      - { name: techSignoff, kind: serviceTask, args: { setRelationField: Status, value: 2 } }\n"
                        + "      - { name: commercialReview, kind: parallel, args: { branches: [pricing, legal] } }\n"
                        + "      - { name: pricing, kind: userTask, args: { assignee: manager, form: ReviewOrder } }\n"
                        + "      - { name: legal, kind: userTask, args: { assignee: manager, form: ReviewOrder } }\n" + P_CONSOLIDATE));
    }

    @Test
    void parallelBranchRoutingToTheForksOwnNextIsRejected() {
        // `consolidate` is what the JOIN flows into - a branch converges on `join`, never on it directly
        // (otherwise the branch and the join both feed it and the flow loops back into the branch).
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(parallelProcess(
                P_FORK + "      - { name: techReview, kind: userTask, args: { assignee: manager, form: ReviewOrder, next: consolidate } }\n"
                        + P_COMMERCIAL + P_CONSOLIDATE)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("next [consolidate] is also reachable from inside one of its branches")),
                "expected a converge-on-join issue, got: " + ex.getIssues());
    }

    @Test
    void parallelBranchRoutingToEndIsRejected() {
        // A token that ends inside a branch never reaches the join, and the instance hangs on it.
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(parallelProcess(
                        P_FORK + "      - { name: techReview, kind: userTask, args: { assignee: manager, form: ReviewOrder, next: end } }\n"
                                + P_COMMERCIAL + P_CONSOLIDATE)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("routes to `end` from inside a parallel branch")),
                "expected an end-inside-a-branch issue, got: " + ex.getIssues());
    }

    @Test
    void theJoinLiteralOutsideAParallelBranchIsRejected() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(parallelProcess(P_FORK + P_TECH + P_COMMERCIAL
                        + "      - { name: consolidate, kind: serviceTask, args: { setRelationField: Status, value: 2, next: join } }\n")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("routes to `join`, which is only valid inside a parallel branch")),
                "expected a join-outside-a-branch issue, got: " + ex.getIssues());
    }

    @Test
    void aStepReachableFromTwoParallelBranchesIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(parallelProcess(P_FORK
                        + "      - { name: techReview, kind: userTask, args: { assignee: manager, form: ReviewOrder, next: sign } }\n"
                        + "      - { name: commercialReview, kind: userTask, args: { assignee: manager, form: ReviewOrder, next: sign } }\n"
                        + "      - { name: sign, kind: serviceTask, args: { setRelationField: Status, value: 2 } }\n" + P_CONSOLIDATE)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("step [sign] is reachable from more than one parallel branch")),
                "expected a shared-step issue, got: " + ex.getIssues());
    }

    @Test
    void routingIntoAParallelBranchFromTheMainFlowIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(parallelProcess(P_FORK
                + P_TECH + P_COMMERCIAL
                + "      - { name: consolidate, kind: serviceTask, args: { setRelationField: Status, value: 2, next: techReview } }\n")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("which is inside a parallel branch - a branch is entered through its fork only")),
                "expected a routes-into-a-branch issue, got: " + ex.getIssues());
    }

    @Test
    void parallelUnknownNextIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(parallelProcess(
                        "      - { name: reviews, kind: parallel, args: { branches: [techReview, commercialReview], next: nowhere } }\n"
                                + P_TECH + P_COMMERCIAL)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("next [nowhere] is not a declared step or `end`")),
                "expected an unknown-next issue, got: " + ex.getIssues());
    }

    /**
     * `locksWithMaster: false` only means something on a composition child - a top-level entity has no
     * master whose lock it could outlive, so the declaration is rejected instead of silently ignored.
     */
    @Test
    void locksWithMasterIsRejectedOnAnEntityThatIsNotACompositionChild() {
        String yaml = """
                name: sales
                entities:
                  - name: Invoice
                    immutableWhen: "Status == 3"
                    locksWithMaster: false
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                  - name: InvoiceStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("not a composition child")),
                "a non-child locksWithMaster should be rejected, got: " + ex.getIssues());
    }

    /**
     * A master that never locks makes the declaration inert. That is the
     * authored-but-silently-unconsumed failure mode, so it fails at authoring time rather than in
     * production.
     */
    @Test
    void locksWithMasterIsRejectedWhenTheMasterNeverLocks() {
        String yaml = """
                name: sales
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: InvoiceAllocation
                    locksWithMaster: false
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("never locks")),
                "an inert locksWithMaster should be rejected, got: " + ex.getIssues());
    }

    /** The valid shape parses: a composition child of a master that does lock. */
    @Test
    void locksWithMasterParsesOnACompositionChildOfALockingMaster() {
        String yaml = """
                name: sales
                entities:
                  - name: Invoice
                    immutableWhen: "Status == 3"
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                  - name: InvoiceStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: InvoiceAllocation
                    locksWithMaster: false
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertFalse(model.getEntities()
                         .stream()
                         .filter(e -> "InvoiceAllocation".equals(e.getName()))
                         .findFirst()
                         .orElseThrow()
                         .locksWithMaster());
    }

    /** An HR model whose Employee carries a role-scoped daily rate. */
    private static String visibleToYaml(String rateAttributes, String extraFields) {
        return """
                name: hr
                permissions:
                  - { role: Payroll }
                  - { role: Administrator }
                entities:
                  - name: Employee
                    identity: email
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: email, type: string, required: true, unique: true, length: 320 }
                      - { name: dailyRate, type: decimal%s }
                %s""".formatted(rateAttributes, extraFields);
    }

    @Test
    void visibleToScopesAFieldToTheRolesItLists() {
        IntentModel model = IntentParser.parse(visibleToYaml(", visibleTo: [Payroll, Administrator]", ""));

        assertEquals(List.of("Payroll", "Administrator"), model.getEntities()
                                                               .get(0)
                                                               .getFields()
                                                               .stream()
                                                               .filter(f -> "dailyRate".equals(f.getName()))
                                                               .findFirst()
                                                               .orElseThrow()
                                                               .getVisibleTo());
    }

    /**
     * A role no permission grants would leave the field invisible to everybody, with nothing anywhere
     * to say so - so the typo is refused, and the message names the roles the intent does declare.
     */
    @Test
    void visibleToMustNameADeclaredRole() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(visibleToYaml(", visibleTo: [Payrol]", "")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("names role [Payrol]") && i.contains("Administrator, Payroll")),
                "expected an undeclared-role issue naming the declared roles, got: " + ex.getIssues());
    }

    /**
     * An empty allow-list parses identically to an absent one, so it is refused rather than ignored.
     */
    @Test
    void anEmptyVisibleToIsRefused() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(visibleToYaml(", visibleTo: []", "")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("empty `visibleTo`")),
                "expected an empty-allow-list issue, got: " + ex.getIssues());
    }

    @Test
    void visibleToIsRefusedOnTheFieldsTheSurfacesNeed() {
        String key = visibleToYaml("", "").replace("primaryKey: true, generated: true }",
                "primaryKey: true, generated: true," + " visibleTo: [Payroll] }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(key));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("not allowed on the primary key")),
                "expected a primary-key issue, got: " + ex.getIssues());

        String identity = visibleToYaml("", "").replace("unique: true, length: 320 }", "unique: true, length: 320, visibleTo: [Payroll] }");
        IntentValidationException ex2 = assertThrows(IntentValidationException.class, () -> IntentParser.parse(identity));
        assertTrue(ex2.getIssues()
                      .stream()
                      .anyMatch(i -> i.contains("not allowed on the identity field")),
                "expected an identity-field issue, got: " + ex2.getIssues());
    }

    /** The generated {@code Name} is a plain column, so a restricted field must not compose one. */
    @Test
    void visibleToMustNotLeakThroughALabel() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(visibleToYaml(", visibleTo: [Payroll]", "    label: \"{dailyRate}\"\n")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("restricted by visibleTo")),
                "expected a restricted-token issue, got: " + ex.getIssues());
    }

    /**
     * A total summed from a restricted field is that same figure on another entity, where nothing would
     * scope it - so the derived field inherits the allow-list of its source.
     */
    @Test
    void aTotalOverARestrictedFieldInheritsItsAllowList() {
        String yaml = """
                name: hr
                permissions:
                  - { role: Payroll }
                entities:
                  - name: Payslip
                    function: Document
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal, aggregate: true }
                  - name: PayslipItem
                    function: DocumentItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal, visibleTo: [Payroll] }
                    relations:
                      - { name: Payslip, kind: manyToOne, to: Payslip, composition: true, required: true }
                """;
        IntentModel model = IntentParser.parse(yaml);

        assertEquals(List.of("Payroll"), model.getEntities()
                                              .stream()
                                              .filter(e -> "Payslip".equals(e.getName()))
                                              .findFirst()
                                              .orElseThrow()
                                              .getFields()
                                              .stream()
                                              .filter(f -> "total".equals(f.getName()))
                                              .findFirst()
                                              .orElseThrow()
                                              .getVisibleTo(),
                "the document total is the sum of a restricted line figure and must be scoped the same way");
    }
}
