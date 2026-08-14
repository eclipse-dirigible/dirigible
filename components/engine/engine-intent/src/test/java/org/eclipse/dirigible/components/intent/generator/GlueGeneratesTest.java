/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code generates} entries the {@link GlueIntentGenerator} emits: the pre-rendered
 * field assignment expressions (source copy vs {@code now} / literal), the composition-item foreign
 * keys, and the cross-model target gen folder resolution.
 */
class GlueGeneratesTest {

    private static final String YAML = """
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
                  - { name: date, type: date }
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
                map:
                  Customer: Customer
                  Note: note
                defaults:
                  Date: now
                  Note: "from quote"
                items:
                  from: QuoteItem
                  to: OrderItem
                  map:
                    Amount: amount
            """;

    /**
     * The motivating shape of issue #6711: a fine arrives by webhook, the responsible person is
     * identified (a status transition), and the declaration document is minted from it with nobody
     * clicking. The status is named, not numbered - the resolver turns it into the seed id before the
     * typed mapping.
     */
    private static final String EVENT_YAML = """
            name: fines
            entities:
              - name: FineStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Fine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                relations:
                  - { name: Status, kind: manyToOne, to: FineStatus, function: EntityStatus, init: 1 }
              - name: Declaration
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                relations:
                  - { name: Fine, kind: manyToOne, to: Fine }
            generates:
              - name: declaration-from-fine
                from: Fine
                to: Declaration
                forEntity: Fine
                event: { onTransition: Fine, when: "Status == POSTED" }
                map:
                  Fine: id
                  Note: note
            seeds:
              - name: fine-statuses
                entity: FineStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: POSTED }
            """;

    @SuppressWarnings("unchecked")
    @Test
    void rendersHeaderAssignmentsItemsAndKeys() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> generates = GlueIntentGenerator.buildGeneratesForTest(model);
        assertEquals(1, generates.size());
        Map<String, Object> g = generates.get(0);

        assertEquals("OrderFromQuote", g.get("className"));
        assertEquals("Quote", g.get("fromEntity"));
        assertEquals("Order", g.get("toEntity"));
        assertEquals(false, g.get("crossModel"));
        assertEquals(true, g.get("hasItems"));
        // A document child's FK back to its master is, by convention, the master entity's name.
        assertEquals("Quote", g.get("srcFkProperty"));
        assertEquals("Order", g.get("toFkProperty"));

        List<Map<String, Object>> fields = (List<Map<String, Object>>) g.get("fieldAssignments");
        // map first (source copy), then defaults (now / literal).
        assertTrue(fields.contains(Map.of("targetProp", "Customer", "expr", "source.Customer")));
        assertTrue(fields.contains(Map.of("targetProp", "Note", "expr", "source.Note")));
        assertTrue(fields.contains(Map.of("targetProp", "Date", "expr", "java.time.LocalDate.now()")));
        assertTrue(fields.contains(Map.of("targetProp", "Note", "expr", "\"from quote\"")));

        List<Map<String, Object>> itemFields = (List<Map<String, Object>>) g.get("itemFieldAssignments");
        assertTrue(itemFields.contains(Map.of("targetProp", "Amount", "expr", "srcItem.Amount")));

        // No completion hook declared - the template's #if renders nothing.
        assertEquals("", g.get("sourceStatusProperty"));
        assertEquals("", g.get("sourceStatusValue"));
    }

    @Test
    void completionHookResolvesTheSourceStatusRelation() {
        IntentModel model = IntentParser.parse("""
                name: sales
                entities:
                  - name: ProformaStatus
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Proforma
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                    relations:
                      - { name: Status, kind: manyToOne, to: ProformaStatus, function: EntityStatus, init: 1 }
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                generates:
                  - name: invoice-from-proforma
                    from: Proforma
                    to: Invoice
                    forEntity: Proforma
                    sourceStatus: 3
                """);
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(model)
                                                   .get(0);

        // Pre-resolved to the EntityStatus FK property + the seed id the source flips to.
        assertEquals("Status", g.get("sourceStatusProperty"));
        assertEquals("3", g.get("sourceStatusValue"));
        assertEquals("Proforma", g.get("fromPerspective"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void resolvesCrossModelTargetGenFolderAndFlag() {
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
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(model)
                                                   .get(0);
        assertEquals(true, g.get("crossModel"));
        assertEquals("sales", g.get("toModel"));
        // With no repository, the cross-model perspective falls back to the entity name (convention).
        assertEquals("SalesInvoice", g.get("toPerspective"));
        assertEquals(false, ((Boolean) g.get("hasItems")).booleanValue());

        List<Map<String, Object>> fields = (List<Map<String, Object>>) g.get("fieldAssignments");
        assertTrue(fields.contains(Map.of("targetProp", "Note", "expr", "source.Note")));
    }

    /**
     * A cross-model SOURCE ({@code fromUses:}) is what lets a create-from be authored on the module
     * owning the TARGET, so only that module references the other and the pair stays independently
     * compilable (and jar-packageable). The glue must therefore point the SOURCE half at the owner's
     * gen folder / project while the generated controller itself stays in this project.
     */
    @SuppressWarnings("unchecked")
    @Test
    void resolvesCrossModelSourceGenFolderAndOwningProject() {
        IntentModel model = IntentParser.parse("""
                name: delivery-notes
                uses:
                  - { model: inventory }
                entities:
                  - name: DeliveryNote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, documentTitle: true }
                    relations:
                      - { name: GoodsIssue, kind: manyToOne, to: GoodsIssue, model: inventory }
                generates:
                  - name: delivery-note-from-goods-issue
                    from: GoodsIssue
                    fromUses: inventory
                    to: DeliveryNote
                    forEntity: GoodsIssue
                    map:
                      GoodsIssue: id
                """);
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(model)
                                                   .get(0);

        assertEquals(true, g.get("crossModelSource"));
        assertEquals("inventory", g.get("fromModel"));
        // The project owning the source's views and its "-transitioned" topic (alias, no project:).
        assertEquals("inventory", g.get("fromProject"));
        assertEquals("GoodsIssue", g.get("fromEntity"));
        // With no repository the owner perspective falls back to the entity name (convention).
        assertEquals("GoodsIssue", g.get("fromPerspective"));
        // The TARGET is local here - the exact mirror of the usual cross-model-target case.
        assertEquals(false, g.get("crossModel"));
        assertEquals("DeliveryNote", g.get("toEntity"));
        assertEquals("", g.get("toModel"));

        List<Map<String, Object>> fields = (List<Map<String, Object>>) g.get("fieldAssignments");
        assertTrue(fields.contains(Map.of("targetProp", "GoodsIssue", "expr", "source.Id")));
    }

    /** A purely local generate keeps the cross-model-source markers empty (backward compatibility). */
    @Test
    void localSourceLeavesTheCrossModelSourceMarkersEmpty() {
        IntentModel model = IntentParser.parse(YAML);
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(model)
                                                   .get(0);
        assertFalse(((Boolean) g.get("crossModelSource")).booleanValue());
        assertEquals("", g.get("fromModel"));
        assertEquals("", g.get("fromProject"));
    }

    @Test
    void integerDecimalAndBooleanLiteralsRenderTyped() {
        String yaml = """
                name: sales
                entities:
                  - name: Quote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: rate, type: decimal }
                      - { name: qty, type: integer }
                      - { name: active, type: boolean }
                generates:
                  - name: order-from-quote
                    from: Quote
                    to: Order
                    defaults:
                      Qty: "3"
                      Rate: "1.5"
                      Active: "true"
                """;
        IntentModel model = IntentParser.parse(yaml);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) GlueIntentGenerator.buildGeneratesForTest(model)
                                                                                          .get(0)
                                                                                          .get("fieldAssignments");
        assertTrue(fields.contains(Map.of("targetProp", "Qty", "expr", "3")));
        assertTrue(fields.contains(Map.of("targetProp", "Rate", "expr", "new java.math.BigDecimal(\"1.5\")")));
        assertTrue(fields.contains(Map.of("targetProp", "Active", "expr", "true")));
        assertFalse(fields.isEmpty());
    }

    /**
     * The source item may be a SEPARATE primary entity that references the source document by FK (an
     * aggregate document whose per-line detail is its own entity), not a composition child. Its package
     * must resolve from its OWN perspective (not the source document's), and a numeric item default
     * must render as {@code BigDecimal} for the decimal line column.
     */
    @SuppressWarnings("unchecked")
    @Test
    void nonCompositionSourceItemResolvesItsOwnPackageAndDecimalDefaults() {
        IntentModel model = IntentParser.parse("""
                name: work
                uses:
                  - { model: sales-invoices }
                entities:
                  - name: Sheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, documentTitle: true }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer, model: sales-invoices }
                  - name: Line
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                      - { name: amount, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: Sheet, kind: manyToOne, to: Sheet, required: true }
                generates:
                  - name: invoice-from-sheet
                    from: Sheet
                    to: SalesInvoice
                    uses: sales-invoices
                    forEntity: Sheet
                    map:
                      Customer: Customer
                    items:
                      from: Line
                      to: SalesInvoiceItem
                      map:
                        name: number
                        price: amount
                      defaults:
                        quantity: 1
                """);
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(model)
                                                   .get(0);
        assertEquals(true, g.get("hasItems"));
        assertEquals("Line", g.get("fromItemEntity"));
        // The source document's perspective is `Sheet`; the item's OWN perspective is `Line` - the
        // template must qualify srcItem with the latter (sanitized to the `line` package), or it
        // references a non-existent class. (For a composition-child item these two coincide.)
        assertEquals("Sheet", g.get("fromPerspective"));
        assertEquals("Line", g.get("fromItemPerspective"));
        // The FK the item loop queries by is the source document entity name.
        assertEquals("Sheet", g.get("srcFkProperty"));

        List<Map<String, Object>> itemFields = (List<Map<String, Object>>) g.get("itemFieldAssignments");
        assertTrue(itemFields.contains(Map.of("targetProp", "Name", "expr", "srcItem.Number")));
        assertTrue(itemFields.contains(Map.of("targetProp", "Price", "expr", "srcItem.Amount")));
        // The decimal `quantity` default renders as BigDecimal (a bare `1` would not compile).
        assertTrue(itemFields.contains(Map.of("targetProp", "Quantity", "expr", "new java.math.BigDecimal(\"1\")")));
    }

    /**
     * An item default naming a TO-ONE RELATION of the item target (a required classifier the map has no
     * source for - a line's TaxRate) is a foreign-key ID: it must stay a bare integer literal. The
     * decimal-column BigDecimal wrap would not compile against the generated Integer FK field - the
     * exact miss that made invoice-from-timesheet emit header-only invoices (every line insert violated
     * the NOT NULL tax-rate column, uncoverable by the map).
     */
    @SuppressWarnings("unchecked")
    @Test
    void relationItemDefaultStaysAnIntegerForeignKey() {
        IntentModel model = IntentParser.parse("""
                name: work
                entities:
                  - name: Sheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, documentTitle: true }
                  - name: Line
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                      - { name: amount, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: Sheet, kind: manyToOne, to: Sheet, required: true }
                  - name: TaxRate
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Invoice
                    function: Document
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, documentTitle: true }
                  - name: InvoiceItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: quantity, type: decimal, precision: 18, scale: 3 }
                      - { name: price, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                      - { name: TaxRate, kind: manyToOne, to: TaxRate, required: true }
                generates:
                  - name: invoice-from-sheet
                    from: Sheet
                    to: Invoice
                    forEntity: Sheet
                    items:
                      from: Line
                      to: InvoiceItem
                      map:
                        name: number
                        price: amount
                      defaults:
                        quantity: 1
                        TaxRate: 1
                """);
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(model)
                                                   .get(0);
        assertEquals(true, g.get("hasItems"));

        List<Map<String, Object>> itemFields = (List<Map<String, Object>>) g.get("itemFieldAssignments");
        // The decimal default keeps the BigDecimal wrap...
        assertTrue(itemFields.contains(Map.of("targetProp", "Quantity", "expr", "new java.math.BigDecimal(\"1\")")));
        // ...but the relation default is the FK id, assigned as the bare integer.
        assertTrue(itemFields.contains(Map.of("targetProp", "TaxRate", "expr", "1")));
    }

    /**
     * The computed line-items form (issue #6555): a list-valued {@code items:} builds a fixed set of
     * synthetic target lines whose cells are expressions over the SOURCE master - a numeric cell runs
     * through {@code Calc} rounded to the target field's scale (a bare literal is a trivial
     * expression), a {@code {field}} string cell interpolates the source, a to-one relation copies the
     * source FK, and a {@code when} cell becomes a null-safe {@code Calc} row guard. The target items
     * child is resolved automatically (never named). No source item repository is iterated - the mirror
     * keys stay empty.
     */
    @SuppressWarnings("unchecked")
    @Test
    void computedItemLinesRenderExpressionsOverTheSource() {
        IntentModel model = IntentParser.parse("""
                name: sales
                entities:
                  - name: Timesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, documentTitle: true }
                      - { name: period, type: month }
                      - { name: billableAmount, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                      - { name: Product, kind: manyToOne, to: Product }
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, documentTitle: true }
                      - { name: date, type: date }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                  - name: InvoiceItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: quantity, type: decimal, precision: 18, scale: 3 }
                      - { name: price, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                      - { name: Product, kind: manyToOne, to: Product }
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: invoice-from-timesheet
                    from: Timesheet
                    to: Invoice
                    forEntity: Timesheet
                    map:
                      Customer: Customer
                    defaults:
                      Date: now
                    items:
                      - name: "Services for {period}"
                        quantity: 1
                        price: BillableAmount
                        Product: Product
                        when: "BillableAmount != 0"
                """);
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(model)
                                                   .get(0);

        assertEquals(false, g.get("hasItems"));
        assertEquals(true, g.get("hasItemLines"));
        // The target items child + its master FK are resolved automatically - the intent never names them.
        assertEquals("InvoiceItem", g.get("toItemEntity"));
        assertEquals("Invoice", g.get("toFkProperty"));
        // The mirror-form keys stay empty (no source item repository is iterated).
        assertEquals("", g.get("fromItemEntity"));
        assertTrue(((List<?>) g.get("itemFieldAssignments")).isEmpty());

        List<Map<String, Object>> lines = (List<Map<String, Object>>) g.get("itemLines");
        assertEquals(1, lines.size());
        Map<String, Object> row = lines.get(0);
        // The `when` cell becomes a null-safe Calc row guard (the postings guard convention).
        assertEquals("Calc.eval(\"BillableAmount\", source, 6).compareTo(new java.math.BigDecimal(\"0\")) != 0", row.get("guard"));

        List<Map<String, Object>> assigns = (List<Map<String, Object>>) row.get("assigns");
        // String cell: {period} interpolates the source master (a month field is a plain String).
        assertTrue(assigns.contains(Map.of("targetProp", "Name", "expr", "\"Services for \" + String.valueOf(source.Period)")));
        // Numeric cells run through Calc rounded to the TARGET field's scale (quantity 3, price 2).
        assertTrue(assigns.contains(Map.of("targetProp", "Quantity", "expr", "Calc.eval(\"1\", source, 3)")));
        assertTrue(assigns.contains(Map.of("targetProp", "Price", "expr", "Calc.eval(\"BillableAmount\", source, 2)")));
        // A to-one relation cell copies the raw source foreign key (issue #6533 parity), not a Calc value.
        assertTrue(assigns.contains(Map.of("targetProp", "Product", "expr", "source.Product")));
    }

    /**
     * A string cell that is neither a {@code {}} template nor a source property is a plain literal (a
     * caption is not mistaken for a field), and a {@code double} target narrows the {@code Calc}
     * result.
     */
    /**
     * The event trigger (issue #6711): an {@code onTransition} create-from binds the source's
     * {@code -transitioned} topic through the {@code isCreate: false} marker, guards on the status the
     * seeded NAME resolved to, and derives its at-most-once back-reference from the {@code map} entry
     * that copies the source key. With no {@code button:} declared it is event-only, so no endpoint and
     * no button are emitted - and it lands in the event-driven subset the listener template renders.
     */
    @Test
    void eventDrivenGenerateCarriesTheTriggerGuardAndBackReference() {
        IntentModel model = IntentParser.parse(EVENT_YAML);
        List<Map<String, Object>> generates = GlueIntentGenerator.buildGeneratesForTest(model);
        Map<String, Object> g = generates.get(0);

        assertEquals(true, g.get("hasEvent"));
        assertEquals(false, g.get("isCreate"));
        assertEquals(true, g.get("eventOnly"));
        // The guard is the SOURCE's status FK against the id its seeded name resolved to.
        assertEquals("Status", g.get("guardProperty"));
        assertEquals("2", g.get("guardValue"));
        // Derived from `map: { Fine: id }` - never declared twice.
        assertEquals("Fine", g.get("backRefProperty"));
        assertEquals("Id", g.get("fromPk"));
    }

    /**
     * {@code onCreate} binds the source's bare create topic (the platform publishes creates unsuffixed)
     * and needs no status guard; an explicit {@code button: true} keeps the click as well, and the
     * button then shares the event's at-most-once guard.
     */
    @Test
    void onCreateEventNeedsNoGuardAndAnExplicitButtonIsKept() {
        IntentModel model = IntentParser.parse(EVENT_YAML.replace("event: { onTransition: Fine, when: \"Status == POSTED\" }",
                "event: { onCreate: Fine }\n    button: true"));
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(model)
                                                   .get(0);

        assertEquals(true, g.get("hasEvent"));
        assertEquals(true, g.get("isCreate"));
        assertEquals(false, g.get("eventOnly"));
        assertEquals("", g.get("guardProperty"));
        assertEquals("", g.get("guardValue"));
        assertEquals("Fine", g.get("backRefProperty"));
    }

    /**
     * A create-from with no event keeps every event marker empty - the template then renders exactly
     * what it rendered before the trigger existed, including no at-most-once guard (minting several
     * targets from one source by clicking twice is a legitimate manual act).
     */
    @Test
    void aCreateFromWithoutAnEventCarriesNoTriggerAndNoGuard() {
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(YAML))
                                                   .get(0);
        assertEquals(false, g.get("hasEvent"));
        assertEquals(false, g.get("eventOnly"));
        assertEquals("", g.get("backRefProperty"));
        assertEquals("", g.get("guardProperty"));
    }

    /**
     * A CROSS-MODEL source's key field is only known once the owner model resolves, so the missing
     * back-reference is caught here rather than by the parser - loudly, because without it the
     * create-from cannot recognize its own output and every event redelivery would mint another
     * document.
     */
    @Test
    void anEventDrivenGenerateWithoutABackReferenceFailsLoudly() {
        IntentModel model = IntentParser.parse("""
                name: declarations
                uses:
                  - { model: fines }
                entities:
                  - name: Declaration
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Fine, kind: manyToOne, to: Fine, model: fines }
                generates:
                  - name: declaration-from-fine
                    from: Fine
                    fromUses: fines
                    to: Declaration
                    forEntity: Fine
                    event: { onTransition: Fine, when: "Status == 2" }
                    map:
                      Note: note
                """);
        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> GlueIntentGenerator.buildGeneratesForTest(model));
        assertTrue(failure.getMessage()
                          .contains("back-reference"),
                "the failure must name the missing back-reference: " + failure.getMessage());
    }

    @SuppressWarnings("unchecked")
    @Test
    void computedItemLineStringLiteralAndDoubleNarrowing() {
        IntentModel model = IntentParser.parse("""
                name: sales
                entities:
                  - name: Timesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: hours, type: decimal, precision: 18, scale: 2 }
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: InvoiceItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: factor, type: double }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                generates:
                  - name: invoice-from-timesheet
                    from: Timesheet
                    to: Invoice
                    items:
                      - name: "Consulting services"
                        factor: "Hours * 2"
                """);
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(model)
                                                   .get(0);
        List<Map<String, Object>> assigns = (List<Map<String, Object>>) ((List<Map<String, Object>>) g.get("itemLines")).get(0)
                                                                                                                        .get("assigns");
        assertTrue(assigns.contains(Map.of("targetProp", "Name", "expr", "\"Consulting services\"")));
        assertTrue(assigns.contains(Map.of("targetProp", "Factor", "expr", "Calc.eval(\"Hours * 2\", source, 2).doubleValue()")));
    }
}
