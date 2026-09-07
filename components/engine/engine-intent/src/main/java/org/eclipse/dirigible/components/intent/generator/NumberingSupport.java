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
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.NumberIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * Builds the {@code numbering} glue collection: one descriptor per {@code number: { stampOn: issue
 * }} field, driving the generated {@code gen/events/<Entity>NumberStamp.java} delegate. The
 * document is created with a UUID placeholder (the uuid auto-fill); this delegate, wired as a
 * {@code delegate:} service task at the issue step, replaces it with the real formatted number -
 * idempotently, so a re-issue after an amend keeps the number.
 *
 * <p>
 * The number is allocated + formatted via {@code sdk.numbering.DocumentNumbers} (the shared
 * per-tenant counter) and written with the targeted {@code updateProperty} (a workflow system
 * write). A partitioned series resolves its partition from the {@code per} FK, falling back to that
 * relation's {@code init:} default when the FK is null (#7101) - the default company is a partition
 * like any other.
 */
final class NumberingSupport {

    private NumberingSupport() {}

    /**
     * One numbering descriptor per {@code stampOn: issue} number field in the model.
     *
     * @param model the parsed intent model
     * @param compositionParents each entity's transitive composition parent (perspective resolution)
     * @return the {@code numbering} collection (possibly empty)
     */
    static List<Map<String, Object>> buildNumbering(IntentModel model, Map<String, String> compositionParents) {
        List<Map<String, Object>> numbering = new ArrayList<>();
        for (EntityIntent entity : model.getEntities()) {
            for (FieldIntent field : entity.getFields()) {
                NumberIntent number = field.getNumber();
                if (number == null || !"issue".equalsIgnoreCase(number.getStampOn())) {
                    continue; // stampOn:create is handled at insert by the DAO; only issue needs a step
                }
                Map<String, Object> descriptor = new LinkedHashMap<>();
                descriptor.put("entity", entity.getName());
                descriptor.put("perspective", IntentEntities.resolvePerspective(entity.getName(), compositionParents, model));
                descriptor.put("masterPk", IntentEntities.keyFieldName(entity));
                descriptor.put("field", IntentNaming.pascalCase(field.getName()));
                descriptor.put("series", number.getSeries() == null ? entity.getName() : number.getSeries());
                // The partition FK property the stamp reads off the entity ("" = tenant-wide series).
                String per = number.getPer() == null || number.getPer()
                                                              .isBlank() ? "" : IntentNaming.pascalCase(number.getPer());
                descriptor.put("per", per);
                // The partition the stamp falls back to when the FK is null on the loaded row: the
                // relation's `init:` default (#7101) - the same value the create-time allocator uses,
                // so both stamp paths resolve the default company to ITS partition, never the base row.
                descriptor.put("perDefault", partitionDefault(entity, per));
                numbering.add(descriptor);
            }
        }
        return numbering;
    }

    /**
     * The {@code init:} of the entity's relation named {@code per} ("" when the relation carries none):
     * the value the partition FK WILL hold on a row that left it unset.
     */
    private static String partitionDefault(EntityIntent entity, String per) {
        if (per.isEmpty()) {
            return "";
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (relation.getName() != null && per.equals(IntentNaming.pascalCase(relation.getName()))) {
                return relation.getInit() == null ? ""
                        : relation.getInit()
                                  .trim();
            }
        }
        return "";
    }
}
