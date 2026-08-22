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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * Translates a declarative {@code fileName:} pattern into the Java expression a generated delegate
 * names its rendered PDF with - the snapshot copy a document mints on issue, the attachment a
 * {@code notify} block sends. Both names used to be hardcoded, and inconsistently: the mint named
 * the file after the numeric primary key while the attachment already used the document number, so
 * the same document arrived in the archive and in the customer's inbox under two different names.
 *
 * <p>
 * The grammar is the smallest one a self-describing archive name needs - literals with
 * {@code {token}} interpolations, no expressions:
 *
 * <pre>
 * fileName: "{Number}_{Date:yyyyMMdd}_{Company.ShortName|Company.Name}"
 * </pre>
 *
 * <ul>
 * <li>{@code {Field}} and one-hop {@code {Relation.Field}} - the same path vocabulary a notify
 * subject or body resolves, against the same record.</li>
 * <li>{@code {Field:pattern}} - a {@code date}/{@code timestamp} field rendered through a
 * {@code java.time.format.DateTimeFormatter} pattern.</li>
 * <li>{@code {A|B}} - alternative operands, the first non-blank one wins (an optional short name
 * beside the legal name is filled for some records and not for others).</li>
 * <li>{@link #VERSION_TOKEN} - the copy's version, a snapshot only.</li>
 * </ul>
 *
 * <p>
 * Every interpolated <b>value</b> is sanitized at run time by the SDK's
 * {@code org.eclipse.dirigible.sdk.print.FileNames}; the literal separators between tokens are the
 * author's and are emitted verbatim. An unresolvable path is an error, not an empty rendering - a
 * pattern that silently drops a token would produce archive names nobody can tell apart, which is
 * the very failure this replaces.
 *
 * <p>
 * This deliberately does not reuse {@link NotificationSupport.Resolver}: that resolver hardcodes
 * the message's {@code entity} local as the base of a direct field, and it makes the reserved
 * deep-link tokens ({@code appUrl} / {@code recordUrl} / {@code inboxUrl}) addressable - neither of
 * which belongs in a file name, and the second of which would emit a local no template declares
 * here.
 */
public final class FileNameSupport {

    /**
     * The reserved token naming a snapshot copy's version. Addressable only where a version exists (the
     * mint); a notify attachment has none, and a pattern that used it there would read as a working
     * declaration while the generated code could not compile.
     */
    public static final String VERSION_TOKEN = "Version";

    /** The Java local the snapshot delegate holds the copy's version in. */
    static final String VERSION_LOCAL = "version";

    /** The SDK helper the generated code sanitizes and formats every interpolated value with. */
    private static final String HELPER = "org.eclipse.dirigible.sdk.print.FileNames";

    /** One {@code {...}} interpolation. The body is parsed by {@link #operandExpression}. */
    private static final Pattern TOKEN = Pattern.compile("\\{([^{}]*)\\}");

    /** A one-hop path: a field, or a to-one relation and one field of its target. */
    private static final Pattern PATH = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?");

    private FileNameSupport() {}

    /**
     * The translated pattern.
     *
     * @param expression the Java String expression, without the {@code .pdf} suffix
     * @param loads the one-hop relation loads the expression reads, in first-use order
     * @param usesVersion whether the pattern named {@link #VERSION_TOKEN} itself (a snapshot then
     *        appends no version suffix of its own - the author already placed it)
     */
    record Resolved(String expression, List<NotificationSupport.RelationLoad> loads, boolean usesVersion) {
    }

    /**
     * Where a pattern is being resolved: which record its paths read, what the generated code calls
     * that record, and what the surrounding template can offer.
     *
     * @param entity the entity every path resolves against
     * @param local the Java local holding that record
     * @param relationsAllowed whether a one-hop {@code {Relation.Field}} may be used - false where the
     *        generated code renders outside the scope the relation locals are declared in (a fan-out's
     *        anchor document, rendered once before the per-row loop)
     * @param versionAllowed whether {@link #VERSION_TOKEN} is addressable
     */
    record Site(EntityIntent entity, String local, boolean relationsAllowed, boolean versionAllowed) {
    }

    /**
     * Resolve a {@code fileName:} pattern into the expression the generated code assigns.
     *
     * @param pattern the authored pattern
     * @param site where it is being resolved
     * @param byName all LOCAL entities by name
     * @param compositionParents composition-parent map (to resolve a relation target's perspective)
     * @param crossModel resolver for a cross-model relation target, or {@code null}
     * @return the translated pattern, or {@code null} when nothing was authored
     * @throws IllegalArgumentException when the pattern is malformed or a path does not resolve - the
     *         caller reports the precise reason and drops, rather than minting indistinguishable names
     */
    static Resolved resolve(String pattern, Site site, Map<String, EntityIntent> byName, Map<String, String> compositionParents,
            NotificationSupport.CrossModelLookup crossModel) {
        if (pattern == null || pattern.isBlank()) {
            return null;
        }
        String authored = pattern.trim();
        Map<String, NotificationSupport.RelationLoad> loads = new LinkedHashMap<>();
        List<String> terms = new ArrayList<>();
        boolean usesVersion = false;
        Matcher matcher = TOKEN.matcher(authored);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                terms.add(NotificationSupport.quote(authored.substring(last, matcher.start())));
            }
            String body = matcher.group(1);
            if (VERSION_TOKEN.equals(body.trim())) {
                requireVersion(site, authored);
                usesVersion = true;
                terms.add(VERSION_LOCAL);
            } else {
                terms.add(tokenExpression(body, authored, site, byName, compositionParents, crossModel, loads));
            }
            last = matcher.end();
        }
        if (last == 0) {
            throw new IllegalArgumentException("fileName [" + authored + "] interpolates nothing - it would name every copy alike");
        }
        if (last < authored.length()) {
            terms.add(NotificationSupport.quote(authored.substring(last)));
        }
        return new Resolved(join(terms), new ArrayList<>(loads.values()), usesVersion);
    }

    /**
     * The default name of a rendered document: its own number when the entity declares a
     * {@code number:} field (so the customer receives {@code SI00000042.pdf}, not
     * {@code SalesInvoice 42.pdf}), else the entity name plus the record id. Shared by the mail
     * attachment and the snapshot mint, which is what makes the two names finally agree.
     *
     * @param entity the rendered entity
     * @param local the Java local holding the record
     * @return a Java String expression
     */
    static String numberOrId(EntityIntent entity, String local) {
        String keyProperty = IntentEntities.keyFieldName(entity);
        for (FieldIntent field : entity.getFields()) {
            if (field.getNumber() != null && field.getName() != null) {
                String number = local + "." + IntentNaming.pascalCase(field.getName());
                return "(" + number + " == null || " + number + ".isBlank() ? \"" + entity.getName() + " \" + " + local + "." + keyProperty
                        + " : " + number + ")";
            }
        }
        return "\"" + entity.getName() + " \" + " + local + "." + keyProperty;
    }

    /** One {@code {...}} body: a single operand, or {@code |}-separated alternatives. */
    private static String tokenExpression(String body, String authored, Site site, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, NotificationSupport.CrossModelLookup crossModel,
            Map<String, NotificationSupport.RelationLoad> loads) {
        List<String> operands = new ArrayList<>();
        for (String operand : body.split("\\|")) {
            operands.add(operandExpression(operand, authored, site, byName, compositionParents, crossModel, loads));
        }
        if (operands.isEmpty()) {
            throw new IllegalArgumentException("fileName [" + authored + "] has an empty {} token");
        }
        return operands.size() == 1 ? operands.get(0) : HELPER + ".first(" + String.join(", ", operands) + ")";
    }

    /** One operand: {@code Path} or {@code Path:pattern}, rendered as a sanitizing helper call. */
    private static String operandExpression(String operand, String authored, Site site, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, NotificationSupport.CrossModelLookup crossModel,
            Map<String, NotificationSupport.RelationLoad> loads) {
        String trimmed = operand.trim();
        int colon = trimmed.indexOf(':');
        String path = colon < 0 ? trimmed
                : trimmed.substring(0, colon)
                         .trim();
        String format = colon < 0 ? null
                : trimmed.substring(colon + 1)
                         .trim();
        if (path.isEmpty() || !PATH.matcher(path)
                                   .matches()) {
            throw new IllegalArgumentException("fileName [" + authored + "]: [" + trimmed
                    + "] is not a field or a one-hop relation.field path on [" + site.entity()
                                                                                     .getName()
                    + "]");
        }
        String access = access(path, authored, site, byName, compositionParents, crossModel, loads);
        return format == null || format.isEmpty() ? HELPER + ".part(" + access + ")"
                : HELPER + ".part(" + access + ", " + NotificationSupport.quote(format) + ")";
    }

    /** A Java read of a direct field or a one-hop relation field, registering the load it needs. */
    private static String access(String path, String authored, Site site, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, NotificationSupport.CrossModelLookup crossModel,
            Map<String, NotificationSupport.RelationLoad> loads) {
        EntityIntent entity = site.entity();
        int dot = path.indexOf('.');
        if (dot < 0) {
            if (fieldOf(entity, path) == null) {
                throw new IllegalArgumentException(
                        "fileName [" + authored + "]: [" + path + "] is not a field of [" + entity.getName() + "]");
            }
            return site.local() + "." + IntentNaming.pascalCase(path);
        }
        if (!site.relationsAllowed()) {
            throw new IllegalArgumentException("fileName [" + authored + "]: [" + path
                    + "] is a relation hop, and this document is rendered once for the whole fan-out - only fields of [" + entity.getName()
                    + "] itself are readable here");
        }
        String relationName = path.substring(0, dot);
        String fieldName = path.substring(dot + 1);
        RelationIntent relation = toOneRelation(entity, relationName);
        if (relation == null || relation.getTo() == null) {
            throw new IllegalArgumentException(
                    "fileName [" + authored + "]: [" + relationName + "] is not a to-one relation of [" + entity.getName() + "]");
        }
        String pascalField = IntentNaming.pascalCase(fieldName);
        if (relation.getModel() != null && !relation.getModel()
                                                    .isBlank()) {
            NotificationSupport.CrossModelTarget target = crossModel == null ? null : crossModel.resolve(relation);
            // A naming-convention fallback carries null propertyNames - then the authored field is
            // trusted, exactly as a cross-model notification placeholder trusts it.
            if (target == null || (target.propertyNames() != null && !target.propertyNames()
                                                                            .contains(pascalField))) {
                throw new IllegalArgumentException(
                        "fileName [" + authored + "]: [" + fieldName + "] could not be resolved on the cross-model target ["
                                + relation.getTo() + "] of model [" + relation.getModel() + "]");
            }
            loads.computeIfAbsent(relationName, name -> new NotificationSupport.RelationLoad(name, relation.getTo(),
                    target.perspectiveName(), IntentNaming.pascalCase(name), true, target.modelAlias(), target.project()));
        } else {
            EntityIntent target = byName.get(relation.getTo());
            if (target == null || fieldOf(target, fieldName) == null) {
                throw new IllegalArgumentException(
                        "fileName [" + authored + "]: [" + fieldName + "] is not a field of [" + relation.getTo() + "]");
            }
            loads.computeIfAbsent(relationName,
                    name -> new NotificationSupport.RelationLoad(name, relation.getTo(), IntentEntities.resolvePerspective(relation.getTo(),
                            compositionParents, IntentEntities.settingEntities(byName.values())), IntentNaming.pascalCase(name), false, "",
                            ""));
        }
        // The generated code loads the related record into a local named after the relation - the same
        // local a notify placeholder reads, so one load serves both.
        return "(" + relationName + " == null ? null : " + relationName + "." + pascalField + ")";
    }

    private static void requireVersion(Site site, String authored) {
        if (!site.versionAllowed()) {
            throw new IllegalArgumentException("fileName [" + authored + "] uses {" + VERSION_TOKEN
                    + "}, which only a snapshot copy has - a sent document carries no version");
        }
    }

    /** Concatenates the terms into a String expression, forcing String on a lone non-literal term. */
    private static String join(List<String> terms) {
        if (terms.size() == 1 && !terms.get(0)
                                       .startsWith("\"")) {
            return "\"\" + " + terms.get(0);
        }
        return String.join(" + ", terms);
    }

    private static FieldIntent fieldOf(EntityIntent entity, String name) {
        for (FieldIntent field : entity.getFields()) {
            if (name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    private static RelationIntent toOneRelation(EntityIntent entity, String name) {
        for (RelationIntent relation : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }
}
