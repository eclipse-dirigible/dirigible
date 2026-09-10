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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.PostSetSupport.TargetType;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code posts} glue the {@link GlueIntentGenerator} emits from a {@code posts:} rule:
 * the source entity + event + per-item collection + target + idempotency back-reference, and the
 * field map (including a sign-flip Calc expression). Structural glue - the handler template + BPMN
 * wiring is a later stage (the driving suite's PROPOSAL_EVENT_POSTING.md).
 */
class GluePostsTest {

    private static final String YAML = """
            name: inventory
            entities:
              - name: StockMovementStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: StockMovement
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
                  - { name: direction, type: integer }
                  - { name: sequence, type: long }
                  - { name: note, type: string }
                  - { name: postedOn, type: date }
                relations:
                  - { name: Product, kind: manyToOne, to: Product }
                  - { name: GoodsIssue, kind: manyToOne, to: GoodsIssue }
              - name: Product
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
              - name: GoodsIssue
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Store, kind: manyToOne, to: Product }
                  - { name: Status, kind: manyToOne, to: StockMovementStatus, function: EntityStatus, init: 1 }
              - name: GoodsIssueItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
                relations:
                  - { name: GoodsIssue, kind: manyToOne, to: GoodsIssue, composition: true }
                  - { name: Product, kind: manyToOne, to: Product }
            posts:
              - name: goodsIssueLedger
                forEntity: GoodsIssue
                event: 2
                forEach: items
                into: StockMovement
                idempotentBy: GoodsIssue
                guard: negativeStock
                set:
                  Product: item.Product
                  Quantity: "-item.Quantity"
                  Direction: 2
                  Sequence: 10
                  Store: source.Store
                  Note: issued
            """;

    @SuppressWarnings("unchecked")
    @Test
    void emitsTheResolvedPostGlueDescriptor() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> posts = GlueIntentGenerator.buildPostsForTest(model);
        assertEquals(1, posts.size());
        Map<String, Object> p = posts.get(0);

        assertEquals("goodsIssueLedger", p.get("name"));
        assertEquals("GoodsIssueLedger", p.get("className"));
        assertEquals("GoodsIssue", p.get("entity"));
        // status-triggered: not a create, guarded to status seed id 2 on the EntityStatus property.
        assertEquals(Boolean.FALSE, p.get("isCreate"));
        assertEquals("Status", p.get("statusProperty"));
        assertEquals("2", p.get("statusValue"));
        // per-item: the composition child of GoodsIssue is GoodsIssueItem (FK GoodsIssue).
        assertEquals(Boolean.TRUE, p.get("perItem"));
        assertEquals("GoodsIssueItem", p.get("itemsEntity"));
        assertEquals("GoodsIssue", p.get("itemsFk"));
        // target + idempotency back-reference.
        assertEquals("StockMovement", p.get("into"));
        assertEquals("Id", p.get("targetPk"));
        assertEquals("GoodsIssue", p.get("backRef"));

        List<Map<String, String>> assigns = (List<Map<String, String>>) p.get("assigns");
        assertEquals(6, assigns.size());
        // item copy, null-safe negation, integer constant, source copy - rendered to Java expressions.
        assertEquals(Map.of("field", "Product", "expr", "item.Product"), assigns.get(0));
        assertEquals(Map.of("field", "Quantity", "expr", "item.Quantity == null ? null : item.Quantity.negate()"), assigns.get(1));
        assertEquals(Map.of("field", "Direction", "expr", "2"), assigns.get(2));
        // a long column takes a long literal - a bare `10` would not assign to a Long.
        assertEquals(Map.of("field", "Sequence", "expr", "10L"), assigns.get(3));
        assertEquals(Map.of("field", "Store", "expr", "source.Store"), assigns.get(4));
        // a plain constant: a Java string literal, never the bare identifier that would not compile.
        assertEquals(Map.of("field", "Note", "expr", "\"issued\""), assigns.get(5));
    }

    /** A constant carrying a quote or a backslash cannot end the literal it is written into. */
    @Test
    void escapesAConstantThatWouldCloseTheLiteral() {
        assertEquals("\"6\\\" pipe\"", PostSetSupport.expression("6\" pipe"));
        assertEquals("\"back\\\\slash\"", PostSetSupport.expression("back\\slash"));
    }

    /** The forms that are values rather than text: numbers, booleans, an explicitly quoted constant. */
    @Test
    void rendersTheNonTextForms() {
        assertEquals("2", PostSetSupport.expression("2"));
        assertEquals("-3.5", PostSetSupport.expression("-3.5"));
        assertEquals("true", PostSetSupport.expression("true"));
        assertEquals("null", PostSetSupport.expression("null"));
        assertEquals("\"source.Store\"", PostSetSupport.expression("\"source.Store\""));
    }

    /**
     * A numeric constant renders as a literal of the TARGET column's Java type: every intent
     * numeric-with-scale type is a {@code BigDecimal} in the generated entity and {@code long} is a
     * {@code Long}, so a bare number did not compile (dirigible #7287).
     */
    @Test
    void rendersANumberForTheTargetsJavaType() {
        assertEquals("new java.math.BigDecimal(\"-3.5\")", PostSetSupport.expression("-3.5", TargetType.DECIMAL));
        assertEquals("new java.math.BigDecimal(\"2\")", PostSetSupport.expression("2", TargetType.DECIMAL));
        assertEquals("2L", PostSetSupport.expression("2", TargetType.LONG));
        assertEquals("2", PostSetSupport.expression("2", TargetType.INTEGER));
        // a number into a text column is that text, not a bare number assigned to a String
        assertEquals("\"2\"", PostSetSupport.expression("2", TargetType.STRING));
        assertEquals("\"true\"", PostSetSupport.expression("true", TargetType.STRING));
        // an FK the target declares under another name is not resolvable: the bare integer, as before
        assertEquals("2", PostSetSupport.expression("2", TargetType.UNKNOWN));
        // a copy is never retyped - the two columns' types are the author's business
        assertEquals("item.Quantity", PostSetSupport.expression("item.Quantity", TargetType.DECIMAL));
        assertEquals("null", PostSetSupport.expression("null", TargetType.DECIMAL));
    }

    /**
     * The target column's type is read off the model - a field by its own spelling, a relation by its
     * key.
     */
    @Test
    void readsTheTargetColumnsTypeOffTheModel() {
        IntentModel model = IntentParser.parse(YAML);
        EntityIntent movement = IntentEntities.byName(model)
                                              .get("StockMovement");
        assertEquals(TargetType.DECIMAL, PostSetSupport.targetType(movement, IntentEntities.byName(model), "Quantity"));
        assertEquals(TargetType.LONG, PostSetSupport.targetType(movement, IntentEntities.byName(model), "Sequence"));
        assertEquals(TargetType.INTEGER, PostSetSupport.targetType(movement, IntentEntities.byName(model), "Direction"));
        assertEquals(TargetType.STRING, PostSetSupport.targetType(movement, IntentEntities.byName(model), "Note"));
        assertEquals(TargetType.TEMPORAL, PostSetSupport.targetType(movement, IntentEntities.byName(model), "PostedOn"));
        // a to-one relation is its target's integer key column
        assertEquals(TargetType.INTEGER, PostSetSupport.targetType(movement, IntentEntities.byName(model), "Product"));
        // no such column: rendered as it always was
        assertEquals(TargetType.UNKNOWN, PostSetSupport.targetType(movement, IntentEntities.byName(model), "Nowhere"));
    }

    /**
     * A constant the target column cannot hold is refused at parse - it would otherwise reach javac as
     * a literal of the wrong type and break the compile of the whole generated module, the class of
     * defect the closed vocabulary exists to end.
     */
    @Test
    void refusesAConstantTheColumnCannotHold() {
        assertRefused(YAML.replace("Sequence: 10", "Sequence: -3.5"), "set [Sequence]", "has a fraction");
        assertRefused(YAML.replace("Direction: 2", "Direction: issued"), "set [Direction]", "is not one");
        assertRefused(YAML.replace("Quantity: \"-item.Quantity\"", "Quantity: issued"), "set [Quantity]", "is not a number");
        assertRefused(YAML.replace("Note: issued", "PostedOn: today"), "set [PostedOn]", "date/time");
    }

    /**
     * The remedy the refusal prints WORKS: the spelling it names survives YAML and reaches the parser
     * with its double quotes, so following it renders the text as a string literal instead of earning
     * the identical refusal a second time (dirigible #7287).
     */
    @Test
    @SuppressWarnings("unchecked")
    void theQuotedRemedySurvivesYaml() {
        String dotted = YAML.replace("Note: issued", "Note: Receipt.Store");
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(dotted));
        String refusal = failure.getIssues()
                                .stream()
                                .filter(issue -> issue.contains("set [Note]"))
                                .findFirst()
                                .orElseThrow();
        assertTrue(refusal.contains("'\"Receipt.Store\"'"), "expected the remedy to be spelled for YAML, got " + refusal);

        // the remedy, verbatim, through the parser
        IntentModel model = IntentParser.parse(YAML.replace("Note: issued", "Note: '\"Receipt.Store\"'"));
        List<Map<String, String>> assigns = (List<Map<String, String>>) GlueIntentGenerator.buildPostsForTest(model)
                                                                                           .get(0)
                                                                                           .get("assigns");
        assertEquals(Map.of("field", "Note", "expr", "\"Receipt.Store\""), assigns.get(assigns.size() - 1));
    }

    private static void assertRefused(String yaml, String field, String reason) {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(failure.getIssues()
                          .stream()
                          .anyMatch(issue -> issue.contains(field) && issue.contains(reason)),
                "expected a refusal naming " + field + " and [" + reason + "], got " + failure.getIssues());
    }

    /**
     * A value that reads as an expression the renderer cannot compile is refused at parse time, naming
     * the rule and the field - never rendered, since both other outcomes are silent.
     */
    @Test
    void refusesAnExpressionItCannotRender() {
        String yaml = YAML.replace("Store: source.Store", "Store: Receipt.Store");
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(failure.getIssues()
                          .stream()
                          .anyMatch(issue -> issue.contains("posts [goodsIssueLedger] set [Store]") && issue.contains("[Receipt.Store]")),
                "expected the refusal to name the rule and the field, got " + failure.getIssues());
    }
}
