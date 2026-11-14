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

import java.util.List;
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
 * {@code date}, {@code due} and {@code amount}. The parser validates a union report's dimensions,
 * measures and parameters against it and the generator resolves them against it, so a union report
 * is an ordinary report over that entity - its table being the generated {@code UNION ALL} view.
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
     * The counterparty relation of the first SAME-model member that names one - the relation whose
     * target the union's {@code counterparty} points at - or {@code null} when no same-model member
     * names a to-one relation of that name.
     *
     * @param model the intent model
     * @param report the union report
     * @return the relation, or null
     */
    public static RelationIntent sameModelCounterparty(IntentModel model, ReportIntent report) {
        for (ReportUnionMemberIntent member : report.getUnion()) {
            if (member == null || member.isCrossModel() || isBlank(member.getCounterparty()) || isBlank(member.getSource())) {
                continue;
            }
            EntityIntent entity = entity(model, member.getSource()
                                                      .trim());
            RelationIntent relation = entity == null ? null
                    : toOne(entity, member.getCounterparty()
                                          .trim());
            if (relation != null && relation.getTo() != null) {
                return relation;
            }
        }
        return null;
    }

    /**
     * The union's logical entity.
     *
     * @param report the union report
     * @param counterpartyTo the entity {@code counterparty} points at, or null when the union declares
     *        none or its target is not known here
     * @param counterpartyModel the {@code uses:} alias owning that entity, or null for this model
     * @return the entity, named after the report
     */
    public static EntityIntent entity(ReportIntent report, String counterpartyTo, String counterpartyModel) {
        EntityIntent entity = new EntityIntent();
        entity.setName(report.getName());
        List<FieldIntent> fields = new java.util.ArrayList<>();
        fields.add(field(SOURCE, "string", true));
        if (declares(report, ReportUnionMemberIntent::getDate)) {
            fields.add(field(DATE, "date", false));
        }
        if (declares(report, ReportUnionMemberIntent::getDue)) {
            fields.add(field(DUE, "date", false));
        }
        fields.add(field(AMOUNT, "decimal", false));
        entity.setFields(fields);
        if (counterpartyTo != null && declares(report, ReportUnionMemberIntent::getCounterparty)) {
            RelationIntent relation = new RelationIntent();
            relation.setName(COUNTERPARTY);
            relation.setKind("manyToOne");
            relation.setTo(counterpartyTo);
            relation.setModel(counterpartyModel);
            entity.setRelations(new java.util.ArrayList<>(List.of(relation)));
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
