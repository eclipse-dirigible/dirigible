/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.eclipse.dirigible.components.intent.model.ReportUnionMemberIntent;

/**
 * The logical source a report's {@code union:} reads (dirigible #7946): one entity, named after the
 * report, whose fields are the columns every member maps - {@code source}, {@code counterparty},
 * {@code date}, {@code due} and {@code amount}, plus the to-one {@code columns:} its members pass
 * through (dirigible #8619). The parser validates a union report's dimensions, measures and
 * parameters against it and the generator resolves them against it, so a union report is an
 * ordinary report over that entity - its table being the generated {@code UNION ALL} view.
 */
public final class ReportUnion {

    /** The member's entity name - which document type a row is. */
    public static final String SOURCE = "source";
    /** The to-one relation every member maps onto the same target. */
    public static final String COUNTERPARTY = "counterparty";
    /** The document date. */
    public static final String DATE = "date";
    /** The due date. */
    public static final String DUE = "due";
    /** The signed amount. */
    public static final String AMOUNT = "amount";

    private ReportUnion() {}

    /**
     * Whether any member declares the column the accessor reads. A logical column is declared by every
     * member or by none - the parser refuses anything else - so this is also whether the union has it.
     *
     * @param report the union report
     * @param column the member's mapping of the column
     * @return whether the column exists on the union
     */
    public static boolean declares(ReportIntent report, Function<ReportUnionMemberIntent, String> column) {
        return report.getUnion()
                     .stream()
                     .anyMatch(member -> member != null && !isBlank(column.apply(member)));
    }

    /**
     * The union's to-one columns, in the order the view selects them: {@code counterparty} when the
     * members map it, then the pass-through {@code columns:} (dirigible #8619) in the order they are
     * first declared.
     *
     * @param report the union report
     * @return the logical names of the to-one columns
     */
    public static List<String> toOneColumns(ReportIntent report) {
        Set<String> names = new LinkedHashSet<>();
        if (declares(report, ReportUnionMemberIntent::getCounterparty)) {
            names.add(COUNTERPARTY);
        }
        for (ReportUnionMemberIntent member : report.getUnion()) {
            if (member != null) {
                for (String name : member.getColumns()
                                         .keySet()) {
                    if (!isBlank(name)) {
                        names.add(name.trim());
                    }
                }
            }
        }
        return new ArrayList<>(names);
    }

    /**
     * How a member maps one of the union's to-one columns - its {@code counterparty}, or the entry of
     * its {@code columns:} - or null when it does not.
     *
     * @param member the member
     * @param column the logical column
     * @return the member's path, trimmed, or null
     */
    public static String mapping(ReportUnionMemberIntent member, String column) {
        String path = COUNTERPARTY.equals(column) ? member.getCounterparty()
                : member.getColumns()
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey() != null && column.equals(entry.getKey()
                                                                                      .trim()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
        return isBlank(path) ? null : path.trim();
    }

    /**
     * The to-one relation a member path names: the entity's own to-one, or - one hop away - a to-one of
     * the entity a SAME-model to-one of it points at ({@code SalesInvoice.Customer} on an allocation
     * row, dirigible #8619). The hop's target is read from this model, so a hop into another model is
     * no path here.
     *
     * @param model the intent model
     * @param entity the member entity
     * @param path the member's path
     * @return the relation the path ends in, or null
     */
    public static RelationIntent toOnePath(IntentModel model, EntityIntent entity, String path) {
        if (entity == null || isBlank(path)) {
            return null;
        }
        String[] segments = path.trim()
                                .split("\\.");
        if (segments.length == 1) {
            return toOne(entity, segments[0]);
        }
        if (segments.length != 2) {
            return null;
        }
        RelationIntent hop = toOne(entity, segments[0]);
        if (hop == null || hop.isCrossModel() || hop.getTo() == null) {
            return null;
        }
        EntityIntent target = entity(model, hop.getTo());
        return target == null ? null : toOne(target, segments[1]);
    }

    /**
     * The counterparty relation of the first SAME-model member that names one - the relation whose
     * target the union's {@code counterparty} points at - or {@code null} when no same-model member
     * names a to-one relation of that name.
     *
     * @param model the intent model
     * @param report the union report
     * @return the relation, or null
     */
    public static RelationIntent sameModelCounterparty(IntentModel model, ReportIntent report) {
        return sameModelRelation(model, report, COUNTERPARTY);
    }

    /**
     * The relation one of the union's to-one columns resolves to on the first SAME-model member that
     * maps it to a to-one path, or {@code null} when none does.
     *
     * @param model the intent model
     * @param report the union report
     * @param column the logical column
     * @return the relation, or null
     */
    public static RelationIntent sameModelRelation(IntentModel model, ReportIntent report, String column) {
        for (ReportUnionMemberIntent member : report.getUnion()) {
            if (member == null || member.isCrossModel() || isBlank(member.getSource())) {
                continue;
            }
            RelationIntent relation = toOnePath(model, entity(model, member.getSource()
                                                                           .trim()),
                    mapping(member, column));
            if (relation != null && relation.getTo() != null) {
                return relation;
            }
        }
        return null;
    }

    /**
     * The entity one of the union's to-one columns points at, and the {@code uses:} alias owning it
     * (null for this model's own).
     */
    public record Target(String to, String model) {
    }

    /**
     * The union's logical entity, with a to-one relation for {@code counterparty}.
     *
     * @param report the union report
     * @param counterpartyTo the entity {@code counterparty} points at, or null when the union declares
     *        none or its target is not known here
     * @param counterpartyModel the {@code uses:} alias owning that entity, or null for this model
     * @return the entity, named after the report
     */
    public static EntityIntent entity(ReportIntent report, String counterpartyTo, String counterpartyModel) {
        Map<String, Target> targets = new LinkedHashMap<>();
        if (counterpartyTo != null) {
            targets.put(COUNTERPARTY, new Target(counterpartyTo, counterpartyModel));
        }
        return entity(report, targets);
    }

    /**
     * The union's logical entity.
     *
     * @param report the union report
     * @param targets what each of its to-one columns points at, by logical name - a column missing here
     *        (its target is not known here) has no relation on the entity
     * @return the entity, named after the report
     */
    public static EntityIntent entity(ReportIntent report, Map<String, Target> targets) {
        EntityIntent entity = new EntityIntent();
        entity.setName(report.getName());
        List<FieldIntent> fields = new ArrayList<>();
        fields.add(field(SOURCE, "string", true));
        if (declares(report, ReportUnionMemberIntent::getDate)) {
            fields.add(field(DATE, "date", false));
        }
        if (declares(report, ReportUnionMemberIntent::getDue)) {
            fields.add(field(DUE, "date", false));
        }
        fields.add(field(AMOUNT, "decimal", false));
        entity.setFields(fields);
        List<RelationIntent> relations = new ArrayList<>();
        for (String column : toOneColumns(report)) {
            Target target = targets.get(column);
            if (target != null) {
                RelationIntent relation = new RelationIntent();
                relation.setName(column);
                relation.setKind("manyToOne");
                relation.setTo(target.to());
                relation.setModel(target.model());
                relations.add(relation);
            }
        }
        if (!relations.isEmpty()) {
            entity.setRelations(relations);
        }
        return entity;
    }

    private static FieldIntent field(String name, String type, boolean required) {
        FieldIntent field = new FieldIntent();
        field.setName(name);
        field.setType(type);
        field.setRequired(required);
        return field;
    }

    private static EntityIntent entity(IntentModel model, String name) {
        for (EntityIntent entity : model.getEntities()) {
            if (name.equals(entity.getName())) {
                return entity;
            }
        }
        return null;
    }

    private static RelationIntent toOne(EntityIntent entity, String name) {
        for (RelationIntent relation : entity.getRelations()) {
            if (name.equals(relation.getName()) && ("manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind()))) {
                return relation;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
