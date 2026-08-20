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

import java.util.List;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.LifecycleIntent;
import org.junit.jupiter.api.Test;

/**
 * Parse + validation coverage for the declarative state machine ({@code lifecycle:}) and the sites
 * that have to agree with it.
 */
class LifecycleGraphIntentTest {

    /** A three-status invoice with a graph over its nomenclature; each case varies one line of it. */
    private static final String MODEL = """
            name: billing
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                relations:
                  - { name: status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                lifecycle:
                  edges:
                    - { from: DRAFT,  to: [ISSUED, CANCELLED] }
                    - { from: ISSUED, to: [PAID] }
            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT,     stage: draft }
                  - { id: 2, name: ISSUED,    stage: live }
                  - { id: 3, name: PAID,      stage: live }
                  - { id: 8, name: CANCELLED, stage: cancelled }
            """;

    private static List<String> issuesOf(String yaml) {
        return assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml)).getIssues();
    }

    private static void assertIssue(List<String> issues, String fragment) {
        assertTrue(issues.stream()
                         .anyMatch(issue -> issue.contains(fragment)),
                "expected an issue containing [" + fragment + "], got: " + issues);
    }

    @Test
    void parsesAGraphAuthoredWithSeededStatusNames() {
        IntentModel model = IntentParser.parse(MODEL);
        EntityIntent invoice = model.getEntities()
                                    .get(1);
        LifecycleIntent lifecycle = invoice.getLifecycle();
        assertEquals(2, lifecycle.getEdges()
                                 .size());
        // The names are resolved to seed ids before the typed mapping, like every other status site.
        assertEquals(Integer.valueOf(1), lifecycle.getEdges()
                                                  .get(0)
                                                  .getFrom());
        assertEquals(List.of(2, 8), lifecycle.getEdges()
                                             .get(0)
                                             .getTo());
        assertEquals(Integer.valueOf(2), lifecycle.getEdges()
                                                  .get(1)
                                                  .getFrom());
        assertEquals(List.of(3), lifecycle.getEdges()
                                          .get(1)
                                          .getTo());
    }

    @Test
    void rejectsAnEntityWithoutAStatusRelation() {
        String yaml = """
                name: billing
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    lifecycle:
                      edges:
                        - { from: 1, to: [2] }
                """;
        assertIssue(issuesOf(yaml), "requires the entity to declare a function: EntityStatus relation");
    }

    @Test
    void rejectsTheRedundantOnKey() {
        // YAML 1.1 reads a bare `on` as the boolean true, so the key would bind to nothing at all.
        assertIssue(issuesOf(MODEL.replace("    lifecycle:\n      edges:", "    lifecycle:\n      on: status\n      edges:")),
                "lifecycle declares `on`");
    }

    @Test
    void rejectsAnUnseededNomenclature() {
        String yaml = MODEL.substring(0, MODEL.indexOf("seeds:"))
                           .replace("from: DRAFT,  to: [ISSUED, CANCELLED]", "from: 1, to: [2, 8]")
                           .replace("from: ISSUED, to: [PAID]", "from: 2, to: [3]");
        assertIssue(issuesOf(yaml), "needs [InvoiceStatus] to be seeded in this model");
    }

    @Test
    void rejectsAnEdgeToAnUnseededStatus() {
        assertIssue(issuesOf(MODEL.replace("to: [PAID]", "to: [99]")), "edge [ISSUED] to [99] is not a seeded status");
    }

    @Test
    void rejectsASelfEdge() {
        assertIssue(issuesOf(MODEL.replace("to: [PAID]", "to: [ISSUED]")), "edge [ISSUED] lists itself as a target");
    }

    @Test
    void rejectsASourceStatusDeclaredTwice() {
        assertIssue(issuesOf(MODEL.replace("- { from: ISSUED, to: [PAID] }", "- { from: DRAFT, to: [PAID] }")),
                "edge [DRAFT] is declared more than once");
    }

    @Test
    void rejectsAnEmptyGraph() {
        String yaml = MODEL.replace("- { from: DRAFT,  to: [ISSUED, CANCELLED] }", "")
                           .replace("- { from: ISSUED, to: [PAID] }", "[]");
        assertIssue(issuesOf(yaml), "declares no edges");
    }

    @Test
    void reportsAnInitTooLargeToBeASeedIdInsteadOfFailing() {
        // A run of digits satisfies "looks numeric" and still overflows an int: the graph must answer
        // "not a seeded status", not throw out of the parse and 500 the editor's validation call.
        assertIssue(issuesOf(MODEL.replace("function: EntityStatus, init: 1", "function: EntityStatus, init: 99999999999999")),
                "starts at init [99999999999999], which is not a seeded status");
    }

    @Test
    void ignoresAWorkflowStatusValueTooLargeToBeASeedId() {
        // Same overflow one site further out - the step's own validation owns that error, and the graph
        // must not fail the whole parse on the way past it.
        assertIssue(issuesOf(MODEL + """
                processes:
                  - name: Approval
                    trigger: { onCreate: Invoice }
                    steps:
                      - { name: Issue, kind: serviceTask, args: { setRelationField: status, value: 99999999999999 } }
                """), "value [99999999999999] must be an integer record id");
    }

    @Test
    void acceptsATransitionThatFollowsAnEdge() {
        IntentModel model = IntentParser.parse(MODEL + """
                transitions:
                  - { name: CancelInvoice, forEntity: Invoice, from: [DRAFT], setStatus: CANCELLED }
                """);
        assertEquals(1, model.getTransitions()
                             .size());
    }

    @Test
    void rejectsATransitionTheGraphDoesNotAllow() {
        assertIssue(issuesOf(MODEL + """
                transitions:
                  - { name: CancelInvoice, forEntity: Invoice, from: [ISSUED], setStatus: CANCELLED }
                """), "transition [CancelInvoice] moves [ISSUED] to [CANCELLED], which entity [Invoice] lifecycle does not allow");
    }

    @Test
    void rejectsAWorkflowStepWritingAStatusNoEdgeReaches() {
        assertIssue(issuesOf(MODEL + """
                processes:
                  - name: Approval
                    trigger: { onCreate: Invoice }
                    steps:
                      - { name: Issue, kind: serviceTask, args: { setRelationField: status, value: DRAFT } }
                """), "step [Issue] sets the status to [DRAFT], which no edge of the [Invoice] lifecycle reaches");
    }

    @Test
    void acceptsAWorkflowStepWritingAStatusAnEdgeReaches() {
        IntentModel model = IntentParser.parse(MODEL + """
                processes:
                  - name: Approval
                    trigger: { onCreate: Invoice }
                    steps:
                      - { name: Issue, kind: serviceTask, args: { setRelationField: status, value: ISSUED } }
                """);
        assertEquals(1, model.getProcesses()
                             .size());
    }

    /**
     * A create-from that mints a follow-up document and flips the source into its post-generation
     * status. The status is a seed id: unlike a transition, a check or a resolve outcome,
     * {@code sourceStatus} is not one of the sites the symbolic-name resolver rewrites, so a seeded
     * name would not reach the graph as an id at all.
     */
    private static final String GENERATES = """
            generates:
              - name: credit-from-invoice
                from: Invoice
                to: CreditNote
                forEntity: Invoice
                sourceStatus: %s
            """;

    private static final String CREDIT_NOTE = """
              - name: CreditNote
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
            """;

    /**
     * The worst of the unchecked status writes: the flip runs after the target document is already
     * committed, so a move the graph does not declare leaves a credit note behind whose invoice never
     * transitioned - and the only symptom is "the note exists but the invoice still shows as issued".
     */
    @Test
    void rejectsACreateFromFlippingTheSourceToAStatusNoEdgeReaches() {
        String yaml = MODEL.replace("seeds:", CREDIT_NOTE + "seeds:") + GENERATES.formatted("1");
        assertIssue(issuesOf(yaml), "generates [credit-from-invoice] flips the source status to [DRAFT], which no edge of the [Invoice]"
                + " lifecycle reaches");
    }

    @Test
    void acceptsACreateFromFlippingTheSourceAlongAnEdge() {
        String yaml = MODEL.replace("seeds:", CREDIT_NOTE + "seeds:") + GENERATES.formatted("8");
        assertEquals(1, IntentParser.parse(yaml)
                                    .getGenerates()
                                    .size());
    }

    /**
     * A register lookup routing its outcome by status writes the same FK as every other status site.
     */
    private static final String RESOLVE = """
              - name: Rate
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: validFrom, type: date }
                relations:
                  - { name: currency, kind: manyToOne, to: Currency }
              - name: Currency
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            resolves:
              - name: invoice-rate
                event: { onCreate: Invoice }
                set: currency
                from: Rate
                match: { currency: currency }
                between: { start: validFrom, value: issuedOn }
                notFound: { setStatus: %s }
            """;

    private static String modelWithResolve(String status) {
        return MODEL.replace("      - { name: number, type: string, documentTitle: true }",
                "      - { name: number, type: string, documentTitle: true }\n      - { name: issuedOn, type: date }")
                    .replace("      - { name: status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }",
                            "      - { name: status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }\n"
                                    + "      - { name: currency, kind: manyToOne, to: Currency }")
                    .replace("seeds:", RESOLVE.formatted(status) + "seeds:");
    }

    @Test
    void rejectsARegisterLookupRoutingToAStatusNoEdgeReaches() {
        assertIssue(issuesOf(modelWithResolve("DRAFT")),
                "resolve [invoice-rate] notFound routes the record to [DRAFT], which no edge of the [Invoice] lifecycle reaches");
    }

    @Test
    void acceptsARegisterLookupRoutingAlongAnEdge() {
        assertEquals(1, IntentParser.parse(modelWithResolve("CANCELLED"))
                                    .getResolves()
                                    .size());
    }
}
