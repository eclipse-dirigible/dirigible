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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.generator.edm.CrossModelSupport;
import org.eclipse.dirigible.components.intent.generator.print.PrintIntentGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;

/**
 * Builds the {@code printFeeders} glue collection: one descriptor per document (header-items)
 * master, driving the generated {@code gen/events/<Entity>PrintFeeder.java} controller. The feeder
 * loads the document record and its related graph through the generated repositories and assembles
 * the nested {@code {document, items}} payload the document-template engine
 * ({@code parsers/document}) binds — so a print template can dereference
 * {@code {{document.Customer.Address}}} instead of only a flat FK label.
 *
 * <p>
 * <b>Model-driven, not template-driven:</b> the feeder provides the master's whole reachable to-one
 * graph, not just the paths the current {@code .print} happens to reference — because the
 * {@code .print} is generate-once and adapted per tenant, so a hand-added path must resolve without
 * regenerating the feeder against the frozen template. The generated Java is the audit artifact: it
 * names, line by line, every repository loaded and every field put on the payload.
 *
 * <p>
 * <b>Depth:</b> same-model relations recurse to depth 2 (cycle-guarded by entity name); a
 * cross-model relation is resolved to depth 1 (the target's own fields via
 * {@link CrossModelSupport}) — descending <i>into</i> a cross-model entity's own relations needs
 * the target's relation graph, which {@code CrossModelSupport} does not expose yet (documented
 * follow-up).
 *
 * <p>
 * The plan is <b>flattened</b> into an ordered node list (parent before child), each node carrying
 * its parent's entity/map variable, so the Velocity template renders it with a single pass and no
 * recursion.
 */
final class PrintFeederSupport {

    /** Maximum relation depth walked from the document master (root = 0). */
    private static final int MAX_DEPTH = 2;

    private PrintFeederSupport() {}

    /**
     * One feeder descriptor per document master in the model.
     *
     * @param model the parsed intent model
     * @param byName entities indexed by name
     * @param compositionParents each entity's transitive composition parent (perspective resolution)
     * @param context the generation context (for cross-model {@code .model} resolution)
     * @return the {@code printFeeders} collection (possibly empty)
     */
    static List<Map<String, Object>> buildPrintFeeders(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentGenerationContext context) {
        List<Map<String, Object>> feeders = new ArrayList<>();
        for (Map.Entry<EntityIntent, EntityIntent> entry : PrintIntentGenerator.documentMasters(model)
                                                                               .entrySet()) {
            feeders.add(buildFeeder(entry.getKey(), entry.getValue(), model, byName, compositionParents, context));
        }
        return feeders;
    }

    private static Map<String, Object> buildFeeder(EntityIntent master, EntityIntent items, IntentModel model,
            Map<String, EntityIntent> byName, Map<String, String> compositionParents, IntentGenerationContext context) {
        Map<String, Object> feeder = new LinkedHashMap<>();
        feeder.put("className", master.getName());
        feeder.put("entity", master.getName());
        feeder.put("perspective", IntentEntities.resolvePerspective(master.getName(), compositionParents));
        feeder.put("rootScalars", scalarDescriptors(master));

        feeder.put("itemsEntity", items.getName());
        feeder.put("itemsPerspective", IntentEntities.resolvePerspective(items.getName(), compositionParents));
        feeder.put("itemsFkProperty", masterFkProperty(items, master.getName()));
        feeder.put("itemScalars", scalarDescriptors(items));

        List<Map<String, Object>> nodes = new ArrayList<>();
        Set<String> usedVars = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        visited.add(master.getName());
        for (RelationIntent relation : master.getRelations()) {
            addNode(relation, "root", "document", 1, model, byName, compositionParents, context, nodes, usedVars, visited);
        }
        feeder.put("nodes", nodes);
        return feeder;
    }

    /**
     * Append a relation node (and, for a same-model target within the depth budget, its own to-one
     * relations) in pre-order so every parent variable is materialised before the child that reads it.
     */
    private static void addNode(RelationIntent relation, String parentEntityVar, String parentMapVar, int depth, IntentModel model,
            Map<String, EntityIntent> byName, Map<String, String> compositionParents, IntentGenerationContext context,
            List<Map<String, Object>> nodes, Set<String> usedVars, Set<String> visited) {
        if (!isToOne(relation) || relation.getName() == null || relation.getTo() == null) {
            return;
        }
        boolean crossModel = relation.getModel() != null && !relation.getModel()
                                                                     .isBlank();
        String entityVar = uniqueVar(relation.getName(), usedVars);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("entityVar", entityVar);
        node.put("mapVar", entityVar + "Map");
        node.put("parentEntityVar", parentEntityVar);
        node.put("parentMapVar", parentMapVar);
        node.put("fkProperty", IntentNaming.pascalCase(relation.getName()));
        node.put("keyInParent", IntentNaming.pascalCase(relation.getName()));
        node.put("entity", relation.getTo());
        node.put("crossModel", crossModel);

        if (crossModel) {
            UsesIntent uses = findUses(model, relation.getModel());
            CrossModelSupport.TargetInfo target = uses == null ? null : CrossModelSupport.resolve(context, uses, relation.getTo());
            node.put("model", relation.getModel());
            node.put("perspective", target != null ? target.perspectiveName() : relation.getTo());
            node.put("labelField", target != null ? target.labelField() : "Name");
            node.put("scalars", crossModelScalars(target));
            nodes.add(node);
            // Depth-2 into a cross-model entity's own relations is a documented follow-up.
        } else {
            EntityIntent target = byName.get(relation.getTo());
            node.put("model", "");
            node.put("perspective", target != null && target.isSetting() ? "Settings"
                    : IntentEntities.resolvePerspective(relation.getTo(), compositionParents));
            node.put("labelField", nameField(target));
            node.put("scalars", scalarDescriptors(target));
            nodes.add(node);
            if (depth < MAX_DEPTH && target != null && visited.add(target.getName())) {
                for (RelationIntent childRelation : target.getRelations()) {
                    addNode(childRelation, entityVar, entityVar + "Map", depth + 1, model, byName, compositionParents, context, nodes,
                            usedVars, visited);
                }
            }
        }
    }

    /**
     * The entity's own persistable fields as {@code {name, date}} descriptors (PascalCase name, primary
     * key excluded). {@code date} flags a {@code date}/{@code timestamp} field so the feeder emits its
     * ISO string ({@code .toString()}) rather than the Jackson array/epoch shape the print binder would
     * otherwise render verbatim; numeric fields stay raw so the binder's money formatting still
     * applies.
     */
    private static List<Map<String, Object>> scalarDescriptors(EntityIntent entity) {
        List<Map<String, Object>> scalars = new ArrayList<>();
        if (entity != null) {
            for (FieldIntent field : entity.getFields()) {
                if (!field.isPrimaryKey() && field.getName() != null) {
                    scalars.add(scalar(IntentNaming.pascalCase(field.getName()), isDateType(field.getType())));
                }
            }
        }
        return scalars;
    }

    /**
     * A cross-model target's fields as descriptors. {@link CrossModelSupport} exposes only property
     * names (not types), so none are flagged as dates — a cross-model date field stays raw (rare, and a
     * documented limitation alongside cross-model depth-2).
     */
    private static List<Map<String, Object>> crossModelScalars(CrossModelSupport.TargetInfo target) {
        List<Map<String, Object>> scalars = new ArrayList<>();
        java.util.Collection<String> names = target != null && target.propertyNames() != null ? target.propertyNames()
                : List.of(target != null ? target.labelField() : "Name");
        for (String name : names) {
            scalars.add(scalar(name, false));
        }
        return scalars;
    }

    private static Map<String, Object> scalar(String name, boolean date) {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("name", name);
        descriptor.put("date", date);
        return descriptor;
    }

    private static boolean isDateType(String type) {
        return "date".equals(type) || "timestamp".equals(type);
    }

    /** The item's composition FK property (PascalCase) pointing back at the master. */
    private static String masterFkProperty(EntityIntent items, String masterName) {
        for (RelationIntent relation : items.getRelations()) {
            if (isToOne(relation) && relation.isComposition() && masterName.equals(relation.getTo())) {
                return IntentNaming.pascalCase(relation.getName());
            }
        }
        return masterName;
    }

    /**
     * The same-model to-one target's label property: its {@code name} field (PascalCased), or empty
     * when the target has none — the template then omits the {@code __label} put rather than emit an
     * accessor for a field that does not exist (which would fail {@code javac}).
     */
    private static String nameField(EntityIntent target) {
        if (target != null) {
            for (FieldIntent field : target.getFields()) {
                if (field.getName() != null && "name".equalsIgnoreCase(field.getName())) {
                    return IntentNaming.pascalCase(field.getName());
                }
            }
        }
        return "";
    }

    private static UsesIntent findUses(IntentModel model, String alias) {
        for (UsesIntent uses : model.getUses()) {
            if (alias.equals(uses.getModel())) {
                return uses;
            }
        }
        return null;
    }

    private static boolean isToOne(RelationIntent relation) {
        return "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
    }

    /** A readable, unique lower-camel Java variable name for the relation (suffixed on collision). */
    private static String uniqueVar(String relationName, Set<String> usedVars) {
        String base = decapitalize(IntentNaming.pascalCase(relationName));
        String candidate = base;
        int index = 2;
        while (!usedVars.add(candidate)) {
            candidate = base + index++;
        }
        return candidate;
    }

    private static String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
