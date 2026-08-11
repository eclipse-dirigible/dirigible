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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.intent.generator.bpmn.BpmnIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * The BPMN a multi-step parallel branch emits (#6568): a branch is a chain that joins at its
 * terminal, a decision inside a branch converges with the {@code join} literal, and a branch that
 * is itself a {@code parallel} nests its own fork/join pair.
 */
class ParallelBranchChainTest {

    /**
     * One fork with two branches. The first is a two-step chain whose user task is editable (so a
     * writer delegate is inserted after it, and the chain must continue from the WRITER, not the task).
     * The second is a nested fork whose own branches are a decision that rejoins from both arms and a
     * plain task; the nested join declares no {@code next}, so it joins into the outer one.
     */
    private static final String YAML = """
            name: parallelchains
            entities:
              - name: OrderStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }
            processes:
              - name: OrderReview
                trigger: { onCreate: SalesOrder }
                steps:
                  - { name: reviews, kind: parallel, args: { branches: [techReview, commercial], next: consolidate } }
                  - { name: techReview, kind: userTask, args: { assignee: manager, form: ReviewOrder, next: techSignoff } }
                  - { name: techSignoff, kind: serviceTask, args: { setRelationField: Status, value: 2 } }
                  - { name: commercial, kind: parallel, args: { branches: [pricing, legal] } }
                  - { name: pricing, kind: decision, args: { if: "amount > 1000", then: escalate, else: join } }
                  - { name: escalate, kind: userTask, args: { assignee: manager, form: ReviewOrder } }
                  - { name: legal, kind: userTask, args: { assignee: manager, form: ReviewOrder } }
                  - { name: consolidate, kind: serviceTask, args: { setRelationField: Status, value: 3 } }
                  - { name: done, kind: end }
            forms:
              - { name: ReviewOrder, forEntity: SalesOrder, fields: [note, Status], editable: [note], actions: [approve] }
            """;

    private static String bpmn() {
        IntentModel model = IntentParser.parse(YAML);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "app", repository);
        context.setSettings(IntentSettings.scaffold(model));

        new BpmnIntentGenerator().generate(context);

        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(repository, atLeastOnce()).createResource(paths.capture(), contents.capture());
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            if (paths.getAllValues()
                     .get(i)
                     .endsWith("/OrderReview.bpmn")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("the process BPMN was not written; wrote " + paths.getAllValues());
    }

    private static void assertFlow(String bpmn, String source, String target) {
        assertTrue(bpmn.contains("sourceRef=\"" + source + "\" targetRef=\"" + target + "\""),
                "expected a flow " + source + " -> " + target + " in:\n" + bpmn);
    }

    @Test
    void aBranchChainRunsItsOwnStepsAndJoinsAtItsTerminal() {
        String bpmn = bpmn();

        assertFlow(bpmn, "reviews", "techReview");
        // The user task is editable, so a writer delegate runs after it and carries the branch's `next`
        // - the chain continues from the writer, and the task itself must not route on directly.
        assertFlow(bpmn, "techReview", "orderReviewTechReviewWrite");
        assertFlow(bpmn, "orderReviewTechReviewWrite", "techSignoff");
        assertFalse(bpmn.contains("sourceRef=\"techReview\" targetRef=\"techSignoff\""),
                "the branch must chain through the writer, not around it");
        // The chain's last step declares no routing at all - that is what makes it the branch terminal.
        assertFlow(bpmn, "techSignoff", "reviewsJoin");
        assertFlow(bpmn, "reviewsJoin", "consolidate");
        // The branch chain is off the linear chain: the fork never falls through to the join's target,
        // and the chain's steps are not spliced into the main flow.
        assertFalse(bpmn.contains("sourceRef=\"reviews\" targetRef=\"consolidate\""), "the fork fans to its branches");
        assertFalse(bpmn.contains("sourceRef=\"techSignoff\" targetRef=\"commercial\""),
                "a branch step must not fall through to the next declared step");
    }

    @Test
    void aNestedParallelBranchNestsItsOwnForkJoinPair() {
        String bpmn = bpmn();

        assertTrue(bpmn.contains("<parallelGateway id=\"commercial\"") && bpmn.contains("<parallelGateway id=\"commercialJoin\""),
                "the nested fork gets its own gateway pair");
        assertFlow(bpmn, "reviews", "commercial");
        assertFlow(bpmn, "commercial", "legal");
        // `legal` is editable too, so its branch terminates at its writer.
        assertFlow(bpmn, "legal", "orderReviewLegalWrite");
        assertFlow(bpmn, "orderReviewLegalWrite", "commercialJoin");
        // The nested fork declares no `next`, so its join flows into the enclosing join.
        assertFlow(bpmn, "commercialJoin", "reviewsJoin");
    }

    @Test
    void aDecisionInsideABranchConvergesOnTheJoinLiteral() {
        String bpmn = bpmn();

        // The decision tests the trigger entity's own field, so a field loader is inserted before it -
        // the flow into the branch must land on the LOADER, and the decision's arms leave from itself.
        assertFlow(bpmn, "commercial", "loadOrderReviewPricing");
        assertFlow(bpmn, "loadOrderReviewPricing", "pricing");
        assertTrue(bpmn.contains("id=\"flow_pricing_then\" sourceRef=\"pricing\" targetRef=\"escalate\""),
                "the conditioned arm routes to `then`, in:\n" + bpmn);
        assertTrue(bpmn.contains("id=\"flow_pricing_default\" sourceRef=\"pricing\" targetRef=\"commercialJoin\""),
                "`else: join` converges on the enclosing join, in:\n" + bpmn);
        // `escalate` declares no routing, so it joins (through its writer) - it must not fall through to
        // the next declared step, which belongs to the sibling branch.
        assertFlow(bpmn, "orderReviewEscalateWrite", "commercialJoin");
        assertFalse(bpmn.contains("targetRef=\"legal\"") && bpmn.contains("sourceRef=\"orderReviewEscalateWrite\" targetRef=\"legal\""),
                "a decision arm must join, not fall through into the sibling branch");
    }

    @Test
    void theEndEventNeverGetsAnOutgoingFlow() {
        // Pulling the branch + join nodes out of the linear chain can leave the end event adjacent to
        // itself (a declared `end` step, then the implicit end) - a flow out of it is invalid BPMN.
        assertFalse(bpmn().contains("sourceRef=\"end\""), "the end event must have no outgoing sequence flow");
    }

    @Test
    void everyNodeIsLaidOutAndNoFlowDanglesOutsideTheDiagram() {
        String bpmn = bpmn();

        for (String node : new String[] {"reviews", "reviewsJoin", "commercial", "commercialJoin", "techReview", "techSignoff", "pricing",
                "escalate", "legal", "consolidate"}) {
            assertTrue(bpmn.contains("BPMNShape_" + node + "\""), "expected a DI shape for [" + node + "] in:\n" + bpmn);
        }
    }

    @Test
    void theWholeMechanismIsIdempotent() {
        assertTrue(bpmn().equals(bpmn()), "identical input must produce byte-identical BPMN");
    }
}
