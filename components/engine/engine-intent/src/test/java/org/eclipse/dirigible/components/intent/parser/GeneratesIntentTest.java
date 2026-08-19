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

    /**
     * The step-axis shape of issue #6800: a process that runs ON the create-from's source, and a log
     * entity to append rows to. Ends at the {@code generates} entry's own keys, as the head above does.
     */
    private static final String GENERATES_STEP_HEAD = """
            name: claims
            entities:
              - name: Claim
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: note,   type: string }
                  - { name: amount, type: decimal }
              - name: LogEntry
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: step,   type: string }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Claim, kind: manyToOne, to: Claim }
            processes:
              - name: ClaimApproval
                trigger: { onCreate: Claim }
                steps:
                  - { name: review,   kind: userTask,    args: { assignee: approver, next: activate } }
                  - { name: activate, kind: serviceTask, args: { setField: note, value: activated, next: done } }
                  - { name: done,     kind: end }
              - name: LogReview
                trigger: { onCreate: LogEntry }
                steps:
                  - { name: check, kind: userTask, args: { assignee: approver, next: over } }
                  - { name: over,  kind: end }
            generates:
              - name: log-activation
                from: Claim
                to: LogEntry
                forEntity: Claim
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

    /**
     * The canonical prompted create-from (issue #6685): manual payment allocation on an issued invoice
     * - the two answers the source cannot derive (which payment, how much) are prompted.
     */
    private static final String PROMPTED_ALLOCATION = """
            name: sales
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: CustomerPayment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: SalesInvoiceCustomerPayment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, required: true }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                  - { name: Customer, kind: manyToOne, to: Customer }
                  - { name: CustomerPayment, kind: manyToOne, to: CustomerPayment, required: true }
            generates:
              - name: allocate-payment
                from: SalesInvoice
                to: SalesInvoiceCustomerPayment
                label: Allocate Payment
                icon: link
                map:
                  SalesInvoice: id
                  Customer: Customer
                prompt:
                  - { field: CustomerPayment, required: true }
                  - { field: amount, required: true }
            """;

    @Test
    void parsesAPromptedGenerate() {
        IntentModel model = IntentParser.parse(PROMPTED_ALLOCATION);
        GeneratesIntent g = model.getGenerates()
                                 .get(0);
        assertTrue(g.hasPrompt());
        assertEquals(2, g.getPrompt()
                         .size());
        assertEquals("CustomerPayment", g.getPrompt()
                                         .get(0)
                                         .getField());
        assertTrue(g.getPrompt()
                    .get(0)
                    .isRequired());
        assertEquals("amount", g.getPrompt()
                                .get(1)
                                .getField());
    }

    @Test
    void rejectsAPromptFieldUnknownOnTheTarget() {
        String yaml = PROMPTED_ALLOCATION.replace("{ field: amount, required: true }", "{ field: missing }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(
                             "prompt field [missing] is not a field or to-one relation of the target [SalesInvoiceCustomerPayment]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsADuplicatePromptField() {
        String yaml = PROMPTED_ALLOCATION.replace("{ field: amount, required: true }", "{ field: CustomerPayment }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("prompt names [CustomerPayment] more than once")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsAPromptFieldThatIsAlsoMapped() {
        String yaml = PROMPTED_ALLOCATION.replace("{ field: amount, required: true }", "{ field: Customer }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("prompt field [Customer] is also mapped or defaulted")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsAPromptOnACrossModelTarget() {
        String yaml = """
                name: timesheets
                uses:
                  - { model: sales }
                entities:
                  - name: ProjectTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: invoice-from-timesheet
                    from: ProjectTimesheet
                    to: SalesInvoice
                    uses: sales
                    prompt:
                      - { field: amount }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("prompt is not supported with a cross-model target")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsAPromptWhoseTargetIsNotACompositionChildOfForEntity() {
        // CustomerPayment relates to Customer, not to the invoice the button lives on - there is no
        // detail registration to render the dialog from. (The stale map/prompt leftovers do not fire
        // their own issues: map keys are target-side and amount is a CustomerPayment field too.)
        String yaml = PROMPTED_ALLOCATION.replace("to: SalesInvoiceCustomerPayment", "to: CustomerPayment")
                                         .replace("- { field: CustomerPayment, required: true }\n", "");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(
                             "prompt requires the target [CustomerPayment] to declare a composition to-one relation to forEntity"
                                     + " [SalesInvoice]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsAPromptOnPageScope() {
        String yaml = PROMPTED_ALLOCATION.replace("label: Allocate Payment", "label: Allocate Payment\n    scope: page");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("prompt requires scope 'entity'")),
                "got: " + ex.getIssues());
    }

    /**
     * An event-driven create-from runs on a message, with nobody there to answer an input form - so the
     * combination has no reading that could work, and the generated create-from deliberately takes the
     * prompted values only on the endpoint path.
     */
    @Test
    void rejectsAPromptOnAnEventDrivenCreateFrom() {
        String yaml = PROMPTED_ALLOCATION.replace("label: Allocate Payment",
                "label: Allocate Payment\n    button: true\n    event: { onCreate: SalesInvoice }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("prompt cannot be combined with event:")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsATimestampPromptField() {
        String yaml = PROMPTED_ALLOCATION
                                         .replace("- { name: amount, type: decimal, required: true }",
                                                 "- { name: amount, type: decimal, required: true }\n"
                                                         + "      - { name: allocatedAt, type: timestamp }")
                                         .replace("{ field: amount, required: true }", "{ field: allocatedAt }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("prompt field [allocatedAt] has type timestamp")),
                "got: " + ex.getIssues());
    }

    /**
     * The canonical append shape (issue #6800): a log row per completed step. The step IS the moment,
     * so no {@code when} guard is needed, and the button is still dropped - nobody clicks a log.
     */
    @Test
    void aStepBoundAppendingCreateFromParses() {
        IntentModel model = IntentParser.parse(GENERATES_STEP_HEAD + """
                    event: { onStepCompleted: { process: ClaimApproval, step: activate }, mode: append }
                    map: { Claim: id, Amount: amount }
                    defaults: { Step: "activate" }
                """);
        GeneratesIntent g = model.getGenerates()
                                 .get(0);
        assertTrue(g.isEventDriven());
        assertTrue(g.isAppendMode());
        assertEquals(GeneratesIntent.MODE_APPEND, g.getEventMode());
        assertFalse(g.hasButton());
    }

    /** Absent {@code mode:} is the at-most-once cardinality every existing intent already has. */
    @Test
    void theDefaultCardinalityIsOnce() {
        IntentModel model = IntentParser.parse(GENERATES_STEP_HEAD + """
                    event: { onStepReached: { process: ClaimApproval, step: review } }
                    map: { Claim: id }
                """);
        GeneratesIntent g = model.getGenerates()
                                 .get(0);
        assertEquals(GeneratesIntent.MODE_ONCE, g.getEventMode());
        assertFalse(g.isAppendMode());
    }

    /** A misspelled cardinality must not be read as the default - that would be a silent guard. */
    @Test
    void rejectsAnUnknownEventMode() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_STEP_HEAD + """
                    event: { onStepCompleted: { process: ClaimApproval, step: activate }, mode: always }
                    map: { Claim: id }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("invalid mode [always]")),
                "an unknown mode should be rejected, got: " + ex.getIssues());
    }

    /** A cardinality with nothing to apply it to: there is no guard on a button. */
    @Test
    void rejectsAModeWithoutATrigger() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_STEP_HEAD + """
                    event: { mode: append }
                    map: { Claim: id }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("event requires")),
                "a mode with no trigger should be rejected, got: " + ex.getIssues());
    }

    /** The back-reference is the row's provenance under append - still required. */
    @Test
    void appendModeStillRequiresTheBackReference() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_STEP_HEAD + """
                    event: { onStepCompleted: { process: ClaimApproval, step: activate }, mode: append }
                    map: { Amount: amount }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("provenance under mode: append")),
                "a missing back-reference should be rejected in append mode too, got: " + ex.getIssues());
    }

    @Test
    void rejectsAnUnknownProcessOrStep() {
        IntentValidationException unknownProcess =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_STEP_HEAD + """
                            event: { onStepCompleted: { process: Nope, step: activate } }
                            map: { Claim: id }
                        """));
        assertTrue(unknownProcess.getIssues()
                                 .stream()
                                 .anyMatch(i -> i.contains("unknown process [Nope]")),
                "got: " + unknownProcess.getIssues());

        IntentValidationException unknownStep =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_STEP_HEAD + """
                            event: { onStepCompleted: { process: ClaimApproval, step: nope } }
                            map: { Claim: id }
                        """));
        assertTrue(unknownStep.getIssues()
                              .stream()
                              .anyMatch(i -> i.contains("unknown step [nope]")),
                "got: " + unknownStep.getIssues());
    }

    /** An end (or a decision, or a wait) occupies no moment, so it has no boundary to emit at. */
    @Test
    void rejectsAStepKindWithNoMomentToObserve() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_STEP_HEAD + """
                    event: { onStepCompleted: { process: ClaimApproval, step: done } }
                    map: { Claim: id }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("only a userTask or a serviceTask has a moment to observe")),
                "got: " + ex.getIssues());
    }

    /**
     * The step event is about the record its process runs on - if that is not the create-from's source,
     * the listener would read a record of the wrong entity by an id that means nothing to it.
     */
    @Test
    void rejectsAStepEventWhoseProcessRunsOnAnotherEntity() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_STEP_HEAD + """
                    event: { onStepReached: { process: LogReview, step: check } }
                    map: { Claim: id }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("runs on [LogEntry], not on the from entity [Claim]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsAStepEventNextToALifecycleTrigger() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_STEP_HEAD + """
                    event: { onCreate: Claim, onStepCompleted: { process: ClaimApproval, step: activate } }
                    map: { Claim: id }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("exactly one trigger is allowed")),
                "got: " + ex.getIssues());
    }

    /** A process and its steps belong to the model that declares them - a foreign source has none. */
    @Test
    void rejectsAStepEventOnACrossModelSource() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse("""
                name: claim-logs
                uses:
                  - { model: claims }
                entities:
                  - name: LogEntry
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: step, type: string }
                    relations:
                      - { name: Claim, kind: manyToOne, to: Claim, model: claims }
                processes:
                  - name: LogReview
                    trigger: { onCreate: LogEntry }
                    steps:
                      - { name: check, kind: userTask, args: { assignee: approver, next: over } }
                      - { name: over,  kind: end }
                generates:
                  - name: log-activation
                    from: Claim
                    fromUses: claims
                    to: LogEntry
                    forEntity: Claim
                    event: { onStepReached: { process: LogReview, step: check }, mode: append }
                    map: { Claim: id }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("cross-model source") && i.contains("local to the model that declares them")),
                "got: " + ex.getIssues());
    }

    /** An appending create-from is still event-driven, so there is nobody to answer a prompt. */
    @Test
    void rejectsAPromptOnAnAppendingCreateFrom() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(GENERATES_STEP_HEAD + """
                    event: { onStepCompleted: { process: ClaimApproval, step: activate }, mode: append }
                    map: { Claim: id }
                    prompt:
                      - { field: Step, required: true }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("prompt")),
                "got: " + ex.getIssues());
    }

}
