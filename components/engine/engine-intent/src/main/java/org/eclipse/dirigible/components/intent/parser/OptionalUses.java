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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.UniqueIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;

/**
 * An OPTIONAL {@code uses:} entry (dirigible #7979): the model as it is generated where the owner
 * model is absent.
 *
 * <p>
 * The decision is made per Generate - the owner's {@code .model} is in the workspace or published,
 * or it is not - and the absent case is the intent with everything that depends on the owner taken
 * out of the raw tree, then parsed like any other: the generators never see an absent model, so no
 * generated file can reference one. What is taken out:
 * <ul>
 * <li>a list item naming the owner by its alias ({@code model:}, {@code uses:}, {@code fromUses:})
 * - the {@code uses:} entry itself, a relation, an {@code exists}/{@code notExists} check, a
 * {@code related:} register, a {@code generates}, a roll-up, a schedule, a report's union
 * member;</li>
 * <li>what names a relation taken out, by a path walked from its own entity - the entity's
 * {@code order:}/{@code list:}/{@code subtitle:} entries and checks, a form's fields, a roll-up
 * through it, and a report (whole: regrouped without the dimension, its figures would mean
 * something else).</li>
 * </ul>
 * Anything else that names the owner or such a relation is refused when the intent is parsed:
 * {@link IntentParser#parse} parses this variant too, so a model that would not stand without its
 * optional dependency never reaches a Generate.
 */
final class OptionalUses {

    /** A dotted path in an expression: a property, or relation hops followed by one. */
    private static final Pattern PATH = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*");

    /** A quoted literal - text, never a path. */
    private static final Pattern QUOTED = Pattern.compile("'[^']*'|\"[^\"]*\"");

    /** The keys naming another model by its {@code uses:} alias. */
    private static final List<String> ALIAS_KEYS = List.of("model", "uses", "fromUses");

    /**
     * A check's keys holding paths over its own record ({@code match}: its values, see {@link #names}).
     */
    private static final List<String> CHECK_PATHS = List.of("field", "fields", "than", "relations", "when", "match");

    /** A report's keys holding paths over its source. */
    private static final List<String> REPORT_PATHS = List.of("dimensions", "columns", "measures", "filter", "having", "orderBy");

    private OptionalUses() {}

    /**
     * The aliases a model declares optional.
     *
     * @param model the parsed model
     * @return the optional {@code uses:} aliases, in declaration order
     */
    static Set<String> aliases(IntentModel model) {
        Set<String> aliases = new LinkedHashSet<>();
        for (UsesIntent uses : model.getUses()) {
            if (uses.isOptional() && uses.getModel() != null) {
                aliases.add(uses.getModel());
            }
        }
        return aliases;
    }

    /**
     * What may never depend on a model that may be absent (the issue's three): a required relation, a
     * relation in a unique key and one partitioning a number series - the record cannot exist, or
     * cannot be told apart, without it.
     *
     * @param model the parsed model
     * @param issues the collecting issue list
     */
    static void validate(IntentModel model, List<String> issues) {
        Set<String> aliases = aliases(model);
        if (aliases.isEmpty()) {
            return;
        }
        for (EntityIntent entity : model.getEntities()) {
            for (RelationIntent relation : entity.getRelations()) {
                if (relation.getModel() == null || !aliases.contains(relation.getModel())) {
                    continue;
                }
                String subject = "entity [" + entity.getName() + "] relation [" + relation.getName() + "] targets the optional uses ["
                        + relation.getModel() + "]";
                if (relation.isRequired()) {
                    issues.add(subject + " and is required - a relation to a model that may be absent cannot be required");
                }
                if (entity.getUnique() != null) {
                    for (UniqueIntent unique : entity.getUnique()) {
                        if (unique.getFields()
                                  .contains(relation.getName())) {
                            issues.add(subject + " and is part of the unique key " + unique.getFields()
                                    + " - a key cannot hold a value that may be absent");
                        }
                    }
                }
                for (FieldIntent field : entity.getFields()) {
                    if (field.getNumber() != null && relation.getName()
                                                             .equals(field.getNumber()
                                                                          .getPer())) {
                        issues.add(subject + " and partitions the number series of [" + field.getName()
                                + "] - a series cannot be partitioned by a value that may be absent");
                    }
                }
            }
        }
    }

    /**
     * Takes the absent models out of a raw intent tree, in place.
     *
     * @param tree the SnakeYAML-loaded tree, before any normalisation
     * @param absent the {@code uses:} aliases whose models are absent
     * @return what was taken out, one entry per construct, for the Generate to report
     */
    static List<String> strip(Object tree, Set<String> absent) {
        List<String> dropped = new ArrayList<>();
        if (!(tree instanceof Map<?, ?> root) || absent.isEmpty()) {
            return dropped;
        }
        List<Map<Object, Object>> entities = maps(root.get("entities"));
        Map<String, Set<String>> gone = new HashMap<>();
        Map<String, Map<String, String>> hops = new HashMap<>();
        for (Map<Object, Object> entity : entities) {
            String name = String.valueOf(entity.get("name"));
            for (Map<Object, Object> relation : maps(entity.get("relations"))) {
                String relationName = String.valueOf(relation.get("name"));
                if (namesAlias(relation, absent)) {
                    gone.computeIfAbsent(name, k -> new LinkedHashSet<>())
                        .add(relationName);
                } else if (relation.get("model") == null && relation.get("to") != null) {
                    hops.computeIfAbsent(name, k -> new HashMap<>())
                        .put(relationName, String.valueOf(relation.get("to")));
                }
            }
            // A check's message is translated under its POSITION unless it has an id (#7611): the
            // checks after a dropped one keep their catalog key by being given the one they had.
            List<Map<Object, Object>> checks = maps(entity.get("checks"));
            for (int i = 0; i < checks.size(); i++) {
                checks.get(i)
                      .putIfAbsent("id", checks.get(i)
                                               .get("kind")
                              + "_" + i);
            }
        }
        dropByAlias(root, null, absent, dropped);
        if (gone.isEmpty()) {
            return dropped;
        }
        for (Map<Object, Object> entity : entities) {
            String name = String.valueOf(entity.get("name"));
            for (String key : List.of("order", "list", "subtitle")) {
                if (entity.get(key) instanceof List<?> entries) {
                    entries.removeIf(entry -> names(entry, name, gone, hops));
                }
            }
            if (entity.get("checks") instanceof List<?> checks) {
                checks.removeIf(node -> {
                    if (!(node instanceof Map<?, ?> check)) {
                        return false;
                    }
                    boolean names = CHECK_PATHS.stream()
                                               .anyMatch(key -> names(check.get(key), name, gone, hops));
                    if (names) {
                        dropped.add("check [" + name + "_" + check.get("id") + "]");
                    }
                    return names;
                });
            }
        }
        for (Map<Object, Object> form : maps(root.get("forms"))) {
            String base = String.valueOf(form.get("forEntity"));
            for (String key : List.of("fields", "editable")) {
                if (form.get(key) instanceof List<?> entries) {
                    entries.removeIf(entry -> names(entry, base, gone, hops));
                }
            }
        }
        if (root.get("reports") instanceof List<?> reports) {
            reports.removeIf(node -> {
                if (!(node instanceof Map<?, ?> report)) {
                    return false;
                }
                String base = String.valueOf(report.get("source"));
                boolean names = REPORT_PATHS.stream()
                                            .anyMatch(key -> names(report.get(key), base, gone, hops));
                if (names) {
                    dropped.add("report [" + report.get("name") + "]");
                }
                return names;
            });
        }
        if (root.get("rollups") instanceof List<?> rollups) {
            rollups.removeIf(node -> {
                if (!(node instanceof Map<?, ?> rollup)) {
                    return false;
                }
                boolean names = names(rollup.get("via"), String.valueOf(rollup.get("entity")), gone, hops);
                if (names) {
                    dropped.add("rollups [" + rollup.get("name") + "]");
                }
                return names;
            });
        }
        return dropped;
    }

    /**
     * Removes, at any depth but the seed rows (data, not declarations), every list item naming an
     * absent alias.
     */
    private static void dropByAlias(Object node, String entity, Set<String> absent, List<String> dropped) {
        if (node instanceof List<?> list) {
            for (Object item : list) {
                dropByAlias(item, entity, absent, dropped);
            }
        }
        if (!(node instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if ("seeds".equals(key)) {
                continue;
            }
            if (!(entry.getValue() instanceof List<?> items)) {
                dropByAlias(entry.getValue(), entity, absent, dropped);
                continue;
            }
            items.removeIf(item -> {
                boolean names = item instanceof Map<?, ?> itemMap && namesAlias(itemMap, absent);
                if (names && !"uses".equals(key)) {
                    dropped.add(describe(key, entity, (Map<?, ?>) item));
                }
                return names;
            });
            for (Object item : items) {
                boolean anEntity = entity == null && "entities".equals(key) && item instanceof Map<?, ?>;
                dropByAlias(item, anEntity ? String.valueOf(((Map<?, ?>) item).get("name")) : entity, absent, dropped);
            }
        }
    }

    private static String describe(String key, String entity, Map<?, ?> item) {
        if ("relations".equals(key)) {
            return "relation [" + entity + "." + item.get("name") + "]";
        }
        if ("checks".equals(key)) {
            return "check [" + entity + "_" + item.get("id") + "]";
        }
        Object label = item.get("name") != null ? item.get("name") : item.get("entity") != null ? item.get("entity") : item.get("source");
        return key + " [" + label + "]" + (entity == null ? "" : " of [" + entity + "]");
    }

    private static boolean namesAlias(Map<?, ?> item, Set<String> absent) {
        return ALIAS_KEYS.stream()
                         .anyMatch(key -> item.get(key) instanceof String alias && absent.contains(alias));
    }

    /**
     * Whether a value - a path, an expression, or a list/map of them - walks through a dropped
     * relation.
     */
    private static boolean names(Object value, String base, Map<String, Set<String>> gone, Map<String, Map<String, String>> hops) {
        if (value instanceof Iterable<?> values) {
            for (Object v : values) {
                if (names(v, base, gone, hops)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Map<?, ?> map) {
            return names(map.values(), base, gone, hops);
        }
        if (!(value instanceof String text)) {
            return false;
        }
        Matcher path = PATH.matcher(QUOTED.matcher(text)
                                          .replaceAll(" "));
        while (path.find()) {
            String entity = base;
            for (String segment : path.group()
                                      .split("\\.")) {
                if (gone.getOrDefault(entity, Set.of())
                        .contains(segment)) {
                    return true;
                }
                entity = hops.getOrDefault(entity, Map.of())
                             .get(segment);
                if (entity == null) {
                    break;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<Object, Object>> maps(Object node) {
        List<Map<Object, Object>> maps = new ArrayList<>();
        if (node instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    maps.add((Map<Object, Object>) map);
                }
            }
        }
        return maps;
    }
}
