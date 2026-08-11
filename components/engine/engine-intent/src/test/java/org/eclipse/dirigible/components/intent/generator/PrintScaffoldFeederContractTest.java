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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.generator.print.PrintIntentGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The scaffold/feeder CONTRACT: every {@code {{...}}} placeholder the generated
 * {@code standard.print} scaffold emits must be a key the generated print feeder actually puts. The
 * two generators derive from the same model but used to disagree - the scaffold referenced the
 * primary key (which the feeder deliberately excludes) and bare relation maps that carried no
 * {@code __label} - and a dead placeholder renders as an empty value the template author then hunts
 * through the whole pipeline. The fixture exercises the shapes that broke: a {@code number:}-style
 * DocumentTitle, an EntityStatus relation, a document back-reference (a target with no {@code name}
 * field, labeled by its number), and a relation to an entity with no resolvable label at all.
 */
class PrintScaffoldFeederContractTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([^}]+)}}");

    private static final String INTENT = """
            name: shipping
            entities:
              # a document master with a DocumentTitle number and NO name field - the back-reference
              # target below: it must be labeled by its number, not dropped.
              - name: GoodsIssue
                function: Document
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, function: DocumentTitle }
                  - { name: note,   type: string }
                relations:
                  - { name: Status, kind: manyToOne, to: ShippingStatus, function: EntityStatus, init: 1 }
              - name: GoodsIssueItem
                function: DocumentItem
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
                relations:
                  - { name: GoodsIssue, kind: manyToOne, to: GoodsIssue, composition: true, required: true }

              # the invoice: number + status + a back-reference to the GoodsIssue + a relation whose
              # target resolves NO label (no name, no label:, no DocumentTitle).
              - name: SalesInvoice
                function: Document
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, function: DocumentTitle }
                  - { name: date,   type: date }
                  - { name: total,  type: decimal, aggregate: true }
                relations:
                  - { name: Status,     kind: manyToOne, to: ShippingStatus, function: EntityStatus, init: 1 }
                  - { name: GoodsIssue, kind: manyToOne, to: GoodsIssue }
                  - { name: Bare,       kind: manyToOne, to: Bare }
              - name: SalesInvoiceItem
                function: DocumentItem
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: name,     type: string }
                  - { name: quantity, type: decimal }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                  - { name: Unit, kind: manyToOne, to: Unit }

              - name: ShippingStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Unit
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              # no name, no label:, no DocumentTitle - nothing can label a reference to it
              - name: Bare
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: code, type: integer }
            """;

    private final IntentModel model = IntentParser.parse(INTENT);
    private final Map<String, EntityIntent> byName = IntentEntities.byName(model);

    private Map<String, Map<String, Object>> feedersByEntity() {
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "app", null);
        Map<String, Map<String, Object>> feeders = new LinkedHashMap<>();
        for (Map<String, Object> feeder : PrintFeederSupport.buildPrintFeeders(model, byName, IntentEntities.compositionParents(model),
                context)) {
            feeders.put((String) feeder.get("entity"), feeder);
        }
        return feeders;
    }

    @Test
    void aDocumentBackReferenceIsLabeledByItsNumberNotDropped() {
        EntityIntent invoice = byName.get("SalesInvoice");
        String scaffold = PrintIntentGenerator.buildTemplate(invoice, byName.get("SalesInvoiceItem"), byName);
        assertTrue(scaffold.contains("{{document.GoodsIssue}}"),
                "a back-reference to another document must stay on the scaffold - it is labeled by its number");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) feedersByEntity().get("SalesInvoice")
                                                                                       .get("nodes");
        Map<String, Object> goodsIssue = nodes.stream()
                                              .filter(node -> "GoodsIssue".equals(node.get("keyInParent")))
                                              .findFirst()
                                              .orElseThrow();
        assertTrue("Number".equals(goodsIssue.get("labelField")),
                "the feeder labels the back-reference by the target's DocumentTitle field, got: " + goodsIssue.get("labelField"));
    }

    @Test
    void aRelationWithNoResolvableLabelIsOmittedFromTheScaffold() {
        EntityIntent invoice = byName.get("SalesInvoice");
        String scaffold = PrintIntentGenerator.buildTemplate(invoice, byName.get("SalesInvoiceItem"), byName);
        assertFalse(scaffold.contains("{{document.Bare}}"),
                "a relation whose target resolves no label would render an empty value - the scaffold must omit it");
    }

    @Test
    void theFooterNeverReferencesThePrimaryKey() {
        // GoodsIssue HAS a number, so its footer uses it; strip the number to force the fallback.
        EntityIntent goodsIssue = byName.get("GoodsIssue");
        goodsIssue.getFields()
                  .removeIf(field -> "number".equals(field.getName()));
        String scaffold = PrintIntentGenerator.buildTemplate(goodsIssue, byName.get("GoodsIssueItem"), byName);
        assertFalse(scaffold.contains("{{document.Id}}"),
                "the feeder deliberately excludes the primary key - the scaffold must never reference it");
    }

    /** The sweep: every placeholder the scaffold emits resolves against the feeder's contract. */
    @Test
    @SuppressWarnings("unchecked")
    void everyScaffoldPlaceholderIsAKeyTheFeederPuts() {
        for (Map.Entry<String, Map<String, Object>> entry : feedersByEntity().entrySet()) {
            EntityIntent master = byName.get(entry.getKey());
            Map<String, Object> feeder = entry.getValue();
            String scaffold = PrintIntentGenerator.buildTemplate(master, byName.get((String) feeder.get("itemsEntity")), byName);

            Set<String> documentKeys = new LinkedHashSet<>();
            ((List<Map<String, Object>>) feeder.get("rootScalars")).forEach(scalar -> documentKeys.add((String) scalar.get("name")));
            ((List<Map<String, Object>>) feeder.get("nodes")).forEach(node -> documentKeys.add((String) node.get("keyInParent")));
            Set<String> itemKeys = new LinkedHashSet<>();
            ((List<Map<String, Object>>) feeder.get("itemScalars")).forEach(scalar -> itemKeys.add((String) scalar.get("name")));
            ((List<Map<String, Object>>) feeder.get("itemNodes")).forEach(node -> itemKeys.add((String) node.get("keyInParent")));

            Matcher matcher = PLACEHOLDER.matcher(scaffold);
            while (matcher.find()) {
                String path = matcher.group(1)
                                     .trim();
                String[] segments = path.split("\\.");
                if ("document".equals(segments[0])) {
                    assertTrue(documentKeys.contains(segments[1]), "scaffold placeholder {{" + path + "}} of [" + master.getName()
                            + "] is not fed - the feeder puts only " + documentKeys);
                } else {
                    assertTrue(itemKeys.contains(segments[0]), "scaffold items placeholder {{" + path + "}} of [" + master.getName()
                            + "] is not fed - the feeder puts only " + itemKeys);
                }
            }
        }
    }
}
