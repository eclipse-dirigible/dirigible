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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.dirigible.components.intent.model.IntentModel;

import com.google.gson.annotations.SerializedName;

/**
 * Rejects intent keys the typed model does not declare, on the RAW YAML tree - before the Gson
 * mapping silently drops them.
 *
 * <p>
 * The typed mapping ignores unknown properties, so an invented or mis-cased key
 * ({@code calculatedActionOnCreate} on a relation that never had it, {@code contributionScheme} for
 * the relation {@code ContributionScheme}) used to be accepted and discarded: generation returned
 * 200, code generation 201, publish 200, and the promise the author wrote was simply absent at
 * runtime. That is the failure mode this module refuses everywhere else, so it is refused here too
 * - an unknown key is a validation ERROR naming the key, where it sits, and the nearest declared
 * name.
 *
 * <p>
 * The known keys are read from the model classes themselves (declared fields, honouring
 * {@link SerializedName}), so the check can never drift from what the parser actually maps. The
 * walk follows typed structure only, with one addition: a {@code Map}-valued property whose keys
 * are a <b>closed vocabulary</b> (a process {@code trigger:}, a glue {@code event:} binding, a
 * posting's {@code rule:}) is registered in {@link #MAP_KEYS} and checked against that vocabulary.
 * A map whose keys come from the model being described (a {@code map:} / {@code defaults:}
 * projection, a relation's {@code where:}, a widget's {@code at:}, a delegate's {@code fields:})
 * stays opaque, as does a step's {@code args:} - its vocabulary depends on the sibling {@code kind}
 * and is checked by {@code IntentParser.validateStepArgs}.
 */
final class UnknownKeyValidator {

    /** Model POJOs live in one package; only those are walked as typed nodes. */
    private static final String MODEL_PACKAGE = IntentModel.class.getPackageName();

    /** Declared key -> field, per model class. Reflection is done once per class. */
    private static final Map<Class<?>, Map<String, Field>> KEYS = new ConcurrentHashMap<>();

    /**
     * The entity-lifecycle / process-step axes a notification, an integration or a departure may bind
     * to.
     */
    private static final Set<String> GLUE_EVENT_KEYS =
            Set.of("onCreate", "onUpdate", "onDelete", "onStepReached", "onStepCompleted", "when");

    /**
     * Author-facing maps whose keys are a closed vocabulary, keyed by {@code <SimpleClassName>#<field>}
     * - and by {@code <SimpleClassName>#<field>.<key>} for a map nested one level inside one (a
     * step-event binding's {@code { process, step }}).
     *
     * <p>
     * These are AUTHORED lists, unlike the reflected ones: every entry mirrors what the readers of that
     * map actually consult, so adding a key to a map-shaped feature means adding it here too. A key the
     * readers reject with a better message of their own (a {@code generates} event's {@code model})
     * stays listed here, so the author gets that message rather than "unknown key".
     */
    private static final Map<String, Set<String>> MAP_KEYS = Map.ofEntries(
            Map.entry("ProcessIntent#trigger", Set.of("onCreate", "onUpdate", "onDelete", "when", "businessKey", "businessKeyStrategy")),
            Map.entry("ProcessIntent#abortOn", Set.of("status", "then")), Map.entry("NotificationIntent#event", GLUE_EVENT_KEYS),
            Map.entry("NotificationIntent#event.onStepReached", Set.of("process", "step")),
            Map.entry("NotificationIntent#event.onStepCompleted", Set.of("process", "step")),
            Map.entry("IntegrationIntent#event", GLUE_EVENT_KEYS),
            Map.entry("IntegrationIntent#event.onStepReached", Set.of("process", "step")),
            Map.entry("IntegrationIntent#event.onStepCompleted", Set.of("process", "step")),
            Map.entry("OutboundIntent#event", GLUE_EVENT_KEYS), Map.entry("OutboundIntent#event.onStepReached", Set.of("process", "step")),
            Map.entry("OutboundIntent#event.onStepCompleted", Set.of("process", "step")),
            Map.entry("PostingIntent#event", Set.of("onTransition", "onCreate", "when", "model")),
            Map.entry("PostingIntent#rule", Set.of("entity", "match")),
            Map.entry("GeneratesIntent#event", Set.of("onTransition", "onCreate", "when", "model")),
            Map.entry("GenerateChildIntent#forEach", Set.of("entity", "days", "model", "match")),
            Map.entry("ResolveIntent#event", Set.of("onCreate", "onUpdate", "when")),
            Map.entry("ResolveIntent#between", Set.of("start", "end", "value")), Map.entry("ResolveIntent#found", Set.of("setStatus")),
            Map.entry("ResolveIntent#notFound", Set.of("setStatus")), Map.entry("ResolveIntent#ambiguous", Set.of("setStatus")));

    private UnknownKeyValidator() {}

    /**
     * Walk the raw tree against the typed model and collect one issue per unknown key.
     *
     * @param tree the SnakeYAML-loaded raw tree (anything but a map is ignored - the typed mapping
     *        reports it)
     * @param issues the collecting issue list
     */
    static void collect(Object tree, List<String> issues) {
        walk(tree, IntentModel.class, "", issues);
    }

    /**
     * The declared name closest to the given key, or null when nothing is close enough. A pure case
     * difference always wins - it is the most common slip and the one hardest to spot by eye.
     *
     * @param key the authored key
     * @param declared the names actually declared
     * @return the nearest declared name, or null
     */
    static String nearest(String key, Collection<String> declared) {
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (String candidate : declared) {
            if (candidate.equalsIgnoreCase(key)) {
                return candidate;
            }
            int distance = distance(key.toLowerCase(Locale.ROOT), candidate.toLowerCase(Locale.ROOT));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best != null && bestDistance <= threshold(key) ? best : null;
    }

    /**
     * The "did you mean [x]?" suffix for an unknown key, empty when nothing is close enough.
     *
     * @param key the authored key
     * @param declared the names actually declared
     * @return the suffix to append to an issue message
     */
    static String suggestion(String key, Collection<String> declared) {
        String nearest = nearest(key, declared);
        if (nearest == null) {
            return "";
        }
        return nearest.equalsIgnoreCase(key) ? " - did you mean [" + nearest + "]? (names are case-sensitive)"
                : " - did you mean [" + nearest + "]?";
    }

    private static void walk(Object node, Class<?> type, String path, List<String> issues) {
        if (!(node instanceof Map<?, ?> map)) {
            return; // a wrong-shaped node is reported by the typed mapping, not here
        }
        Map<String, Field> declared = keysOf(type);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Field field = declared.get(key);
            if (field == null) {
                issues.add("unknown key [" + key + "] " + at(path) + suggestion(key, declared.keySet()));
                continue;
            }
            String childPath = path.isEmpty() ? key : path + "." + key;
            Set<String> vocabulary = MAP_KEYS.get(type.getSimpleName() + "#" + key);
            if (vocabulary != null) {
                walkMap(entry.getValue(), vocabulary, type.getSimpleName() + "#" + key, childPath, issues);
                continue;
            }
            descend(entry.getValue(), field.getGenericType(), childPath, issues);
        }
    }

    /**
     * Check a closed-vocabulary map, and any registered map nested one level inside it (a step-event
     * binding's {@code { process, step }} under {@code event: { onStepReached: ... }}).
     */
    private static void walkMap(Object node, Set<String> vocabulary, String registration, String path, List<String> issues) {
        if (!(node instanceof Map<?, ?> map)) {
            return; // a wrong-shaped node is reported by the feature's own validator
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (!vocabulary.contains(key)) {
                issues.add("unknown key [" + key + "] " + at(path) + suggestion(key, vocabulary));
                continue;
            }
            Set<String> nested = MAP_KEYS.get(registration + "." + key);
            if (nested != null) {
                walkMap(entry.getValue(), nested, registration + "." + key, path + "." + key, issues);
            }
        }
    }

    /** Follow a declared property into its typed children; stop at anything not a model POJO. */
    private static void descend(Object value, Type type, String path, List<String> issues) {
        if (type instanceof Class<?> single && isModelType(single)) {
            walk(value, single, path, issues);
            return;
        }
        if (!(type instanceof ParameterizedType parameterized) || !List.class.equals(parameterized.getRawType())) {
            return;
        }
        Type argument = parameterized.getActualTypeArguments()[0];
        if (!(argument instanceof Class<?> element) || !isModelType(element) || !(value instanceof List<?> list)) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            walk(list.get(i), element, path + "[" + label(list.get(i), i) + "]", issues);
        }
    }

    /** A list element is addressed by its authored name when it has one, else by its index. */
    private static String label(Object element, int index) {
        if (element instanceof Map<?, ?> map && map.get("name") != null) {
            return String.valueOf(map.get("name"));
        }
        return String.valueOf(index);
    }

    private static String at(String path) {
        return path.isEmpty() ? "at the intent root" : "at [" + path + "]";
    }

    private static boolean isModelType(Class<?> type) {
        return MODEL_PACKAGE.equals(type.getPackageName()) && !type.isEnum();
    }

    private static Map<String, Field> keysOf(Class<?> type) {
        return KEYS.computeIfAbsent(type, UnknownKeyValidator::readKeys);
    }

    private static Map<String, Field> readKeys(Class<?> type) {
        Map<String, Field> keys = new LinkedHashMap<>();
        for (Field field : type.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            SerializedName serialized = field.getAnnotation(SerializedName.class);
            keys.put(serialized == null ? field.getName() : serialized.value(), field);
        }
        return keys;
    }

    /** Edit distance that still counts as a typo - a longer name affords a longer slip. */
    private static int threshold(String key) {
        if (key.length() <= 4) {
            return 1;
        }
        return key.length() <= 8 ? 2 : 3;
    }

    /** Plain Levenshtein distance, single-row dynamic programming. */
    private static int distance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int substitution = previous[j - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }
}
