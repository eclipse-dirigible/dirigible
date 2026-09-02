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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.LoggedValue;
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

    /** The global perspective every {@code function: Setting} entity is generated under. */
    public static final String SETTINGS_PERSPECTIVE = "Settings";

    /**
     * Names of the model's entities declared as settings / nomenclature.
     *
     * @param entities the entities to scan
     * @return the setting entity names
     */
    public static Set<String> settingEntities(Collection<EntityIntent> entities) {
        Set<String> settings = new LinkedHashSet<>();
        for (EntityIntent entity : entities) {
            if (entity.getName() != null && entity.isSetting()) {
                settings.add(entity.getName());
            }
        }
        return settings;
    }

    /**
     * The perspective an entity's GENERATED artifacts live under: the global {@code Settings}
     * perspective for a setting entity, otherwise the entity itself or its transitive composition
     * parent. This is the resolution every emission that names a package, an import, a controller URL
     * or an event topic must use - a setting entity's DAO/controller are generated under
     * {@code data/settings} / {@code api/settings} and its events publish on the
     * {@code <project>-Settings-<entity>} topic, so a settings-unaware resolution produces imports of a
     * non-existent package and topic bindings nothing ever publishes to.
     *
     * @param entityName the entity (or relation target) to resolve
     * @param compositionParents the composition-parent map ({@link #compositionParents(IntentModel)})
     * @param settingEntities the setting entity names ({@link #settingEntities(Collection)})
     * @return the perspective name
     */
    public static String resolvePerspective(String entityName, Map<String, String> compositionParents, Set<String> settingEntities) {
        if (settingEntities.contains(entityName)) {
            return SETTINGS_PERSPECTIVE;
        }
        return compositionPerspective(entityName, compositionParents);
    }

    /**
     * Convenience overload of {@link #resolvePerspective(String, Map, Set)} deriving the setting set
     * from the model.
     *
     * @param entityName the entity (or relation target) to resolve
     * @param compositionParents the composition-parent map
     * @param model the intent model the entity belongs to
     * @return the perspective name
     */
    public static String resolvePerspective(String entityName, Map<String, String> compositionParents, IntentModel model) {
        return resolvePerspective(entityName, compositionParents, settingEntities(model.getEntities()));
    }

    /**
     * The raw composition walk - itself, or the transitive composition parent - DELIBERATELY
     * settings-unaware. Role naming keys on it (a setting entity's generated roles stay named by the
     * entity, not by the shared {@code Settings} perspective). Anything that emits packages, URLs or
     * topics must use {@link #resolvePerspective(String, Map, Set)} instead.
     *
     * @param entityName the entity to resolve
     * @param compositionParents the composition-parent map
     * @return the composition-resolved perspective
     */
    public static String compositionPerspective(String entityName, Map<String, String> compositionParents) {
        String current = entityName;
        Set<String> visited = new LinkedHashSet<>();
        while (compositionParents.containsKey(current)) {
            if (!visited.add(current)) {
                LOGGER.warn("Composition cycle detected at entity [{}] - keeping its own perspective", LoggedValue.of(entityName));
                return entityName;
            }
            current = compositionParents.get(current);
        }
        return current;
    }

    /** The entity's primary-key field, or null when none is declared. */
    /**
     * The property a to-one target's records are LABELED by, resolving broader than the authored
     * {@code name} field so a document back-reference labels as its number: (1) an authored
     * {@code name} field; (2) the stored {@code Name} a {@code label:} expression generates; (3) the
     * {@code function: DocumentTitle} field - a document's human identity (its number). Empty when
     * nothing resolves - the caller then omits the {@code __label} put / the scaffold field rather than
     * reference a value that cannot exist.
     *
     * @param target the relation's target entity, may be {@code null}
     * @return the PascalCase label property, or {@code ""}
     */
    public static String labelFieldOf(EntityIntent target) {
        if (target == null) {
            return "";
        }
        for (FieldIntent field : target.getFields()) {
            if (field.getName() != null && "name".equalsIgnoreCase(field.getName())) {
                return IntentNaming.pascalCase(field.getName());
            }
        }
        if (target.getLabel() != null && !target.getLabel()
                                                .isBlank()) {
            return "Name"; // the stored, repository-recomputed label property the expression generates
        }
        for (FieldIntent field : target.getFields()) {
            if (field.isDocumentTitle() && field.getName() != null) {
                return IntentNaming.pascalCase(field.getName());
            }
        }
        return "";
    }

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
