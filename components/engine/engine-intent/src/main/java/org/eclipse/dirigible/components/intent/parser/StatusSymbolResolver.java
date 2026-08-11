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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves a status referenced by its <b>seeded name</b> to the seed id, everywhere the intent
 * names a status, on the raw YAML tree - before the typed Gson mapping, so every downstream
 * validator, generator and template keeps seeing the plain integers it always saw.
 *
 * <pre>
 * transitions:
 *   - { name: VoidSalesInvoice, forEntity: SalesInvoice, from: [ISSUED, SENT], setStatus: VOIDED }
 * reports:
 *   - { name: OverdueInvoices, source: SalesInvoice, filter: "balance &gt; 0 AND Status != VOIDED" }
 * </pre>
 *
 * Why this exists: a status id is <b>positional</b>. Inserting a status into the middle of a
 * nomenclature shifts every later id, and every guard, filter and transition authored against the
 * old numbering keeps generating well-formed output that now means something else - the failure
 * mode that left a voided invoice on the ledger with no reversing entry (dirigible #6645). A name
 * cannot be silently retargeted, and a mistyped name is a generation error rather than a wrong
 * number.
 *
 * <p>
 * Scope: the nomenclature must be seeded <b>in this model</b>. A cross-model status entity is
 * seeded in its owner model, which the parser cannot read (it holds one file, with no repository),
 * so a symbol against it is refused with a message naming the numeric-id fallback instead of being
 * quietly left in place.
 */
final class StatusSymbolResolver {

    /** A comparison inside a guard / filter expression: term, operator, symbolic right-hand side. */
    private static final Pattern COMPARISON =
            Pattern.compile("(\\b[A-Za-z_][A-Za-z0-9_]*\\b)\\s*(==|!=|<>|<=|>=|=|<|>)\\s*([A-Za-z_][A-Za-z0-9_]*)\\b");

    /** The operators for which a status NAME is meaningful - a name has no ordering. */
    private static final Set<String> EQUALITY = Set.of("==", "!=", "<>", "=");

    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    /** Entity name to its raw node. */
    private final Map<String, Map<?, ?>> entities = new LinkedHashMap<>();

    /** Seeded entity name to its rows' {@code name} to {@code id} mapping, in seed order. */
    private final Map<String, Map<String, Integer>> seededIds = new LinkedHashMap<>();

    private final List<String> issues = new ArrayList<>();

    private StatusSymbolResolver() {}

    /**
     * Rewrite every symbolic status reference in the tree to its seed id, in place.
     *
     * @param tree the SnakeYAML-loaded raw tree
     * @throws IntentValidationException naming every symbol that did not resolve
     */
    static void resolve(Object tree) {
        Map<?, ?> root = asMap(tree);
        if (root == null) {
            return;
        }
        StatusSymbolResolver resolver = new StatusSymbolResolver();
        resolver.index(root);
        resolver.rewriteEntities(root);
        resolver.rewriteTransitions(root);
        resolver.rewriteProcesses(root);
        resolver.rewritePostings(root);
        resolver.rewriteReports(root);
        if (!resolver.issues.isEmpty()) {
            throw new IntentValidationException(resolver.issues);
        }
    }

    // ----- indexing ---------------------------------------------------------------

    private void index(Map<?, ?> root) {
        for (Object node : asList(root.get("entities"))) {
            Map<?, ?> entity = asMap(node);
            String name = text(entity, "name");
            if (name != null) {
                entities.put(name, entity);
            }
        }
        for (Object node : asList(root.get("seeds"))) {
            Map<?, ?> seed = asMap(node);
            String entityName = text(seed, "entity");
            if (entityName == null || text(seed, "language") != null) {
                continue; // a translation seed carries no base rows
            }
            String idField = idFieldOf(entities.get(entityName));
            Map<String, Integer> ids = seededIds.computeIfAbsent(entityName, key -> new LinkedHashMap<>());
            for (Object rowNode : asList(seed.get("rows"))) {
                Map<?, ?> row = asMap(rowNode);
                String rowName = text(row, "name");
                Integer id = integerOf(row == null ? null : row.get(idField));
                if (rowName != null && id != null) {
                    ids.putIfAbsent(rowName, id);
                }
            }
        }
    }

    /** The field name the entity's seed rows key its primary key by ({@code id} by convention). */
    private static String idFieldOf(Map<?, ?> entity) {
        for (Object node : asList(entity == null ? null : entity.get("fields"))) {
            Map<?, ?> field = asMap(node);
            if (field != null && Boolean.TRUE.equals(field.get("primaryKey")) && text(field, "name") != null) {
                return text(field, "name");
            }
        }
        return "id";
    }

    // ----- sites ------------------------------------------------------------------

    private void rewriteEntities(Map<?, ?> root) {
        for (Object node : asList(root.get("entities"))) {
            Map<?, ?> entity = asMap(node);
            if (entity == null) {
                continue;
            }
            String entityName = text(entity, "name");
            // A to-one relation's `init:` is the FK's DB-level default - the id of a seed row of ITS
            // OWN target, which need not be a status (a payment method, a document kind).
            for (Object relationNode : asList(entity.get("relations"))) {
                Map<?, ?> relation = asMap(relationNode);
                if (relation == null || relation.get("init") == null) {
                    continue;
                }
                Target target = new Target(text(relation, "to"), text(relation, "model"));
                Integer id = resolveScalar(relation.get("init"), target,
                        "entity [" + entityName + "] relation [" + text(relation, "name") + "] init");
                if (id != null) {
                    put(relation, "init", String.valueOf(id));
                }
            }
            Target status = statusOf(entityName);
            String statusRelation = statusRelationName(entityName);
            if (entity.get("immutableWhen") != null) {
                String rewritten = rewriteExpression(text(entity, "immutableWhen"), statusRelation, status,
                        "entity [" + entityName + "] immutableWhen");
                put(entity, "immutableWhen", rewritten);
            }
            for (Object checkNode : asList(entity.get("checks"))) {
                Map<?, ?> check = asMap(checkNode);
                if (check == null) {
                    continue;
                }
                String subject = "entity [" + entityName + "] check [" + text(check, "kind") + "]";
                putResolved(check, "status", status, subject + " status");
                putResolved(check, "setStatus", status, subject + " setStatus");
            }
        }
    }

    private void rewriteTransitions(Map<?, ?> root) {
        for (Object node : asList(root.get("transitions"))) {
            Map<?, ?> transition = asMap(node);
            if (transition == null) {
                continue;
            }
            String subject = "transition [" + text(transition, "name") + "]";
            Target status = statusOf(text(transition, "forEntity"));
            putResolvedList(transition, "from", status, subject + " from");
            putResolved(transition, "setStatus", status, subject + " setStatus");
        }
    }

    private void rewriteProcesses(Map<?, ?> root) {
        for (Object node : asList(root.get("processes"))) {
            Map<?, ?> process = asMap(node);
            if (process == null) {
                continue;
            }
            String subject = "process [" + text(process, "name") + "]";
            String triggerEntity = triggerEntityOf(process);
            Map<?, ?> abortOn = asMap(process.get("abortOn"));
            if (abortOn != null) {
                putResolvedList(abortOn, "status", statusOf(triggerEntity), subject + " abortOn status");
            }
            // `setRelationField: <Relation>` + `value:` writes an id of THAT relation's target - the
            // status in the canonical case, but the same shape serves any nomenclature FK.
            for (Object stepNode : asList(process.get("steps"))) {
                Map<?, ?> args = asMap(asMap(stepNode) == null ? null : asMap(stepNode).get("args"));
                String relationName = text(args, "setRelationField");
                if (relationName == null || args.get("value") == null) {
                    continue;
                }
                Map<?, ?> relation = toOneRelation(triggerEntity, relationName);
                Target target = relation == null ? new Target(null, null) : new Target(text(relation, "to"), text(relation, "model"));
                putResolved(args, "value", target,
                        subject + " step [" + text(asMap(stepNode), "name") + "] setRelationField [" + relationName + "] value");
            }
        }
    }

    private void rewritePostings(Map<?, ?> root) {
        for (Object node : asList(root.get("postings"))) {
            Map<?, ?> posting = asMap(node);
            Map<?, ?> event = asMap(posting == null ? null : posting.get("event"));
            if (event == null || event.get("when") == null) {
                continue;
            }
            String subject = "posting [" + text(posting, "name") + "] event when";
            Object sourceName = event.get("onTransition") == null ? event.get("onCreate") : event.get("onTransition");
            String source = sourceName == null ? null
                    : String.valueOf(sourceName)
                            .trim();
            // A cross-model source's nomenclature is seeded in its own model; name it as such rather
            // than reporting the entity as unknown.
            Target status = text(event, "model") != null ? new Target(source, text(event, "model")) : statusOf(source);
            put(event, "when", rewriteExpression(text(event, "when"), statusRelationName(source), status, subject));
        }
    }

    private void rewriteReports(Map<?, ?> root) {
        for (Object node : asList(root.get("reports"))) {
            Map<?, ?> report = asMap(node);
            if (report == null || report.get("filter") == null) {
                continue;
            }
            String source = text(report, "source");
            put(report, "filter", rewriteExpression(text(report, "filter"), statusRelationName(source), statusOf(source),
                    "report [" + text(report, "name") + "] filter"));
        }
    }

    /** The entity whose lifecycle event starts the process - the target of its {@code trigger}. */
    private static String triggerEntityOf(Map<?, ?> process) {
        Map<?, ?> trigger = asMap(process.get("trigger"));
        if (trigger == null) {
            return null;
        }
        for (String event : List.of("onCreate", "onUpdate", "onDelete")) {
            String entity = text(trigger, event);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    // ----- resolution -------------------------------------------------------------

    /** The status nomenclature an entity's lifecycle is expressed through. */
    private Target statusOf(String entityName) {
        Map<?, ?> relation = statusRelation(entityName);
        return relation == null ? new Target(null, null) : new Target(text(relation, "to"), text(relation, "model"));
    }

    private String statusRelationName(String entityName) {
        Map<?, ?> relation = statusRelation(entityName);
        return relation == null ? null : text(relation, "name");
    }

    private Map<?, ?> statusRelation(String entityName) {
        for (Object node : asList(entities.get(entityName) == null ? null
                : entities.get(entityName)
                          .get("relations"))) {
            Map<?, ?> relation = asMap(node);
            if (relation != null && "entitystatus".equals(lower(text(relation, "function")))) {
                return relation;
            }
        }
        return null;
    }

    private Map<?, ?> toOneRelation(String entityName, String relationName) {
        for (Object node : asList(entities.get(entityName) == null ? null
                : entities.get(entityName)
                          .get("relations"))) {
            Map<?, ?> relation = asMap(node);
            if (relation != null && relationName.equals(text(relation, "name"))) {
                return relation;
            }
        }
        return null;
    }

    /** Resolve a scalar key in place when it holds a symbol; leave a numeric value untouched. */
    private void putResolved(Map<?, ?> owner, String key, Target target, String subject) {
        if (owner.get(key) == null) {
            return;
        }
        Integer id = resolveScalar(owner.get(key), target, subject);
        if (id != null) {
            put(owner, key, id);
        }
    }

    /** Resolve a key holding either a single status or a list of them, in place. */
    @SuppressWarnings("unchecked")
    private void putResolvedList(Map<?, ?> owner, String key, Target target, String subject) {
        Object value = owner.get(key);
        if (value instanceof List<?> list) {
            List<Object> mutable = (List<Object>) list;
            for (int i = 0; i < mutable.size(); i++) {
                Integer id = resolveScalar(mutable.get(i), target, subject);
                if (id != null) {
                    mutable.set(i, id);
                }
            }
        } else {
            putResolved(owner, key, target, subject);
        }
    }

    /**
     * The seed id a scalar names, or {@code null} when it is already numeric (nothing to do) or did not
     * resolve (an issue is recorded).
     */
    private Integer resolveScalar(Object value, Target target, String subject) {
        if (value instanceof Number || value == null) {
            return null;
        }
        String token = String.valueOf(value)
                             .trim();
        if (token.isEmpty() || INTEGER.matcher(token)
                                      .matches()) {
            return null;
        }
        return resolveSymbol(token, target, subject);
    }

    /**
     * Rewrite `&lt;status relation&gt; == &lt;NAME&gt;` comparisons in a guard / filter expression.
     * When the nomenclature is cross-model its relation name is not resolvable here, so ANY symbolic
     * comparison is reported as the cross-model status reference it almost certainly is - the
     * alternative is leaving it in place and failing later with a message about a malformed guard.
     */
    private String rewriteExpression(String expression, String statusRelation, Target target, String subject) {
        if (expression == null || (statusRelation == null && target.model() == null)) {
            return expression;
        }
        Matcher matcher = COMPARISON.matcher(expression);
        StringBuilder rewritten = new StringBuilder();
        while (matcher.find()) {
            String replacement = matcher.group();
            boolean aboutStatus = statusRelation == null || statusRelation.equalsIgnoreCase(matcher.group(1));
            if (aboutStatus && !INTEGER.matcher(matcher.group(3))
                                       .matches()) {
                if (!EQUALITY.contains(matcher.group(2))) {
                    issues.add(subject + " compares the status to the name [" + matcher.group(3) + "] with [" + matcher.group(2)
                            + "] - a status name has no ordering; use ==/!= per status, or a report `scope:`");
                } else {
                    Integer id = resolveSymbol(matcher.group(3), target, subject);
                    if (id != null) {
                        replacement = matcher.group(1) + " " + matcher.group(2) + " " + id;
                    }
                }
            }
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rewritten);
        return rewritten.toString();
    }

    /**
     * The seed id the name refers to; records an issue and returns {@code null} when it cannot resolve.
     */
    private Integer resolveSymbol(String name, Target target, String subject) {
        if (target.entity() == null) {
            issues.add(subject + " names the status [" + name
                    + "] but no `function: EntityStatus` relation resolves the nomenclature to look it up in");
            return null;
        }
        if (target.model() != null) {
            issues.add(subject + " names the status [" + name + "] of [" + target.entity() + "], which belongs to model [" + target.model()
                    + "] and is seeded there - a cross-model status must be referenced by its numeric seed id");
            return null;
        }
        Map<String, Integer> ids = seededIds.get(target.entity());
        if (ids == null || ids.isEmpty()) {
            issues.add(subject + " names the status [" + name + "] but [" + target.entity()
                    + "] has no seeded rows in this model - seed the nomenclature or use the numeric id");
            return null;
        }
        Integer id = ids.get(name);
        if (id == null) {
            for (Map.Entry<String, Integer> entry : ids.entrySet()) {
                if (entry.getKey()
                         .equalsIgnoreCase(name)) {
                    id = entry.getValue();
                    break;
                }
            }
        }
        if (id == null) {
            issues.add(
                    subject + " names [" + name + "], which is not a seeded status of [" + target.entity() + "] - known: " + ids.keySet());
        }
        return id;
    }

    /** A status nomenclature: the entity holding it and, when it is not local, the model owning it. */
    private record Target(String entity, String model) {
    }

    // ----- raw-tree helpers -------------------------------------------------------

    private static Map<?, ?> asMap(Object node) {
        return node instanceof Map<?, ?> map ? map : null;
    }

    private static List<?> asList(Object node) {
        return node instanceof List<?> list ? list : List.of();
    }

    private static String text(Map<?, ?> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value)
                            .trim();
        return text.isEmpty() ? null : text;
    }

    private static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private static void put(Map<?, ?> owner, String key, Object value) {
        ((Map<Object, Object>) owner).put(key, value);
    }

    private static Integer integerOf(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value)
                            .trim();
        return INTEGER.matcher(text)
                      .matches() ? Integer.valueOf(text) : null;
    }
}
