/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;

/**
 * Materialises a {@code kind: manyToMany} relation into the <b>intermediate entity</b> the platform
 * has always required authors to write by hand - a link entity holding a {@code composition} to the
 * declaring side and a {@code manyToOne} to the target:
 *
 * <pre>
 * entities:
 *   - name: Order
 *     relations:
 *       - { name: products, kind: manyToMany, to: Product }
 * </pre>
 *
 * becomes, before any validator or generator sees the model:
 *
 * <pre>
 * entities:
 *   - name: Order
 *     relations:
 *       - { name: products, kind: oneToMany, to: OrderProduct }
 *   - name: OrderProduct
 *     fields:
 *       - { name: id, type: integer, primaryKey: true, generated: true }
 *     relations:
 *       - { name: Order,   kind: manyToOne, to: Order, composition: true, required: true }
 *       - { name: Product, kind: manyToOne, to: Product, required: true }
 * </pre>
 *
 * <p>
 * The expansion happens on the typed model at the START of validation, so the link entity is an
 * ordinary entity from that point on - it gets its table, its FK columns, its detail table under
 * the declaring entity's page, and it can be seeded, reported on and referenced like any other.
 * That is the whole point: an n:m link is a real row, and the one thing the DSL must never do is
 * accept {@code manyToMany} and generate nothing (dirigible #6718).
 *
 * <p>
 * The link entity's name is {@code <Declaring><Target>} unless the relation names it with
 * {@code through:}. Bridge data (a quantity, a valid-from date) means the link is a domain entity
 * in its own right - author it explicitly with its own fields and drop the {@code manyToMany}; the
 * relation attributes that describe the target picker ({@code where} / {@code show} / {@code major}
 * / {@code size} / {@code leafOnly}) travel onto the link's target relation, and the ones that only
 * make sense on a hand-authored to-one are rejected rather than silently dropped.
 */
final class ManyToManyExpander {

    private static final String MANY_TO_MANY = "manyToMany";

    private static final String MANY_TO_ONE = "manyToOne";

    private static final String ONE_TO_MANY = "oneToMany";

    private ManyToManyExpander() {}

    /**
     * Rewrite every {@code manyToMany} relation in the model into its link entity, in place.
     *
     * @param model the parsed model
     * @param issues collector for the problems that make an n:m unmaterialisable
     */
    static void expand(IntentModel model, List<String> issues) {
        Set<String> names = new LinkedHashSet<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                names.add(entity.getName());
            }
        }
        Set<String> aliases = new HashSet<>();
        for (UsesIntent uses : model.getUses()) {
            if (uses.getModel() != null) {
                aliases.add(uses.getModel());
            }
        }
        Map<EntityIntent, List<EntityIntent>> links = new LinkedHashMap<>();
        Set<String> reportedPairs = new HashSet<>();
        for (EntityIntent owner : List.copyOf(model.getEntities())) {
            if (owner.getName() == null) {
                continue;
            }
            for (RelationIntent relation : owner.getRelations()) {
                if (!MANY_TO_MANY.equals(relation.getKind()) || isBlank(relation.getName())) {
                    continue;
                }
                EntityIntent link = materialize(model, owner, relation, names, aliases, reportedPairs, issues);
                if (link != null) {
                    names.add(link.getName());
                    links.computeIfAbsent(owner, key -> new ArrayList<>())
                         .add(link);
                }
            }
        }
        // Each link entity is inserted right after the entity that declares the n:m, so the generated
        // .edm reads in the order the author thinks in (owner, its link, the next entity).
        for (Map.Entry<EntityIntent, List<EntityIntent>> entry : links.entrySet()) {
            model.getEntities()
                 .addAll(model.getEntities()
                              .indexOf(entry.getKey())
                         + 1, entry.getValue());
        }
    }

    /**
     * The link entity for one {@code manyToMany}, or {@code null} when the relation cannot be
     * materialised (every reason is reported as an issue, naming the coordinates the author wrote).
     */
    private static EntityIntent materialize(IntentModel model, EntityIntent owner, RelationIntent relation, Set<String> names,
            Set<String> aliases, Set<String> reportedPairs, List<String> issues) {
        String subject = "entity [" + owner.getName() + "] relation [" + relation.getName() + "]";
        String target = relation.getTo();
        if (isBlank(target)) {
            issues.add(subject + " has no target");
            return null;
        }
        if (relation.isCrossModel()) {
            if (!aliases.contains(relation.getModel())) {
                issues.add(subject + " references undeclared model [" + relation.getModel() + "] - add it to uses:");
                return null;
            }
        } else if (!names.contains(target)) {
            issues.add(subject + " points to unknown entity [" + target + "]");
            return null;
        }
        List<String> unsupported = unsupportedAttributes(relation);
        if (!unsupported.isEmpty()) {
            issues.add(subject + " is a manyToMany so it cannot declare " + unsupported
                    + " - those describe a hand-authored to-one; author the intermediate entity explicitly (a composition to ["
                    + owner.getName() + "] plus a manyToOne to [" + target + "]) and put them on its relations");
            return null;
        }
        // A self-referencing n:m (both ends the same entity) is legitimate and is NOT the both-sides
        // mistake - the "other side" the check looks for is the relation itself.
        if (!relation.isCrossModel() && !target.equals(owner.getName()) && declaresManyToManyTo(model, target, owner.getName())) {
            String pair = owner.getName()
                               .compareTo(target) <= 0 ? owner.getName() + "/" + target : target + "/" + owner.getName();
            if (reportedPairs.add(pair)) {
                issues.add("entities [" + owner.getName() + "] and [" + target
                        + "] both declare a manyToMany to each other - an n:m materialises ONE link entity; keep the declaration on the"
                        + " side whose page should own the link lines and drop the other");
            }
            return null;
        }
        String linkName = linkEntityName(owner, relation, target);
        if (names.contains(linkName)) {
            issues.add(subject + " materialises the link entity [" + linkName
                    + "] but an entity with that name is already declared - drop the manyToMany and relate to the declared entity"
                    + " (it is the intermediate entity), or name the link entity with through: <Name>");
            return null;
        }
        EntityIntent link = linkEntity(linkName, owner, relation, target);
        rewriteToNavigation(relation, linkName);
        return link;
    }

    /**
     * The attributes an author may write on a to-one but which have no meaning on an n:m - each one
     * would describe the link's own FK, which only the intermediate entity can carry. Reported together
     * so the author gets the whole list at once.
     */
    private static List<String> unsupportedAttributes(RelationIntent relation) {
        List<String> unsupported = new ArrayList<>();
        if (relation.isComposition()) {
            unsupported.add("composition");
        }
        if (!isBlank(relation.getFunction())) {
            unsupported.add("function");
        }
        if (!isBlank(relation.getInit())) {
            unsupported.add("init");
        }
        if (relation.getDependsOn() != null) {
            unsupported.add("dependsOn");
        }
        if (relation.isCalculated()) {
            unsupported.add("calculatedAction");
        }
        if (relation.isPersonal()) {
            unsupported.add("personal");
        }
        if (relation.isPartner()) {
            unsupported.add("partner");
        }
        return unsupported;
    }

    /** Whether {@code entityName} declares a {@code manyToMany} back to {@code target}. */
    private static boolean declaresManyToManyTo(IntentModel model, String entityName, String target) {
        for (EntityIntent entity : model.getEntities()) {
            if (!entityName.equals(entity.getName())) {
                continue;
            }
            for (RelationIntent relation : entity.getRelations()) {
                if (MANY_TO_MANY.equals(relation.getKind()) && !relation.isCrossModel() && target.equals(relation.getTo())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@code through:} when authored, else {@code <Declaring><Target>} - and, for a self-referencing
     * n:m (both ends the same entity), {@code <Declaring><Relation>}, since the target's name would
     * only repeat the declaring one.
     */
    private static String linkEntityName(EntityIntent owner, RelationIntent relation, String target) {
        if (!isBlank(relation.getThrough())) {
            return relation.getThrough()
                           .trim();
        }
        String suffix = target.equals(owner.getName()) ? IntentNaming.pascalCase(relation.getName()) : target;
        return owner.getName() + suffix;
    }

    /**
     * The link entity: a generated integer key, the composition to the declaring side (so it is managed
     * as a detail of that entity's page, never a top-level perspective of its own) and the association
     * to the target. Both FKs are NOT NULL - a link row that points at only one end is not a link.
     */
    private static EntityIntent linkEntity(String linkName, EntityIntent owner, RelationIntent relation, String target) {
        EntityIntent link = new EntityIntent();
        link.setName(linkName);
        link.setDescription(relation.getDescription());
        FieldIntent id = new FieldIntent();
        id.setName("id");
        id.setType("integer");
        id.setPrimaryKey(true);
        id.setGenerated(true);
        link.getFields()
            .add(id);
        RelationIntent toOwner = new RelationIntent();
        toOwner.setName(owner.getName());
        toOwner.setKind(MANY_TO_ONE);
        toOwner.setTo(owner.getName());
        toOwner.setComposition(true);
        toOwner.setRequired(true);
        RelationIntent toTarget = new RelationIntent();
        toTarget.setName(targetRelationName(owner, relation, target));
        toTarget.setKind(MANY_TO_ONE);
        toTarget.setTo(target);
        toTarget.setRequired(true);
        toTarget.setModel(relation.getModel());
        // The picker attributes describe the target dropdown, which lives on the link row - carry them.
        toTarget.setWhere(relation.getWhere());
        toTarget.setShow(relation.getShow());
        toTarget.setSize(relation.getSize());
        toTarget.setMajor(relation.isMajor());
        toTarget.setLeafOnly(relation.isLeafOnly());
        link.getRelations()
            .add(toOwner);
        link.getRelations()
            .add(toTarget);
        return link;
    }

    /**
     * The link's target-side relation name - the target entity's name, except for a self-referencing
     * n:m where that would collide with the composition's name (both ends being the same entity), in
     * which case the authored relation name plays the role.
     */
    private static String targetRelationName(EntityIntent owner, RelationIntent relation, String target) {
        return target.equals(owner.getName()) ? IntentNaming.pascalCase(relation.getName()) : target;
    }

    /**
     * The authored relation becomes the navigation-only {@code oneToMany} to the link entity, so the
     * model that reaches the validators and generators holds exactly one representation of the n:m. The
     * picker attributes moved onto the link's target relation are cleared here rather than left behind
     * on a kind that ignores them.
     */
    private static void rewriteToNavigation(RelationIntent relation, String linkName) {
        relation.setKind(ONE_TO_MANY);
        relation.setTo(linkName);
        relation.setThrough(null);
        relation.setModel(null);
        relation.setWhere(null);
        relation.setShow(null);
        relation.setSize(null);
        relation.setLeafOnly(false);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
