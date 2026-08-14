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

import org.eclipse.dirigible.components.intent.model.GeneratesIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * Parse + validation coverage for the {@code generates} (create-from) block.
 */
class GeneratesIntentTest {

    /**
     * The declarations-from-fines shape of issue #6711, up to the {@code generates} entry's own keys.
     */
    private static final String GENERATES_EVENT_HEAD = """
            name: fines
            entities:
              - name: FineStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Fine
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                relations:
                  - { name: Status, kind: manyToOne, to: FineStatus, function: EntityStatus, init: 1 }
              - name: Declaration
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                relations:
                  - { name: Fine, kind: manyToOne, to: Fine }
            generates:
              - name: declaration-from-fine
                from: Fine
                to: Declaration
                forEntity: Fine
            """;

    private static final String SAME_MODEL = """
            name: sales
            entities:
              - name: Quote
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: note, type: string }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: QuoteItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Quote, kind: manyToOne, to: Quote, composition: true, required: true }
              - name: Order
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: note, type: string }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: OrderItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Order, kind: manyToOne, to: Order, composition: true, required: true }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            generates:
              - name: order-from-quote
                from: Quote
                to: Order
                label: "Create Order"
                icon: file-plus
                map:
                  Customer: Customer
                  Note: note
                defaults:
                  Note: "from quote"
                items:
                  from: QuoteItem
                  to: OrderItem
                  map:
                    Amount: amount
            """;

    @Test
    void parsesASameModelGenerateWithItems() {
        IntentModel model = IntentParser.parse(SAME_MODEL);
        assertEquals(1, model.getGenerates()
                             .size());
        GeneratesIntent g = model.getGenerates()
                                 .get(0);
        assertEquals("order-from-quote", g.getName());
        assertEquals("Quote", g.getFrom());
        assertEquals("Order", g.getTo());
        assertEquals("Create Order", g.getLabel());
        // forEntity defaults to `from` when unset.
        assertEquals("Quote", g.getForEntity());
        // scope defaults to entity (a create-from needs a source record).
        assertEquals("entity", g.getScope());
        assertEquals("Customer", g.getMap()
                                  .get("Customer"));
        assertEquals("QuoteItem", g.getItems()
                                   .getFrom());
        assertEquals("OrderItem", g.getItems()
                                   .getTo());
    }

    @Test
    void parsesACrossModelGenerate() {
        String yaml = """
                name: timesheets
                uses:
                  - { model: sales }
                entities:
                  - name: ProjectTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: note, type: string }
                generates:
                  - name: invoice-from-timesheet
                    from: ProjectTimesheet
                    to: SalesInvoice
                    uses: sales
                    map:
                      Note: note
                """;
        IntentModel model = IntentParser.parse(yaml);
        GeneratesIntent g = model.getGenerates()
                                 .get(0);
        assertEquals("sales", g.getUses());
        assertEquals("SalesInvoice", g.getTo());
    }

    @Test
    void rejectsUnknownFromEntity() {
        String yaml = """
                name: sales
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: bad
                    from: Missing
                    to: Order
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("from references unknown entity [Missing]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsUnknownToWhenNotCrossModel() {
        String yaml = """
                name: sales
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: bad
                    from: Order
                    to: SalesInvoice
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("to references unknown entity [SalesInvoice]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsUnknownUsesAlias() {
        String yaml = """
                name: timesheets
                entities:
                  - name: ProjectTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: bad
                    from: ProjectTimesheet
                    to: SalesInvoice
                    uses: sales
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("uses unknown model alias [sales]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsMapSourceThatIsNotASourceProperty() {
        String yaml = """
                name: sales
                entities:
                  - name: Quote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: bad
                    from: Quote
                    to: Order
                    map:
                      Note: doesNotExist
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("source [doesNotExist] is not a field or to-one relation")),
                "got: " + ex.getIssues());
    }

    @Test
    void parsesComputedItemLinesAsAList() {
        // A list-valued `items:` is the computed form (issue #6555): it lands in itemLines, NOT items.
        IntentModel model = IntentParser.parse("""
                name: sales
                entities:
                  - name: Quote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: OrderItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: price, type: decimal }
                    relations:
                      - { name: Order, kind: manyToOne, to: Order, composition: true, required: true }
                generates:
                  - name: order-from-quote
                    from: Quote
                    to: Order
                    items:
                      - { name: "Total for the period", price: Total }
                """);
        GeneratesIntent g = model.getGenerates()
                                 .get(0);
        assertEquals(null, g.getItems());
        assertEquals(1, g.getItemLines()
                         .size());
        assertEquals("Total", g.getItemLines()
                               .get(0)
                               .get("price"));
    }

    @Test
    void rejectsComputedItemLineCellNotOnTheTargetItemsChild() {
        String yaml = """
                name: sales
                entities:
                  - name: Quote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: OrderItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: price, type: decimal }
                    relations:
                      - { name: Order, kind: manyToOne, to: Order, composition: true, required: true }
                generates:
                  - name: order-from-quote
                    from: Quote
                    to: Order
                    items:
                      - { nope: 1 }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("item line cell [nope] is not a field or to-one relation")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsComputedItemLineWhenTargetHasNoItemsChild() {
        String yaml = """
                name: sales
                entities:
                  - name: Quote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: order-from-quote
                    from: Quote
                    to: Order
                    items:
                      - { anything: 1 }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("has no composition line-items child")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsComputedItemLineBadWhenGuard() {
        String yaml = """
                name: sales
                entities:
                  - name: Quote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: OrderItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: price, type: decimal }
                    relations:
                      - { name: Order, kind: manyToOne, to: Order, composition: true, required: true }
                generates:
                  - name: order-from-quote
                    from: Quote
                    to: Order
                    items:
                      - { price: Total, when: "Total > 0" }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("item line when") && i.contains("==|!=")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsComputedItemLineUnknownInterpolationSource() {
        String yaml = """
                name: sales
                entities:
                  - name: Quote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: OrderItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                    relations:
                      - { name: Order, kind: manyToOne, to: Order, composition: true, required: true }
                generates:
                  - name: order-from-quote
                    from: Quote
                    to: Order
                    items:
                      - { name: "Line {missing}" }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("interpolates {missing}")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsOneHopRelationFieldMapping() {
        String yaml = """
                name: sales
                entities:
                  - name: Quote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                generates:
                  - name: bad
                    from: Quote
                    to: Order
                    map:
                      Note: Customer.name
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("relation.field path") && i.contains("not yet supported")),
                "got: " + ex.getIssues());
    }

    /**
     * {@code fromUses:} - a SOURCE owned by another model. The source entity is deliberately NOT
     * declared locally: that is the whole point, and it resolves from the owner's {@code .model} at
     * generation time exactly as a cross-model target does.
     */
    @Test
    void acceptsACrossModelSourceWithoutALocalSourceEntity() {
        IntentModel model = IntentParser.parse("""
                name: delivery-notes
                uses:
                  - { model: inventory }
                entities:
                  - name: DeliveryNote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, documentTitle: true }
                generates:
                  - name: delivery-note-from-goods-issue
                    from: GoodsIssue
                    fromUses: inventory
                    to: DeliveryNote
                    forEntity: GoodsIssue
                """);
        GeneratesIntent g = model.getGenerates()
                                 .get(0);
        assertEquals("GoodsIssue", g.getFrom());
        assertEquals("inventory", g.getFromUses());
        assertTrue(g.isCrossModelSource());
    }

    @Test
    void rejectsACrossModelSourceWithAnUndeclaredAlias() {
        String yaml = """
                name: delivery-notes
                entities:
                  - name: DeliveryNote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: bad
                    from: GoodsIssue
                    fromUses: inventory
                    to: DeliveryNote
                    forEntity: GoodsIssue
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("fromUses unknown model alias [inventory]")),
                "got: " + ex.getIssues());
    }

    /**
     * The button is contributed onto the SOURCE's view, which the owner project generates - so it
     * cannot be hosted on a local view, which carries no record of the source id the endpoint needs.
     */
    @Test
    void rejectsACrossModelSourceWhoseForEntityIsNotTheSource() {
        String yaml = """
                name: delivery-notes
                uses:
                  - { model: inventory }
                entities:
                  - name: DeliveryNote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: bad
                    from: GoodsIssue
                    fromUses: inventory
                    to: DeliveryNote
                    forEntity: DeliveryNote
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("forEntity must be the source entity [GoodsIssue]")),
                "got: " + ex.getIssues());
    }

    /** Without {@code fromUses:} an unknown source is still an error - now naming the new key. */
    @Test
    void unknownLocalSourceSuggestsTheFromUsesAlias() {
        String yaml = """
                name: sales
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: bad
                    from: Missing
                    to: Order
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("add a fromUses: alias if the source lives in another model")),
                "got: " + ex.getIssues());
    }

    /**
     * The {@code generates} event trigger (issue #6711). The source is declared once, by {@code from:}
     * - the event says WHEN, never what, so a second entity name there is a mistake rather than a
     * second source. Same for the owning model: {@code fromUses:} declares it.
     */
    @Test
    void generatesEventIsRejectedWhenItNamesAnotherSourceOrRepeatsTheModel() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_EVENT_HEAD + """
                    event: { onTransition: Declaration, model: fines, when: "Status == 2" }
                    map: { Fine: id }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("is not the from entity")),
                "an event naming another entity should be rejected, got: " + ex.getIssues());
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must not declare model:")),
                "an event repeating the owning model should be rejected, got: " + ex.getIssues());
    }

    /** An {@code onTransition} without a status guard would fire on every write of the source. */
    @Test
    void generatesOnTransitionRequiresTheStatusGuard() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_EVENT_HEAD + """
                    event: { onTransition: Fine }
                    map: { Fine: id }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("event requires `when:")),
                "an unguarded onTransition should be rejected, got: " + ex.getIssues());
    }

    /**
     * Without the back-reference an event redelivery mints a second document, so the missing map entry
     * is an authoring error - reported with the fix in the message.
     */
    @Test
    void anEventDrivenGeneratesRequiresTheBackReferenceInItsMap() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_EVENT_HEAD + """
                    event: { onTransition: Fine, when: "Status == 2" }
                    map: { Note: note }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("at-most-once guard")),
                "a missing back-reference should be rejected, got: " + ex.getIssues());
    }

    /** {@code button: false} with no event leaves the create-from with no trigger at all. */
    @Test
    void buttonFalseWithoutAnEventIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_EVENT_HEAD + """
                    button: false
                    map: { Fine: id }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("would generate nothing")),
                "a create-from with neither trigger should be rejected, got: " + ex.getIssues());
    }

    /** The valid shape parses, and the event-driven entry drops its button by default. */
    @Test
    void anEventDrivenGeneratesParsesAndDropsItsButton() {
        IntentModel model = IntentParser.parse(GENERATES_EVENT_HEAD + """
                    event: { onTransition: Fine, when: "Status == 2" }
                    map: { Fine: id }
                """);
        assertTrue(model.getGenerates()
                        .get(0)
                        .isEventDriven());
        assertFalse(model.getGenerates()
                         .get(0)
                         .hasButton(),
                "declaring an event is how an author says nobody has to click");
    }

    /** A create-from with no event keeps its button - the shape every existing intent carries. */
    @Test
    void aCreateFromWithoutAnEventKeepsItsButton() {
        IntentModel model = IntentParser.parse(GENERATES_EVENT_HEAD + """
                    map: { Fine: id }
                """);
        assertFalse(model.getGenerates()
                         .get(0)
                         .isEventDriven());
        assertTrue(model.getGenerates()
                        .get(0)
                        .hasButton());
    }
}
