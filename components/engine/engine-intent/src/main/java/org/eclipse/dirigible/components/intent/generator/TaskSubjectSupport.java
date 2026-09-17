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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * What a record of the trigger entity is CALLED where it is listed away from its own application -
 * an Inbox row, a notification. A task row read {@code Sales Invoice Approval - Approve - Ref 6}:
 * the BPM business key, which defaults to the primary key, so the approver had to open every task
 * to learn which customer and which amount they were approving (issue #7077).
 * <p>
 * The subject is DERIVED, not authored: an entity already declares what identifies its records, and
 * a second, parallel declaration could only drift from the list its own application renders. Up to
 * three properties are picked by the role they play -
 * <ol>
 * <li>what the record is CALLED - {@link IntentEntities#labelFieldOf(EntityIntent)} (an authored
 * {@code name} field, the stored {@code label:} expression, or the document's number),</li>
 * <li>who it is ABOUT - its first {@code major} to-one relation that is not the status badge,</li>
 * <li>how MUCH it is - the {@code aggregate: true} field a document totals in its footer</li>
 * </ol>
 * - and the remainder is topped up from the record's leading {@code major} fields, i.e. the columns
 * its own list view shows. Nothing a role restricts ({@code sensitive}, {@code visibleTo}) is ever
 * part of a subject: the Inbox row is read by whoever holds the task, not by whoever may see the
 * record.
 * <p>
 * Only the property NAMES travel (as the {@code __subjectFields} process variable, {@code
 * <Property>:<kind>} entries). The values are resolved live by the reader, exactly as the task form
 * resolves the record it edits - a subject minted at process start would state the total of a
 * document whose lines are added afterwards.
 */
public final class TaskSubjectSupport {

    /**
     * How many properties a subject line carries - enough to identify a record, short enough to scan.
     */
    private static final int MAX_FIELDS = 3;

    private TaskSubjectSupport() {}

    /**
     * The trigger entity's subject declaration - {@code "Number:text,Customer:relation,Total:number"},
     * or {@code ""} when nothing identifies the entity beyond its key.
     *
     * @param entity the process's trigger entity, may be {@code null}
     * @return the encoded subject fields, never {@code null}
     */
    public static String subjectFields(EntityIntent entity) {
        if (entity == null) {
            return "";
        }
        Set<String> taken = new LinkedHashSet<>();
        List<String> encoded = new ArrayList<>();
        add(encoded, taken, labelField(entity));
        add(encoded, taken, partyRelation(entity));
        add(encoded, taken, totalField(entity));
        for (FieldIntent field : entity.getFields()) {
            if (encoded.size() >= MAX_FIELDS) {
                break;
            }
            if (isListColumn(field)) {
                add(encoded, taken, IntentNaming.pascalCase(field.getName()) + ":" + kindOf(field.getType()));
            }
        }
        return String.join(",", encoded);
    }

    private static void add(List<String> encoded, Set<String> taken, String entry) {
        if (entry == null || entry.isBlank() || encoded.size() >= MAX_FIELDS) {
            return;
        }
        String property = entry.substring(0, entry.indexOf(':'));
        if (taken.add(property)) {
            encoded.add(entry);
        }
    }

    /** What the record is called - the same property a to-one relation to it would label it by. */
    private static String labelField(EntityIntent entity) {
        String label = IntentEntities.labelFieldOf(entity);
        return label.isBlank() ? "" : label + ":text";
    }

    /**
     * Who the record is about - its first {@code major} to-one relation other than the status badge,
     * which every generated surface already renders separately (and which says nothing about WHICH
     * record this is).
     */
    private static String partyRelation(EntityIntent entity) {
        for (RelationIntent relation : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && relation.isMajor() && !relation.isEntityStatus() && relation.getName() != null) {
                return IntentNaming.pascalCase(relation.getName()) + ":relation";
            }
        }
        return "";
    }

    /** How much the record is - the field a document totals in its footer. */
    private static String totalField(EntityIntent entity) {
        for (FieldIntent field : entity.getFields()) {
            if (field.isAggregate() && field.getName() != null) {
                return IntentNaming.pascalCase(field.getName()) + ":" + kindOf(field.getType());
            }
        }
        return "";
    }

    /**
     * Whether the field is one of the record's own list columns: authored, shown ({@code major}),
     * readable by anyone holding the task, and not the key the subject exists to replace.
     */
    private static boolean isListColumn(FieldIntent field) {
        return field.getName() != null && field.isMajor() && !field.isPrimaryKey() && !field.isReadOnly() && !field.isSensitive()
                && field.getVisibleTo()
                        .isEmpty();
    }

    /**
     * How the reader formats the value: a grouped decimal, a whole number, a date, or plain text.
     * Counted quantities are separated from money because the instance's number pattern carries the
     * decimals money is written with - {@code 3 days} must not read {@code 3.00}.
     */
    private static String kindOf(String type) {
        if (type == null) {
            return "text";
        }
        switch (type.toLowerCase(Locale.ROOT)) {
            case "decimal":
            case "double":
                return "number";
            case "integer":
            case "int":
            case "long":
                return "integer";
            case "date":
            case "timestamp":
                return "date";
            default:
                return "text";
        }
    }
}
