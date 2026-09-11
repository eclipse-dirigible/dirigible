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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;

/**
 * The source-row rule of a create-from's mirror items block (issue #7091): which rows of the source
 * document become lines of the target, and what an unqualified one costs.
 *
 * <p>
 * The motivating shape is "invoice the approved month": every member timesheet of a project-month
 * used to be cloned into an invoice line, so an unapproved one was billed at the same footing as an
 * approved one - and an empty one, whose mapped quantity the target refuses, stopped the whole
 * month from being invoiced with nothing the intent could say about it.
 */
class GlueGeneratesItemsWhereTest {

    /** A project-month billed from its member timesheets, only the approved and non-empty ones. */
    private static final String YAML = """
            name: timesheets
            entities:
              - name: TimesheetStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: ProjectTimesheet
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: period, type: string, documentTitle: true }
              - name: EmployeeTimesheet
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: employeeName, type: string }
                  - { name: totalHours, type: decimal }
                  - { name: rate, type: decimal }
                  - { name: closedOn, type: date }
                relations:
                  - { name: ProjectTimesheet, kind: manyToOne, to: ProjectTimesheet, composition: true, required: true }
                  - { name: Status, kind: manyToOne, to: TimesheetStatus, function: EntityStatus, init: 1 }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
              - name: SalesInvoiceItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: quantity, type: decimal }
                  - { name: price, type: decimal }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
            generates:
              - name: invoice-from-timesheet
                from: ProjectTimesheet
                to: SalesInvoice
                items:
                  from: EmployeeTimesheet
                  to: SalesInvoiceItem
                  where:
                    - { field: Status, op: eq, value: APPROVED }
                    - { field: totalHours, op: gt, value: 0 }
                  map:
                    Name: employeeName
                    Quantity: totalHours
                    Price: rate
            seeds:
              - name: timesheet-statuses
                entity: TimesheetStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: SUBMITTED }
                  - { id: 3, name: APPROVED }
                  - { id: 4, name: REJECTED }
            """;

    /**
     * The rule renders as the tail of the very {@code Criteria} that already selects the source's item
     * rows by their master foreign key, so the rows it excludes are never loaded. The status is NAMED:
     * an id is positional, and inserting a status mid-nomenclature would otherwise silently retarget
     * the rule - so it is resolved to its seed id before the typed mapping, exactly as every other
     * status site is.
     */
    @Test
    void theRuleRendersAsTheItemQuerysCriteriaTailWithTheStatusNameResolved() {
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(YAML))
                                                   .get(0);

        assertEquals(true, g.get("hasItems"));
        assertEquals(".eq(\"Status\", 3).gt(\"TotalHours\", 0)", g.get("itemWhere"));
        // Skipping is the default: no message, so an unqualified row is simply left out.
        assertEquals("", g.get("itemRefuse"));
    }

    /**
     * {@code refuse:} declares the other reading - an unqualified row stops the whole create-from. It
     * is a property of the document, not of the platform: dropping a rejected line silently and billing
     * it silently are both wrong, for different months.
     */
    @Test
    void aDeclaredRefusalIsCarriedOntoTheDescriptor() {
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(YAML.replace("""
                      map:
                        Name: employeeName
                """, """
                      refuse: "Member timesheet is not approved"
                      map:
                        Name: employeeName
                """)))
                                                   .get(0);

        assertEquals(".eq(\"Status\", 3).gt(\"TotalHours\", 0)", g.get("itemWhere"));
        assertEquals("Member timesheet is not approved", g.get("itemRefuse"));
    }

    /**
     * A moment is a legitimate rule value, resolved against the clock of the run that fires - the whole
     * point of reusing the {@code where} shape a schedule's query already carries.
     */
    @Test
    void aMomentValueRendersAgainstTheClockOfTheRun() {
        Map<String, Object> g = GlueIntentGenerator
                                                   .buildGeneratesForTest(
                                                           IntentParser.parse(YAML.replace("- { field: totalHours, op: gt, value: 0 }",
                                                                   "- { field: closedOn, op: le, value: CURRENT_DATE }")))
                                                   .get(0);

        assertEquals(".eq(\"Status\", 3).le(\"ClosedOn\", java.time.LocalDate.now())", g.get("itemWhere"));
    }

    /**
     * The keys are opt-in: an items block with no rule keeps exactly the descriptor it had, so a
     * create-from written before this existed regenerates the unfiltered clone loop it always had.
     */
    @Test
    void withoutARuleTheDescriptorIsUnchanged() {
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(YAML.replace("""
                      where:
                        - { field: Status, op: eq, value: APPROVED }
                        - { field: totalHours, op: gt, value: 0 }
                """, "")))
                                                   .get(0);

        assertEquals(true, g.get("hasItems"));
        assertEquals("", g.get("itemWhere"));
        assertEquals("", g.get("itemRefuse"));
    }

    /** A rule the database would reject on the first click is refused at parse. */
    @Test
    void aFieldTheSourceItemDoesNotDeclareIsRefused() {
        IntentValidationException failure = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(YAML.replace("field: totalHours", "field: approvedHours")));

        assertTrue(failure.getMessage()
                          .contains("approvedHours"),
                "the failure must name the field: " + failure.getMessage());
    }

    /** The operator vocabulary is the schedule query's, and closed. */
    @Test
    void anUnsupportedOperatorIsRefused() {
        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(YAML.replace("op: gt", "op: between")));

        assertTrue(failure.getMessage()
                          .contains("unsupported operator"),
                "the failure must name the operator: " + failure.getMessage());
    }

    /**
     * Without conditions no row is ever unqualified, so the message is a promise nothing can keep - the
     * authored-but-unconsumed class this module refuses everywhere else.
     */
    @Test
    void aRefusalWithNoRuleIsRefused() {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(YAML.replace("""
                      where:
                        - { field: Status, op: eq, value: APPROVED }
                        - { field: totalHours, op: gt, value: 0 }
                """, """
                      refuse: "Member timesheet is not approved"
                """)));

        assertTrue(failure.getMessage()
                          .contains("refuse with no where"),
                "the failure must say the rule is missing: " + failure.getMessage());
    }

    /** A moment compared against a non-temporal field is a query that could never match. */
    @Test
    void aMomentComparedWithANonTemporalFieldIsRefused() {
        IntentValidationException failure = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(YAML.replace("op: gt, value: 0", "op: gt, value: CURRENT_DATE")));

        assertTrue(failure.getMessage()
                          .contains("non-temporal"),
                "the failure must name the shape mismatch: " + failure.getMessage());
    }

    /**
     * A delivery note generated from another model's goods issue, its lines from the issue's lines -
     * the cross-model SOURCE shape ({@code fromUses:}), whose items are owned by the owner model too.
     */
    private static final String CROSS_MODEL_YAML = """
            name: delivery-notes
            uses:
              - { model: inventory }
            entities:
              - name: DeliveryNote
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
              - name: DeliveryNoteItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
                relations:
                  - { name: DeliveryNote, kind: manyToOne, to: DeliveryNote, composition: true, required: true }
            generates:
              - name: delivery-note-from-goods-issue
                from: GoodsIssue
                fromUses: inventory
                to: DeliveryNote
                forEntity: GoodsIssue
                map:
                  Number: number
                items:
                  from: GoodsIssueItem
                  to: DeliveryNoteItem
                  where:
                    - { field: Status, op: eq, value: APPROVED }
                  map:
                    Quantity: quantity
            """;

    /**
     * The owner model as the inventory project generated it: the item's status FK is the property the
     * edm generator gave the {@code DOCUMENT_STATUS} widget, which is how a consumer learns WHICH
     * property is the status one.
     */
    private static final String OWNER_MODEL = """
            {
              "model": {
                "entities": [
                  {
                    "name": "GoodsIssue",
                    "perspectiveName": "GoodsIssue",
                    "dataName": "INVENTORY_GOODSISSUE",
                    "properties": [
                      { "name": "Id", "dataName": "ID", "dataType": "INTEGER", "dataPrimaryKey": "true" },
                      { "name": "Number", "dataName": "NUMBER", "dataType": "VARCHAR" }
                    ]
                  },
                  {
                    "name": "GoodsIssueItem",
                    "perspectiveName": "GoodsIssue",
                    "dataName": "INVENTORY_GOODSISSUEITEM",
                    "properties": [
                      { "name": "Id", "dataName": "ID", "dataType": "INTEGER", "dataPrimaryKey": "true" },
                      { "name": "Quantity", "dataName": "QUANTITY", "dataType": "DECIMAL" },
                      { "name": "GoodsIssue", "dataName": "GOODSISSUE_ID", "dataType": "INTEGER",
                        "relationshipType": "COMPOSITION", "relationshipEntityName": "GoodsIssue", "widgetType": "DROPDOWN" },
                      { "name": "Status", "dataName": "STATUS_ID", "dataType": "INTEGER",
                        "relationshipEntityName": "GoodsIssueItemStatus", "widgetType": "DOCUMENT_STATUS" }
                    ]
                  }
                ]
              }
            }
            """;

    /**
     * A cross-model item's nomenclature is seeded in the owner model, so a status NAME in its rule
     * cannot resolve - and used to be left in place, rendering as a string compared against the integer
     * status FK: a rule that matched nothing on every click, with no diagnostic (dirigible #7225). It
     * is refused the way every other cross-model status site is - by seed id only - at the one point
     * the owner {@code .model} tells which condition names the status.
     */
    @Test
    void aStatusNameOnACrossModelItemSourceIsRefused() {
        IntentGenerationContext context = contextWithOwnerModel(IntentParser.parse(CROSS_MODEL_YAML));

        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> GlueIntentGenerator.buildGeneratesForTest(context.getModel(), context));

        assertTrue(failure.getIssues()
                          .stream()
                          .anyMatch(issue -> issue.contains("[Status]") && issue.contains("[APPROVED]") && issue.contains("[inventory]")
                                  && issue.contains("numeric seed id")),
                "the refusal must name the relation, the name and the owner model: " + failure.getIssues());
    }

    /** The seed id is the cross-model form, and it renders exactly as a local rule does. */
    @Test
    void aStatusSeedIdOnACrossModelItemSourceRenders() {
        IntentGenerationContext context =
                contextWithOwnerModel(IntentParser.parse(CROSS_MODEL_YAML.replace("value: APPROVED", "value: 3")));

        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(context.getModel(), context)
                                                   .get(0);

        assertEquals(true, g.get("crossModelSource"));
        assertEquals(true, g.get("hasItems"));
        assertEquals(".eq(\"Status\", 3)", g.get("itemWhere"));
        // Read off the owner model, not guessed from the item's name.
        assertEquals("GoodsIssue", g.get("fromItemPerspective"));
        assertEquals("Id", g.get("fromItemPk"));
    }

    /**
     * Only the status condition is subject to the rule: the other conditions compare ordinary columns,
     * where a string is just a value.
     */
    @Test
    void aStringOnAnOrdinaryCrossModelItemColumnIsNotAStatus() {
        IntentGenerationContext context = contextWithOwnerModel(IntentParser.parse(
                CROSS_MODEL_YAML.replace("- { field: Status, op: eq, value: APPROVED }", "- { field: quantity, op: gt, value: 0 }")));

        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(context.getModel(), context)
                                                   .get(0);

        assertEquals(".gt(\"Quantity\", 0)", g.get("itemWhere"));
    }

    /**
     * A context whose repository serves {@link #OWNER_MODEL} as the sibling inventory project's model.
     */
    private static IntentGenerationContext contextWithOwnerModel(IntentModel model) {
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(missing.exists()).thenReturn(false);
        IResource owner = mock(IResource.class);
        when(owner.exists()).thenReturn(true);
        when(owner.getContent()).thenReturn(OWNER_MODEL.getBytes(StandardCharsets.UTF_8));
        when(repository.getResource(anyString())).thenReturn(missing);
        when(repository.getResource("/users/admin/workspace/inventory/inventory.model")).thenReturn(owner);
        return TestContexts.context(model, repository, "/users/admin/workspace/delivery-notes", "app");
    }
}
