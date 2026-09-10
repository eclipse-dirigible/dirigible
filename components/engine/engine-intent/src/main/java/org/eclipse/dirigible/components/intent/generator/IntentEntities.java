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


    /**
     * The document-items child of {@code master}: the composition child that carries the document's
     * LINES. It is the one answer to "what are this document's items" that every consumer must give -
     * the document (header-items) layout, a document-level {@code checks:} gate, a {@code postings:}
     * {@code creates:} target and the print feeder - because a master can own several composition
     * children (an invoice also owns its payment allocations, its promotions, its printed
     * {@code function: Snapshot} copies and its reminders) and only one of them is its lines.
     * <p>
     * Resolution, in entity-declaration order so the answer never depends on hash order:
     * <ol>
     * <li>a child flagged {@code function: DocumentItem} - the authored answer;</li>
     * <li>else the legacy {@code *Item}-named child ({@code SalesInvoice} -&gt;
     * {@code SalesInvoiceItem});</li>
     * <li>else the SOLE composition child - a master with exactly one child has no ambiguity to
     * resolve;</li>
     * <li>else the first composition child in declaration order - deterministic, and the author
     * disambiguates by flagging the lines child.</li>
     * </ol>
     * This is the same preference {@code EdmIntentGenerator.documentMasters} applies to pick the lines
     * table of a document layout; resolving a check against a different child made the same authored
     * document give two different answers, and a guard on a multi-child document counted printed
     * snapshots instead of lines (#7027).
     *
     * @param master the document entity's name
     * @param entities every entity of the model, in declaration order
     * @return the items child, or {@code null} when the master owns no composition child
     */
    public static EntityIntent documentItemsChild(String master, Collection<EntityIntent> entities) {
        if (master == null || entities == null) {
            return null;
        }
        EntityIntent flagged = null;
        EntityIntent named = null;
        EntityIntent first = null;
        int children = 0;
        for (EntityIntent candidate : entities) {
            if (candidate.getName() == null || !master.equals(compositionParentOf(candidate))) {
                continue;
            }
            children++;
            if (first == null) {
                first = candidate;
            }
            if (flagged == null && candidate.isDocumentItem()) {
                flagged = candidate;
            }
            if (named == null && candidate.getName()
                                          .endsWith("Item")) {
                named = candidate;
            }
        }
        if (flagged != null) {
            return flagged;
        }
        if (named != null) {
            return named;
        }
        return children == 0 ? null : first;
    }

    /**
     * The model's DOCUMENT MASTERS - each header entity whose generated repository resums its totals
     * from its lines on every write - mapped to that line-items entity.
     *
     * <p>
     * This is the rule the EDM generator emits the document layout by ({@code MANAGE_DOCUMENT} plus
     * {@code documentItemsEntity}), and so the rule the generation pipeline builds the DAO's
     * {@code documentMaster} from: such a master's {@code save()} and {@code update()} end in
     * {@code recalculate()}, which sets every {@code aggregate: true} header column the lines also
     * declare to the sum over those lines, and the lines' own writes resum the master. A consumer
     * asking "does the write overwrite this header column?" must resolve the master through THIS rule,
     * not through {@link #documentItemsChild} - that is the broader "what are this document's items"
     * answer a check or a posting reads its lines through, and it also names a sole or first
     * composition child whose master the pipeline never treats as a document (dirigible #7234).
     *
     * <p>
     * Two passes, both in entity-declaration order so the first {@code *Item} child wins
     * deterministically when a master has several: a composition child that is the document's line
     * items - explicit {@code function: DocumentItem}, or the legacy {@code *Item} naming - makes its
     * composition parent a master; then {@code function: Document} on a master whose SINGLE composition
     * child is neither flagged nor {@code *Item}-named makes that sole child the items (the author
     * declared the document intent).
     *
     * @param entities every entity of the model, in declaration order
     * @param compositionParents the composition-parent map ({@link #compositionParents(IntentModel)})
     * @return each document master's name mapped to its line-items entity's name
     */
    public static Map<String, String> documentMasters(Collection<EntityIntent> entities, Map<String, String> compositionParents) {
        Map<String, String> masters = new LinkedHashMap<>();
        for (EntityIntent entity : entities) {
            String child = entity.getName();
            if (child == null) {
                continue;
            }
            String parent = compositionParents.get(child);
            if (parent == null) {
                continue;
            }
            if ((entity.isDocumentItem() || child.endsWith("Item")) && !masters.containsKey(parent)) {
                masters.put(parent, child);
            }
        }
        for (EntityIntent entity : entities) {
            String name = entity.getName();
            if (name == null || !entity.isDocument() || masters.containsKey(name)) {
                continue;
            }
            String sole = soleCompositionChild(entities, compositionParents, name);
            if (sole != null) {
                masters.put(name, sole);
            }
        }
        return masters;
    }

    /** The single composition child of {@code master}, or {@code null} when it has zero or several. */
    private static String soleCompositionChild(Collection<EntityIntent> entities, Map<String, String> compositionParents, String master) {
        String only = null;
        for (EntityIntent entity : entities) {
            if (entity.getName() != null && master.equals(compositionParents.get(entity.getName()))) {
                if (only != null) {
                    return null; // ambiguous - more than one composition child
                }
                only = entity.getName();
            }
        }
        return only;
    }

    /**
     * The composition parent of {@code entity}: the target of its first {@code composition: true}
     * to-one relation, or {@code null} when it is not a dependent entity. The single-entity form of
     * {@link #compositionParents(IntentModel)}.
     *
     * @param entity the entity
     * @return the parent entity's name, or {@code null}
     */
    public static String compositionParentOf(EntityIntent entity) {
        if (entity == null || entity.getRelations() == null) {
            return null;
        }
        for (RelationIntent relation : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && relation.isComposition() && relation.getTo() != null) {
                return relation.getTo();
            }
        }
        return null;
    }

    /**
     * The items child's composition FK property (PascalCase) pointing back at {@code master} - the
     * column a per-document query filters on.
     *
     * @param items the items child
     * @param master the document entity's name
     * @return the PascalCase FK property, or {@code null}
     */
    public static String itemsBackReference(EntityIntent items, String master) {
        if (items == null || items.getRelations() == null) {
            return null;
        }
        for (RelationIntent relation : items.getRelations()) {
            if (relation.isComposition() && master.equals(relation.getTo()) && relation.getName() != null) {
                return IntentNaming.pascalCase(relation.getName());
            }
        }
        return null;
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

    /**
     * The entity's {@code function: EntityStatus} relation - the to-one FK its lifecycle is written to
     * - or {@code null} when the entity declares none.
     *
     * @param entity the entity, may be {@code null}
     * @return the status relation, or {@code null}
     */
    public static RelationIntent entityStatusRelation(EntityIntent entity) {
        if (entity == null || entity.getRelations() == null) {
            return null;
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (relation.isEntityStatus()) {
                return relation;
            }
        }
        return null;
    }

    /** The PascalCase name of the entity's primary-key property (defaults to {@code Id}). */
    public static String keyFieldName(EntityIntent entity) {
        FieldIntent pk = primaryKeyOf(entity);
        return pk == null ? "Id" : IntentNaming.pascalCase(pk.getName());
    }

    /**
     * The SQL type an authored field {@code type:} becomes - the one mapping the whole pipeline reads.
     *
     * <p>
     * It is the EDM's {@code dataType}, and through it the generated Java class of the column, which is
     * what tells a default that has a Java stand-in from one the database alone can apply. Two
     * generators deciding that independently is how a defaulted column came to read as changed on every
     * posting redelivery (#7131), so there is one answer here rather than a copy per caller.
     *
     * @param intentType the authored {@code type:}, or {@code null}
     * @return the SQL type, {@code VARCHAR} for anything unrecognized (its widest stand-in)
     */
    public static String sqlType(String intentType) {
        if (intentType == null) {
            return "VARCHAR";
        }
        switch (intentType.trim()
                          .toLowerCase(java.util.Locale.ROOT)) {
            case "integer":
            case "int":
                return "INTEGER";
            case "long":
                return "BIGINT";
            case "decimal":
            case "double":
                return "DECIMAL";
            case "boolean":
                return "BOOLEAN";
            case "date":
                return "DATE";
            case "timestamp":
                return "TIMESTAMP";
            case "text": // a wide VARCHAR, not a CLOB - see TEXT_LENGTH
            case "uuid":
            case "string":
            case "month": // stored as the picker's YYYY-MM string
            case "week": // stored as the picker's YYYY-Www ISO-week string
            default:
                return "VARCHAR";
        }
    }
}
