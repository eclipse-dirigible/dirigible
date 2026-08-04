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

import org.eclipse.dirigible.components.intent.generator.print.PrintIntentGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * Builds the {@code snapshots} glue collection: one descriptor per {@code function: Snapshot} child
 * whose master is a document (header-items) master, driving the generated
 * {@code gen/events/<module>/<Master>SnapshotGenerator.java} delegate. Wired into a process as a
 * {@code delegate:} service task ({@code delegate: gen.events.<Master>SnapshotGenerator},
 * module-scoped by the BPMN generator) so an immutable printed copy of the document is rendered and
 * stored on issue - the number stays across amendments, only the snapshot {@code Version}
 * increments.
 *
 * <p>
 * The delegate reuses the master's generated {@code PrintFeeder} (same module-scoped events
 * package) to assemble the {@code {document, items}} payload, renders it server-side via
 * {@code sdk.print.Print}, and stores the PDF via {@code sdk.cms.Attachments} - so only a document
 * master (which has a feeder) can carry a snapshot child.
 *
 * <p>
 * The render <b>language</b> is a per-snapshot knob: a literal {@code language:} code, a
 * {@code languageFrom: relation.field} path on the master (the customer decides the invoice's
 * language), or - absent both - the first entry of the tenant-resolved application language set,
 * read at mint time via {@code sdk.print.Print.defaultLanguage()}. All three shapes are
 * pre-rendered here as a Java expression (the expansions convention - the template stays
 * shape-only).
 */
final class SnapshotSupport {

    /** The run-time fallback: the first entry of the tenant-resolved application language set. */
    private static final String DEFAULT_LANGUAGE_EXPRESSION = NotifySupport.DEFAULT_LANGUAGE_EXPRESSION;

    private SnapshotSupport() {}

    /**
     * One snapshot descriptor per {@code function: Snapshot} child of a document master.
     *
     * @param model the parsed intent model
     * @param byName entities indexed by name
     * @param compositionParents each entity's transitive composition parent (perspective resolution)
     * @param crossModel resolver for a cross-model {@code languageFrom} relation, or {@code null}
     * @return the {@code snapshots} collection (possibly empty)
     */
    static List<Map<String, Object>> buildSnapshots(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, NotificationSupport.CrossModelLookup crossModel) {
        Set<String> documentMasters = new LinkedHashSet<>();
        for (EntityIntent master : PrintIntentGenerator.documentMasters(model)
                                                       .keySet()) {
            documentMasters.add(master.getName());
        }
        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (EntityIntent entity : model.getEntities()) {
            if (!entity.isSnapshot()) {
                continue;
            }
            RelationIntent masterRelation = compositionMaster(entity);
            if (masterRelation == null || !documentMasters.contains(masterRelation.getTo())) {
                continue; // a snapshot needs a document master (with a PrintFeeder) to render from
            }
            EntityIntent master = byName.get(masterRelation.getTo());
            if (master == null) {
                continue;
            }
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("master", master.getName());
            snapshot.put("masterPk", IntentEntities.keyFieldName(master));
            snapshot.put("masterPerspective", IntentEntities.resolvePerspective(master.getName(), compositionParents));
            putLanguage(snapshot, entity, master, byName, compositionParents, crossModel);
            snapshot.put("snapshotEntity", entity.getName());
            snapshot.put("snapshotPerspective", IntentEntities.resolvePerspective(entity.getName(), compositionParents));
            snapshot.put("snapshotMasterFk", IntentNaming.pascalCase(masterRelation.getName()));
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    /**
     * The render-language keys, always present (empty strings when not applicable - an undefined
     * Velocity variable renders as its own literal name, so the template must never depend on a key
     * being absent): {@code languageExpression} is the Java expression the delegate assigns, and the
     * {@code languageFkProperty} / {@code languageTarget*} keys carry the {@code languageFrom} load
     * coordinates.
     */
    private static void putLanguage(Map<String, Object> snapshot, EntityIntent entity, EntityIntent master,
            Map<String, EntityIntent> byName, Map<String, String> compositionParents, NotificationSupport.CrossModelLookup crossModel) {
        snapshot.put("languageFkProperty", "");
        snapshot.put("languageTargetEntity", "");
        snapshot.put("languageTargetPerspective", "");
        snapshot.put("languageTargetModel", "");
        String literal = entity.getLanguage();
        if (literal != null && !literal.isBlank()) {
            snapshot.put("languageExpression", "\"" + literal.trim() + "\"");
            return;
        }
        String path = entity.getLanguageFrom();
        if (path == null || path.isBlank()) {
            snapshot.put("languageExpression", DEFAULT_LANGUAGE_EXPRESSION);
            return;
        }
        int dot = path.indexOf('.');
        if (dot < 0) {
            throw new IllegalStateException("languageFrom [" + path + "] of snapshot [" + entity.getName()
                    + "] must be a relation.field path on the master [" + master.getName() + "]");
        }
        String relationName = path.substring(0, dot)
                                  .trim();
        String fieldName = path.substring(dot + 1)
                               .trim();
        RelationIntent relation = toOneRelation(master, relationName);
        if (relation == null) {
            // The parser validates the shape; an unresolvable relation here means the intent changed
            // underneath the knob - fail the pass loudly rather than minting wrong-language copies.
            throw new IllegalStateException("languageFrom [" + path + "] of snapshot [" + entity.getName() + "]: [" + relationName
                    + "] is not a to-one relation of its master [" + master.getName() + "]");
        }
        String pascalField = IntentNaming.pascalCase(fieldName);
        boolean isCrossModel = relation.getModel() != null && !relation.getModel()
                                                                       .isBlank();
        if (isCrossModel) {
            NotificationSupport.CrossModelTarget target = crossModel == null ? null : crossModel.resolve(relation);
            if (target == null || (target.propertyNames() != null && !target.propertyNames()
                                                                            .contains(pascalField))) {
                throw new IllegalStateException("languageFrom [" + path + "] of snapshot [" + entity.getName() + "]: [" + fieldName
                        + "] could not be resolved on the cross-model target [" + relation.getTo() + "] of model [" + relation.getModel()
                        + "]");
            }
            snapshot.put("languageTargetPerspective", target.perspectiveName());
            snapshot.put("languageTargetModel", target.modelAlias());
        } else {
            EntityIntent target = byName.get(relation.getTo());
            if (target == null || stringField(target, fieldName) == null) {
                throw new IllegalStateException("languageFrom [" + path + "] of snapshot [" + entity.getName() + "]: [" + fieldName
                        + "] is not a string field of [" + relation.getTo() + "]");
            }
            snapshot.put("languageTargetPerspective",
                    target.isSetting() ? "Settings" : IntentEntities.resolvePerspective(relation.getTo(), compositionParents));
        }
        snapshot.put("languageFkProperty", IntentNaming.pascalCase(relationName));
        snapshot.put("languageTargetEntity", relation.getTo());
        snapshot.put("languageExpression", "languageSource == null || languageSource." + pascalField + " == null || languageSource."
                + pascalField + ".isBlank() ? " + DEFAULT_LANGUAGE_EXPRESSION + " : languageSource." + pascalField + ".trim()");
    }

    private static RelationIntent toOneRelation(EntityIntent entity, String name) {
        for (RelationIntent relation : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name.equals(relation.getName()) && relation.getTo() != null) {
                return relation;
            }
        }
        return null;
    }

    private static FieldIntent stringField(EntityIntent entity, String name) {
        for (FieldIntent field : entity.getFields()) {
            if (name.equals(field.getName()) && (field.getType() == null || "string".equals(field.getType())
                    || "text".equals(field.getType()) || "uuid".equals(field.getType()))) {
                return field;
            }
        }
        return null;
    }

    /** The snapshot's composition to-one relation back to its master (the owning document). */
    private static RelationIntent compositionMaster(EntityIntent snapshot) {
        for (RelationIntent relation : snapshot.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && relation.isComposition() && relation.getTo() != null) {
                return relation;
            }
        }
        return null;
    }
}
