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
 */
final class SnapshotSupport {

    private SnapshotSupport() {}

    /**
     * One snapshot descriptor per {@code function: Snapshot} child of a document master.
     *
     * @param model the parsed intent model
     * @param byName entities indexed by name
     * @param compositionParents each entity's transitive composition parent (perspective resolution)
     * @return the {@code snapshots} collection (possibly empty)
     */
    static List<Map<String, Object>> buildSnapshots(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents) {
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
            snapshot.put("language", "en");
            snapshot.put("snapshotEntity", entity.getName());
            snapshot.put("snapshotPerspective", IntentEntities.resolvePerspective(entity.getName(), compositionParents));
            snapshot.put("snapshotMasterFk", IntentNaming.pascalCase(masterRelation.getName()));
            snapshots.add(snapshot);
        }
        return snapshots;
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
