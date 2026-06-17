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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared resolution of entity-graph facts that more than one generator needs to agree on - chiefly
 * the <b>perspective</b> an entity resolves to (its own, or its transitive composition parent's)
 * and its primary-key property name. The EDM generator and the glue generator must compute these
 * identically: the glue's {@code @Listener} topic and the entity's generated DAO publish topic both
 * key on the same perspective, and a divergence would silently break event delivery.
 */
public final class IntentEntities {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentEntities.class);

    private IntentEntities() {}

    /** Entities indexed by name. */
    public static Map<String, EntityIntent> byName(IntentModel model) {
        Map<String, EntityIntent> index = new HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                index.put(entity.getName(), entity);
            }
        }
        return index;
    }

    /**
     * Each entity mapped to its composition parent (the target of its first {@code composition: true}
     * to-one relation), if any. A DEPENDENT entity is managed under its parent's perspective.
     */
    public static Map<String, String> compositionParents(IntentModel model) {
        Map<String, String> parents = new LinkedHashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() == null) {
                continue;
            }
            for (RelationIntent relation : entity.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && relation.isComposition() && relation.getTo() != null) {
                    parents.put(entity.getName(), relation.getTo());
                    break;
                }
            }
        }
        return parents;
    }

    /** The perspective an entity belongs to: itself, or its transitive composition parent. */
    public static String resolvePerspective(String entityName, Map<String, String> compositionParents) {
        String current = entityName;
        Set<String> visited = new LinkedHashSet<>();
        while (compositionParents.containsKey(current)) {
            if (!visited.add(current)) {
                LOGGER.warn("Composition cycle detected at entity [{}] - keeping its own perspective", entityName);
                return entityName;
            }
            current = compositionParents.get(current);
        }
        return current;
    }

    /** The entity's primary-key field, or null when none is declared. */
    public static FieldIntent primaryKeyOf(EntityIntent entity) {
        if (entity == null) {
            return null;
        }
        for (FieldIntent field : entity.getFields()) {
            if (field.isPrimaryKey() && field.getName() != null) {
                return field;
            }
        }
        return null;
    }

    /** The PascalCase name of the entity's primary-key property (defaults to {@code Id}). */
    public static String keyFieldName(EntityIntent entity) {
        FieldIntent pk = primaryKeyOf(entity);
        return pk == null ? "Id" : IntentNaming.pascalCase(pk.getName());
    }
}
