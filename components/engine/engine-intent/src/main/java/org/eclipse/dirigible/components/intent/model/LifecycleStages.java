/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What a status MEANS to the lifecycle, declared once where the status nomenclature is seeded:
 *
 * <pre>
 * seeds:
 *   - name: sales-invoice-statuses
 *     entity: SalesInvoiceStatus
 *     rows:
 *       - { id: 1, name: DRAFT,     stage: draft }
 *       - { id: 3, name: ISSUED,    stage: live }
 *       - { id: 8, name: CANCELLED, stage: cancelled }
 *       - { id: 9, name: VOIDED,    stage: void }
 * </pre>
 *
 * {@code stage} is a closed vocabulary ({@link #STAGES}) and pure metadata - it is never a column
 * of the seeded table, so it does not appear in the generated CSV. It exists so that a consumer can
 * ask "which rows of this entity are economically live?" without every author hand-writing the same
 * predicate over positional seed ids: a {@code reports:} entry's {@code scope} resolves through it
 * (see {@code ReportIntentGenerator}), which is what keeps a draft or voided document out of a
 * revenue total by construction instead of by convention.
 *
 * <p>
 * Read by both the parser (validating {@code scope} and the vocabulary) and the report generator
 * (emitting the status {@code IN (...)} predicate), so the two cannot drift.
 */
public final class LifecycleStages {

    /** A row nobody has issued yet - visible to its author, not yet economically real. */
    public static final String DRAFT = "draft";
    /** A row that counts: issued, sent, paid - anything in normal circulation. */
    public static final String LIVE = "live";
    /** A row withdrawn before it became live. */
    public static final String CANCELLED = "cancelled";
    /** A row deliberately retired while keeping its number (анулиране) - out of circulation. */
    public static final String VOID = "void";

    /** The closed stage vocabulary a status seed row may declare. */
    public static final Set<String> STAGES = Set.of(DRAFT, LIVE, CANCELLED, VOID);

    /**
     * The explicit opt-out of a report's lifecycle scope - "this report is ABOUT the lifecycle, count
     * every row".
     */
    public static final String SCOPE_ALL = "all";

    /** The seed-row key carrying the stage classification. */
    public static final String STAGE_KEY = "stage";

    private LifecycleStages() {}

    /**
     * The entity's status relation - its {@code function: EntityStatus} to-one, the FK a lifecycle is
     * expressed through.
     *
     * @param entity the entity to inspect, may be {@code null}
     * @return the status relation, or {@code null} when the entity carries no lifecycle
     */
    public static RelationIntent statusRelation(EntityIntent entity) {
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

    /**
     * The stage classification of a status nomenclature: each declared stage mapped to the seed ids
     * carrying it, in seed order. Empty when the entity is not seeded in this model, when no row
     * declares a stage, or when the entity itself declares a {@code stage} property (see
     * {@link #declaresStageProperty(EntityIntent)} - the row key is then data, not metadata).
     *
     * @param model the model whose seeds carry the nomenclature
     * @param statusEntity the status entity's name
     * @return stage to seed ids, never {@code null}
     */
    public static Map<String, List<Integer>> stagesOf(IntentModel model, String statusEntity) {
        Map<String, List<Integer>> stages = new LinkedHashMap<>();
        if (model == null || statusEntity == null) {
            return stages;
        }
        EntityIntent entity = entityByName(model, statusEntity);
        if (entity == null || declaresStageProperty(entity)) {
            return stages;
        }
        String idField = idFieldOf(entity);
        for (SeedIntent seed : model.getSeeds()) {
            if (!statusEntity.equals(seed.getEntity()) || seed.isLanguageSeed()) {
                continue;
            }
            for (Map<String, Object> row : seed.getRows()) {
                String stage = stageOf(row);
                Integer id = integerOf(row.get(idField));
                if (stage != null && id != null) {
                    stages.computeIfAbsent(stage, key -> new ArrayList<>())
                          .add(id);
                }
            }
        }
        return stages;
    }

    /**
     * The status nomenclature as it is seeded in this model: each seed id mapped to the row's
     * {@code name}, in seed order. The id set is what a lifecycle graph, a transition and a workflow
     * setter are validated against; the names are what their error messages read as (an id is
     * positional and means nothing to the reader of a rejection).
     *
     * @param model the model whose seeds carry the nomenclature
     * @param statusEntity the status entity's name
     * @return seed id to row name (the name may be {@code null}), never {@code null}
     */
    public static Map<Integer, String> seededStatuses(IntentModel model, String statusEntity) {
        Map<Integer, String> statuses = new LinkedHashMap<>();
        if (model == null || statusEntity == null) {
            return statuses;
        }
        EntityIntent entity = entityByName(model, statusEntity);
        if (entity == null) {
            return statuses;
        }
        String idField = idFieldOf(entity);
        for (SeedIntent seed : model.getSeeds()) {
            if (!statusEntity.equals(seed.getEntity()) || seed.isLanguageSeed()) {
                continue;
            }
            for (Map<String, Object> row : seed.getRows()) {
                Integer id = integerOf(row.get(idField));
                if (id != null) {
                    Object name = row.get("name");
                    statuses.putIfAbsent(id, name == null ? null
                            : String.valueOf(name)
                                    .trim());
                }
            }
        }
        return statuses;
    }

    /**
     * The stage a seed row declares, normalized, or {@code null} when it declares none or one outside
     * the vocabulary (the parser reports that separately).
     *
     * @param row the seed row
     * @return the stage, or {@code null}
     */
    public static String stageOf(Map<String, Object> row) {
        Object raw = row == null ? null : row.get(STAGE_KEY);
        if (raw == null) {
            return null;
        }
        String stage = String.valueOf(raw)
                             .trim()
                             .toLowerCase(Locale.ROOT);
        return STAGES.contains(stage) ? stage : null;
    }

    /**
     * Whether the entity declares its own {@code stage} field or to-one relation - in which case a seed
     * row's {@code stage} key is that column's DATA and must not be read as lifecycle metadata. Only a
     * status nomenclature has to care; the parser reports the collision there.
     *
     * @param entity the entity to inspect
     * @return whether {@code stage} is a property of the entity
     */
    public static boolean declaresStageProperty(EntityIntent entity) {
        if (entity == null) {
            return false;
        }
        for (FieldIntent field : entity.getFields()) {
            if (STAGE_KEY.equalsIgnoreCase(field.getName())) {
                return true;
            }
        }
        if (entity.getRelations() != null) {
            for (RelationIntent relation : entity.getRelations()) {
                if (STAGE_KEY.equalsIgnoreCase(relation.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The entity's primary-key field name as seed rows key it ({@code id} by convention). */
    private static String idFieldOf(EntityIntent entity) {
        for (FieldIntent field : entity.getFields()) {
            if (field.isPrimaryKey() && field.getName() != null) {
                return field.getName();
            }
        }
        return "id";
    }

    private static EntityIntent entityByName(IntentModel model, String name) {
        for (EntityIntent entity : model.getEntities()) {
            if (name.equals(entity.getName())) {
                return entity;
            }
        }
        return null;
    }

    /**
     * A seed-row id as an int - the YAML round-trip yields a {@code Long}, an authored CSV a String.
     */
    private static Integer integerOf(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value)
                                         .trim());
        } catch (NumberFormatException ex) {
            return null; // a non-numeric id is reported by the entity's own primary-key validation
        }
    }
}
